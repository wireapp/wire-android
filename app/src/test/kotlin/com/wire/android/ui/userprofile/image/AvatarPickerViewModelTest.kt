package com.wire.android.ui.userprofile.image

import android.content.Context
import android.net.Uri
import androidx.compose.material3.ExperimentalMaterial3Api
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.config.mockUri
import com.wire.android.datastore.UserDataStore
import com.wire.android.framework.FakeKaliumFileSystem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.userprofile.avatarpicker.AvatarPickerViewModel
import com.wire.android.util.AvatarImageManager
import com.wire.android.util.ImageUtil
import com.wire.android.util.copyToTempPath
import com.wire.android.util.resampleImageAndCopyToTempPath
import com.wire.kalium.logic.CoreFailure.Unknown
import com.wire.kalium.logic.data.id.parseIntoQualifiedID
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

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalMaterial3Api::class)
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
            val uploadedAssetId = "some-dummy-value@some-dummy-domain".parseIntoQualifiedID()
            val mockedContext = mockk<Context>()

            val (arrangement, avatarPickerViewModel) = Arrangement()
                .withSuccessfulInitialAvatarLoad()
                .withSuccessfulAvatarUpload(uploadedAssetId)
                .arrange()

            // When
            avatarPickerViewModel.uploadNewPickedAvatarAndBack(mockedContext)

            // Then
            with(arrangement) {
                coVerify {
                    uploadUserAvatarUseCase(any(), any())
                    userDataStore.updateUserAvatarAssetId(uploadedAssetId.toString())
                    avatarPickerViewModel.navigateBack()
                }
                assertEquals(null, avatarPickerViewModel.errorMessageCode)
            }
        }

    @Test
    fun `given a valid picked image, before uploading it, it gets resampled correctly`() = runTest {
        // Given
        val uploadedAssetId = "some-dummy-value@some-dummy-domain".parseIntoQualifiedID()
        val mockedContext = mockk<Context>()
        val pickedUri = mockk<Uri>()

        val (arrangement, avatarPickerViewModel) = Arrangement()
            .withSuccessfulInitialAvatarLoad()
            .withSuccessfulAvatarUpload(uploadedAssetId)
            .withPickedUri(pickedUri)
            .arrange()

        // When
        avatarPickerViewModel.uploadNewPickedAvatarAndBack(mockedContext)

        // Then
        with(arrangement) {
            coVerify {
                avatarImageManager.getWritableAvatarUri(match { it == fakeKaliumFileSystem.selfUserAvatarPath() })
                pickedUri.resampleImageAndCopyToTempPath(
                    mockedContext,
                    fakeKaliumFileSystem.selfUserAvatarPath(),
                    ImageUtil.ImageSizeClass.Small
                )
            }
            assertEquals(pickedUri, avatarPickerViewModel.pictureState.avatarUri)
        }
    }

    @Test
    fun `given a valid image, when uploading the asset fails, then should emit an error`() = runTest {
        // Given
        val mockedContext = mockk<Context>()

        val (arrangement, avatarPickerViewModel) = Arrangement()
            .withSuccessfulInitialAvatarLoad()
            .withErrorUploadResponse()
            .arrange()

        // When
        avatarPickerViewModel.uploadNewPickedAvatarAndBack(mockedContext)

        // Then
        with(arrangement) {
            coVerify {
                uploadUserAvatarUseCase(any(), any())
                avatarImageManager.getWritableAvatarUri(any()) wasNot Called
            }
            assertNotNull(avatarPickerViewModel.errorMessageCode)
        }
    }

    @Test
    fun `given there is an error, when calling clear messages, then should clean the error messages codes`() = runTest {
        // Given
        val mockedContext = mockk<Context>()
        val (_, avatarPickerViewModel) = Arrangement()
            .withSuccessfulInitialAvatarLoad()
            .withErrorUploadResponse()
            .arrange()

        // When
        avatarPickerViewModel.uploadNewPickedAvatarAndBack(mockedContext)
        avatarPickerViewModel.clearErrorMessage()

        // Then
        assertEquals(null, avatarPickerViewModel.errorMessageCode)
    }

    private class Arrangement {

        val navigationManager = mockk<NavigationManager>()

        val userDataStore = mockk<UserDataStore>()

        val getAvatarAsset = mockk<GetAvatarAssetUseCase>()

        val uploadUserAvatarUseCase = mockk<UploadUserAvatarUseCase>()

        val avatarImageManager = mockk<AvatarImageManager>()

        val viewModel by lazy {
            AvatarPickerViewModel(
                navigationManager,
                userDataStore,
                getAvatarAsset,
                uploadUserAvatarUseCase,
                avatarImageManager,
                TestDispatcherProvider(),
                fakeKaliumFileSystem
            )
        }

        private val mockUri = mockk<Uri>()

        fun withSuccessfulInitialAvatarLoad(): Arrangement {
            MockKAnnotations.init(this, relaxUnitFun = true)
            mockkStatic(Uri::class)
            mockkStatic(Uri::resampleImageAndCopyToTempPath)
            every { Uri.parse(any()) } returns mockUri
            val fakeAvatarData = "some-dummy-avatar".toByteArray()
            val avatarPath = fakeKaliumFileSystem.selfUserAvatarPath()
            fakeKaliumFileSystem.sink(avatarPath).buffer().use {
                it.write(fakeAvatarData)
            }
            coEvery { getAvatarAsset(any()) } returns PublicAssetResult.Success(avatarPath)
            every { userDataStore.avatarAssetId } returns flow { emit("avatar-value@avatar-domain") }
            coEvery { avatarImageManager.getWritableAvatarUri(any()) } returns mockUri
            coEvery { mockUri.resampleImageAndCopyToTempPath(any(), any()) } returns 1L
            every { mockUri.copyToTempPath(any(), any()) } returns 1L

            return this
        }

        fun withSuccessfulAvatarUpload(expectedUserAssetId: UserAssetId): Arrangement {
            coEvery { userDataStore.updateUserAvatarAssetId(any()) } returns Unit
            coEvery { uploadUserAvatarUseCase(any(), any()) } returns UploadAvatarResult.Success(expectedUserAssetId)

            return this
        }

        fun withPickedUri(pickedUri: Uri): Arrangement {
            viewModel.updatePickedAvatarUri(pickedUri)

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
