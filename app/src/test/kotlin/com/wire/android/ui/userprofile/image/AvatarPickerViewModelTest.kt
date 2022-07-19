package com.wire.android.ui.userprofile.image

import android.content.Context
import android.net.Uri
import androidx.compose.material3.ExperimentalMaterial3Api
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.datastore.UserDataStore
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.userprofile.avatarpicker.AvatarPickerViewModel
import com.wire.android.util.AvatarImageManager
import com.wire.kalium.logic.CoreFailure.Unknown
import com.wire.kalium.logic.data.asset.FakeKaliumFileSystem
import com.wire.kalium.logic.data.id.parseIntoQualifiedID
import com.wire.kalium.logic.feature.asset.GetAvatarAssetUseCase
import com.wire.kalium.logic.feature.asset.PublicAssetResult
import com.wire.kalium.logic.feature.user.UploadAvatarResult
import com.wire.kalium.logic.feature.user.UploadUserAvatarUseCase
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import com.wire.kalium.logic.data.user.UserAssetId
import okio.Path
import okio.Path.Companion.toPath
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
            val uploadedAssetId = "some-dummy-value@some-dummy-domain".parseIntoQualifiedID()
            val expectedAvatarPath = fakeKaliumFileSystem.providePersistentAssetPath("Some-dummy-path")
            val mockedContext = mockk<Context>()

            // Given
            val (arrangement, avatarPickerViewModel) = Arrangement()
                .withSuccessfulInitialAvatarLoad()
                .withSuccessfulAvatarUpload(uploadedAssetId)
                .arrange()

            with(arrangement) {

            avatarPickerViewModel.uploadNewPickedAvatarAndBack(mockedContext)

            coVerify {
                uploadUserAvatarUseCase(any(), any())
                userDataStore.updateUserAvatarAssetId(uploadedAssetId.toString())
                avatarPickerViewModel.navigateBack()
            }
            assertEquals(null, avatarPickerViewModel.errorMessageCode)
        }}
//
//    @Test
//    fun `given a valid image, when uploading the asset fails, then should emit an error`() = runTest {
//        val uploadedAssetId = "some-asset-id"
//        val rawImage = uploadedAssetId.toByteArray()
//        coEvery { avatarImageManager.uriToTempPath(any()) } returns rawImage
//        coEvery { uploadUserAvatarUseCase(any()) } returns UploadAvatarResult.Failure(Unknown(RuntimeException("some error")))
//
//        avatarPickerViewModel.uploadNewPickedAvatarAndBack()
//
//        coVerify {
//            avatarImageManager.uriToTempPath(mockUri)
//            uploadUserAvatarUseCase(rawImage)
//        }
//        coVerify {
//            avatarImageManager.getWritableAvatarUri(rawImage) wasNot Called
//        }
//        assertNotNull(avatarPickerViewModel.errorMessageCode)
//    }
//
//    @Test
//    fun `given there is an error, when calling clear messages, then should clean the error messages codes`() = runTest {
//        avatarPickerViewModel.clearErrorMessage()
//
//        assertEquals(null, avatarPickerViewModel.errorMessageCode)
//    }

    private class Arrangement {

        @MockK
        lateinit var navigationManager: NavigationManager

        @MockK
        lateinit var userDataStore: UserDataStore

        @MockK
        lateinit var getAvatarAsset: GetAvatarAssetUseCase

        @MockK
        lateinit var uploadUserAvatarUseCase: UploadUserAvatarUseCase

        @MockK
        lateinit var avatarImageManager: AvatarImageManager

        private val mockUri = mockk<Uri>()

        fun withSuccessfulInitialAvatarLoad(): Arrangement {
            val fakeAvatarData = "some-dummy-avatar".toByteArray()
            val avatarPath = fakeKaliumFileSystem.selfUserAvatarPath()
            fakeKaliumFileSystem.sink(avatarPath).buffer().use {
                it.write(fakeAvatarData)
            }
            coEvery { getAvatarAsset(any()) } returns PublicAssetResult.Success(avatarPath)
            every { userDataStore.avatarAssetId } returns flow { emit("avatar-value@avatar-domain") }
            coEvery { avatarImageManager.getWritableAvatarUri(any()) } returns mockUri
            return this
        }

        fun withSuccessfulAvatarUpload(expectedUserAssetId: UserAssetId): Arrangement {
            coEvery { userDataStore.updateUserAvatarAssetId(any()) } returns Unit
            coEvery { uploadUserAvatarUseCase(any(), any()) } returns UploadAvatarResult.Success(expectedUserAssetId)
            return this
        }

        fun arrange() =
            this to AvatarPickerViewModel(
                navigationManager,
                userDataStore,
                getAvatarAsset,
                uploadUserAvatarUseCase,
                avatarImageManager,
                TestDispatcherProvider(),
                fakeKaliumFileSystem
            )
    }

    companion object {
        val fakeKaliumFileSystem: FakeKaliumFileSystem = FakeKaliumFileSystem()
    }
}
