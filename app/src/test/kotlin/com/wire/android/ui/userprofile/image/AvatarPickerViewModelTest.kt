package com.wire.android.ui.userprofile.image

import android.net.Uri
import androidx.compose.material3.ExperimentalMaterial3Api
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.datastore.UserDataStore
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.common.imagepreview.PictureState
import com.wire.android.util.AvatarImageManager
import com.wire.kalium.logic.CoreFailure.Unknown
import com.wire.kalium.logic.feature.asset.GetPublicAssetUseCase
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
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalMaterial3Api::class)
@ExtendWith(CoroutineTestExtension::class)
class AvatarPickerViewModelTest {

    private lateinit var avatarPickerViewModel: AvatarPickerViewModel

    @MockK
    lateinit var navigationManager: NavigationManager

    @MockK
    lateinit var userDataStore: UserDataStore

    @MockK
    lateinit var getPublicAsset: GetPublicAssetUseCase

    @MockK
    lateinit var uploadUserAvatarUseCase: UploadUserAvatarUseCase

    @MockK
    lateinit var avatarImageManager: AvatarImageManager

    private val mockUri = mockk<Uri>()

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        // setup mocks for view model
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns mockUri
        every { userDataStore.avatarAssetId } returns flow { emit("some-asset-id") }
        coEvery { userDataStore.updateUserAvatarAssetId(any()) } returns Unit
        coEvery { getPublicAsset(any()) } returns PublicAssetResult.Success("some-asset-id".toByteArray())

        avatarPickerViewModel =
            AvatarPickerViewModel(navigationManager, userDataStore, getPublicAsset, uploadUserAvatarUseCase, avatarImageManager)
    }

    @Test
    fun `given a navigation case, when going back requested, then should delegate call to manager navigateBack`() = runTest {
        avatarPickerViewModel.navigateBack()

        coVerify(exactly = 1) { navigationManager.navigateBack() }
    }

    @Test
    fun `given an image, when picked, then should emit a picked state with uri`() = runTest {
        avatarPickerViewModel.pickNewImage(mockUri)

        assertEquals(PictureState.Picked::class, avatarPickerViewModel.pictureState::class)
    }

    @Test
    fun `given a valid image, when uploading the asset succeed, then should call the usecase and navigate back on success`() = runTest {
        val uploadedAssetId = "some-asset-id"
        val rawImage = uploadedAssetId.toByteArray()
        coEvery { uploadUserAvatarUseCase(any()) } returns UploadAvatarResult.Success(uploadedAssetId)
        coEvery { avatarImageManager.uriToByteArray(any()) } returns rawImage

        avatarPickerViewModel.uploadNewPickedAvatarAndBack(mockUri)

        coVerify {
            avatarImageManager.uriToByteArray(mockUri)
            uploadUserAvatarUseCase(rawImage)
            userDataStore.updateUserAvatarAssetId(uploadedAssetId)
            avatarImageManager.getWritableAvatarUri(rawImage)
        }
        assertEquals(null, avatarPickerViewModel.errorMessageCode)
    }

    @Test
    fun `given a valid image, when uploading the asset fails, then should emit an error`() = runTest {
        val uploadedAssetId = "some-asset-id"
        val rawImage = uploadedAssetId.toByteArray()
        coEvery { avatarImageManager.uriToByteArray(any()) } returns rawImage
        coEvery { uploadUserAvatarUseCase(any()) } returns UploadAvatarResult.Failure(Unknown(RuntimeException("some error")))

        avatarPickerViewModel.uploadNewPickedAvatarAndBack(mockUri)

        coVerify {
            avatarImageManager.uriToByteArray(mockUri)
            uploadUserAvatarUseCase(rawImage)
        }
        coVerify {
            avatarImageManager.getWritableAvatarUri(rawImage) wasNot Called
        }

        assertEquals(AvatarPickerViewModel.ErrorCodes.UploadAvatarError, avatarPickerViewModel.errorMessageCode)
    }

    @Test
    fun `given there is an error, when calling clear messages, then should clean the error messages codes`() = runTest {
        avatarPickerViewModel.clearErrorMessage()

        assertEquals(null, avatarPickerViewModel.errorMessageCode)
    }
}
