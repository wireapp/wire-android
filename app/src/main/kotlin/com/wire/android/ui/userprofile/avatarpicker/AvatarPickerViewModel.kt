package com.wire.android.ui.userprofile.avatarpicker

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
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.feature.asset.GetAvatarAssetUseCase
import com.wire.kalium.logic.feature.asset.PublicAssetResult
import com.wire.kalium.logic.feature.user.UploadAvatarResult
import com.wire.kalium.logic.feature.user.UploadUserAvatarUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ExperimentalMaterial3Api
@HiltViewModel
class AvatarPickerViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val dataStore: UserDataStore,
    private val getAvatarAsset: GetAvatarAssetUseCase,
    private val uploadUserAvatar: UploadUserAvatarUseCase,
    private val avatarImageManager: AvatarImageManager,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    var pictureState by mutableStateOf<PictureState>(PictureState.Empty)
        private set

    var errorMessageCode by mutableStateOf<ErrorCodes?>(null)

    init {
        loadInitialAvatarState()
    }

    fun loadInitialAvatarState() = viewModelScope.launch {
        try {
            dataStore.avatarAssetId.first()?.apply {
                val avatarRaw = (getAvatarAsset(assetKey = this) as PublicAssetResult.Success).asset
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
            withContext(dispatchers.io()) {
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
            withContext(dispatchers.io()) {
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

    fun getTemporaryTargetAvatarUri(): Uri {
        return avatarImageManager.getShareableTempAvatarUri()
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
