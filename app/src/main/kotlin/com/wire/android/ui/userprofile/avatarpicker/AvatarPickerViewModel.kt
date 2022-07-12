package com.wire.android.ui.userprofile.avatarpicker

import android.content.Context
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
import com.wire.android.util.copyToTempPath
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.data.id.parseIntoQualifiedID
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
    private val dispatchers: DispatcherProvider,
    private val kaliumFileSystem: KaliumFileSystem
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
                val qualifiedAsset = this.parseIntoQualifiedID()
                val avatarRawPath = (getAvatarAsset(assetKey = qualifiedAsset) as PublicAssetResult.Success).assetPath
                val currentAvatarUri = avatarImageManager.getWritableAvatarUri(avatarRawPath)
                pictureState = PictureState.Initial(currentAvatarUri)
            }
        } catch (e: ClassCastException) {
            appLogger.e("There was an error loading the user avatar", e)
        }
    }

    fun processAvatar(imageUri: Uri) {
        viewModelScope.launch {
            withContext(dispatchers.io()) {
                pictureState = avatarImageManager
                    .postProcessAvatar(imageUri)
                    ?.let { PictureState.Picked(it) } ?: PictureState.Empty
            }
        }
    }

    fun uploadNewPickedAvatarAndBack(context: Context) {
        val imgUri = pictureState.avatarPath
        pictureState = PictureState.Uploading(imgUri)
        viewModelScope.launch {
            withContext(dispatchers.io()) {
                val tempAvatarPath = kaliumFileSystem.providePersistentAssetPath("temp_avatar.jpg")
                val imageDataSize = imgUri.copyToTempPath(context, tempAvatarPath)
                val result = uploadUserAvatar(tempAvatarPath, imageDataSize)
                if (result is UploadAvatarResult.Success) {
                    dataStore.updateUserAvatarAssetId(result.userAssetId.toString())
                    avatarImageManager.getWritableAvatarUri(tempAvatarPath)
                    navigateBack()
                } else {
                    errorMessageCode = when ((result as UploadAvatarResult.Failure).coreFailure) {
                        is NetworkFailure.NoNetworkConnection -> ErrorCodes.NoNetworkError
                        else -> ErrorCodes.UploadAvatarError
                    }
                    // reset picked state
                    pictureState = PictureState.Picked(imgUri)
                }
            }
        }
    }

    fun navigateBack() = viewModelScope.launch { navigationManager.navigateBack() }

    fun clearErrorMessage() {
        errorMessageCode = null
    }

    fun getTemporaryAvatarUri(): Uri {
        return avatarImageManager.getShareableTempAvatarUri()
    }

    sealed class ErrorCodes {
        object UploadAvatarError : ErrorCodes()
        object NoNetworkError : ErrorCodes()
    }

    sealed class PictureState(open val avatarPath: Uri) {
        data class Uploading(override val avatarPath: Uri) : PictureState(avatarPath)
        data class Initial(override val avatarPath: Uri) : PictureState(avatarPath)
        data class Picked(override val avatarPath: Uri) : PictureState(avatarPath)
        object Empty : PictureState("".toUri())
    }
}
