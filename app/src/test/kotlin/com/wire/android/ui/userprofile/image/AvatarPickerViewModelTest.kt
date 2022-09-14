package com.wire.android.ui.userprofile.image

import android.content.Context
import android.net.Uri
import app.cash.turbine.test
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.datastore.UserDataStore
import com.wire.android.framework.FakeKaliumFileSystem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.userprofile.avatarpicker.AvatarPickerViewModel
import com.wire.android.util.AvatarImageManager
import com.wire.android.util.copyToTempPath
import com.wire.android.util.resampleImageAndCopyToTempPath
import com.wire.android.util.toByteArray
import com.wire.kalium.logic.CoreFailure.Unknown
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.user.AssetId
import com.wire.kalium.logic.data.user.UserAssetId
import com.wire.kalium.logic.feature.asset.GetAvatarAssetUseCase
import com.wire.kalium.logic.feature.asset.PublicAssetResult
import com.wire.kalium.logic.feature.user.UploadAvatarResult
import com.wire.kalium.logic.feature.user.UploadUserAvatarUseCase
import io.mockk.Called
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import okio.buffer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class AvatarPickerViewModelTest {

    @Test
    fun `given a navigation case, when going back requested, then should delegate call to manager navigateBack`() = runTest {
        // Given
        val (arrangement, avatarPickerViewModel) = Arrangement()
            .withSuccessfulInitialAvatarLoad()
            .arrange()

        // When
        avatarPickerViewModel.navigateBack()

        // Then
        coVerify(exactly = 1) { arrangement.navigationManager.navigateBack() }
    }

    @Test
    fun `given a valid image, when uploading the asset succeeds, then the useCase should be called and navigate back on success`() =
        runTest {
            // Given
            val uploadedAssetId = AssetId("some-dummy-value", "some-dummy-domain")

            val (arrangement, avatarPickerViewModel) = Arrangement()
                .withSuccessfulInitialAvatarLoad()
                .withSuccessfulAvatarUpload(uploadedAssetId)
                .arrange()

            avatarPickerViewModel.infoMessage.test {
                // When
                avatarPickerViewModel.uploadNewPickedAvatarAndBack()

                // Then
                coVerify {
                    with(arrangement) {
                        uploadUserAvatarUseCase(any(), any())
                        userDataStore.updateUserAvatarAssetId(uploadedAssetId.toString())
                        avatarPickerViewModel.navigateBack()
                    }
                }

                expectNoEvents()
            }
        }

    @Test
    fun `given a valid image, when uploading the asset fails, then should emit an error`() = runTest {
        // Given
        val (arrangement, avatarPickerViewModel) = Arrangement()
            .withSuccessfulInitialAvatarLoad()
            .withErrorUploadResponse()
            .arrange()

        avatarPickerViewModel.infoMessage.test {
            // When
            avatarPickerViewModel.uploadNewPickedAvatarAndBack()

            // Then
            with(arrangement) {
                coVerify {
                    uploadUserAvatarUseCase(any(), any())
                    avatarImageManager.getWritableAvatarUri(any()) wasNot Called
                }
            }

            assertEquals(AvatarPickerViewModel.InfoMessageType.UploadAvatarError.uiText, awaitItem())
        }
    }

    private class Arrangement {

        val navigationManager = mockk<NavigationManager>()

        val userDataStore = mockk<UserDataStore>()

        val getAvatarAsset = mockk<GetAvatarAssetUseCase>()

        val uploadUserAvatarUseCase = mockk<UploadUserAvatarUseCase>()

        val avatarImageManager = mockk<AvatarImageManager>()

        val context = mockk<Context>()

        @MockK
        private lateinit var qualifiedIdMapper: QualifiedIdMapper

        val dispatcherProvider = TestDispatcherProvider()

        val viewModel by lazy {
            AvatarPickerViewModel(
                navigationManager,
                userDataStore,
                getAvatarAsset,
                uploadUserAvatarUseCase,
                avatarImageManager,
                dispatcherProvider,
                fakeKaliumFileSystem,
                qualifiedIdMapper,
                context
            )
        }

        private val mockUri = mockk<Uri>()

        fun withSuccessfulInitialAvatarLoad(): Arrangement {
            val avatarAssetId = "avatar-value@avatar-domain"
            MockKAnnotations.init(this, relaxUnitFun = true)
            mockkStatic(Uri::class)
            mockkStatic(Uri::resampleImageAndCopyToTempPath)
            mockkStatic(Uri::toByteArray)
            every { Uri.parse(any()) } returns mockUri
            val fakeAvatarData = "some-dummy-avatar".toByteArray()
            val avatarPath = fakeKaliumFileSystem.selfUserAvatarPath()
            fakeKaliumFileSystem.sink(avatarPath).buffer().use {
                it.write(fakeAvatarData)
            }
            coEvery { getAvatarAsset(any()) } returns PublicAssetResult.Success(avatarPath)
            coEvery { avatarImageManager.getWritableAvatarUri(any()) } returns mockUri
            coEvery { avatarImageManager.getShareableTempAvatarUri(any()) } returns mockUri
            coEvery { navigationManager.navigateBack() } returns Unit
            coEvery { any<Uri>().resampleImageAndCopyToTempPath(any(), any(), any(), any()) } returns 1L
            coEvery { any<Uri>().toByteArray(any(), any()) } returns ByteArray(5)
            every { userDataStore.avatarAssetId } returns flow { emit(avatarAssetId) }
            every { mockUri.copyToTempPath(any(), any()) } returns 1L
            every { qualifiedIdMapper.fromStringToQualifiedID(any()) } returns QualifiedID("avatar-value", "avatar-domain")

            return this
        }

        fun withSuccessfulAvatarUpload(expectedUserAssetId: UserAssetId): Arrangement {
            coEvery { userDataStore.updateUserAvatarAssetId(any()) } returns Unit
            coEvery { uploadUserAvatarUseCase(any(), any()) } returns UploadAvatarResult.Success(expectedUserAssetId)

            return this
        }

        fun withErrorUploadResponse(): Arrangement {
            coEvery { uploadUserAvatarUseCase(any(), any()) } returns UploadAvatarResult.Failure(Unknown(RuntimeException("some error")))

            return this
        }

        fun arrange() =
            this to viewModel
    }

    companion object {
        val fakeKaliumFileSystem: FakeKaliumFileSystem = FakeKaliumFileSystem()
    }
}
