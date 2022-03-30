package com.wire.android.ui.userprofile.image

import android.net.Uri
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.datastore.UserDataStore
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.AvatarImageManager
import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.feature.asset.GetPublicAssetUseCase
import com.wire.kalium.logic.feature.asset.PublicAssetResult
import com.wire.kalium.logic.feature.user.UploadAvatarResult
import com.wire.kalium.logic.feature.user.UploadUserAvatarUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@ExperimentalMaterial3Api
@HiltViewModel
class AvatarPickerViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val dataStore: UserDataStore,
    private val getUserAvatar: GetPublicAssetUseCase,
    private val uploadUserAvatar: UploadUserAvatarUseCase,
    private val avatarImageManager: AvatarImageManager
) : ViewModel() {

    var pictureState by mutableStateOf<PictureState>(PictureState.Empty)
        private set

    var errorMessageCode by mutableStateOf<ErrorCodes?>(null)

    init {
        loadAvatar()
    }

    private fun loadAvatar() = viewModelScope.launch {
        try {
            dataStore.avatarAssetId.first()?.apply {
                val avatarRaw = (getUserAvatar(this) as PublicAssetResult.Success).asset
                val currentAvatarUri = avatarImageManager.getWritableAvatarUri(avatarRaw)
                pictureState = PictureState.Initial(currentAvatarUri)
            }
        } catch (e: ClassCastException) {
            appLogger.e("There was an error loading the user avatar", e)
        }
    }

    fun uploadNewPickedAvatarAndBack() {
        val imgUri = pictureState.avatarUri
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val data = avatarImageManager.uriToByteArray(imgUri)
                val result = uploadUserAvatar(data)
                if (result is UploadAvatarResult.Success) {
                    dataStore.updateUserAvatarAssetId(result.userAssetId)
                    avatarImageManager.getWritableAvatarUri(data)
                    navigateBack()
                } else {
                    errorMessageCode = when ((result as UploadAvatarResult.Failure).coreFailure) {
                        is NetworkFailure.NoNetworkConnection -> ErrorCodes.NoNetworkError
                        else -> ErrorCodes.UploadAvatarError
                    }
                }
            }
        }
    }

    fun postProcessAvatarImage(imgUri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                avatarImageManager.postProcessCapturedAvatar(imgUri)
                pictureState = PictureState.Picked(imgUri)
            }
        }
    }

    fun pickNewImage(imageUri: Uri) {
        pictureState = PictureState.Picked(imageUri)
    }

    fun navigateBack() = viewModelScope.launch { navigationManager.navigateBack() }

    fun clearErrorMessage() {
        errorMessageCode = null
    }

    sealed class ErrorCodes {
        object UploadAvatarError : ErrorCodes()
        object NoNetworkError : ErrorCodes()
    }

    sealed class PictureState(open val avatarUri: Uri) {
        data class Initial(override val avatarUri: Uri) : PictureState(avatarUri)
        data class Picked(override val avatarUri: Uri) : PictureState(avatarUri)
        object Empty : PictureState("".toUri())
    }
}
