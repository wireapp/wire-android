package com.wire.android.ui.userprofile.image

import android.content.Context
import android.net.Uri
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.datastore.UserDataStore
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.common.imagepreview.PictureState
import com.wire.android.util.DEFAULT_IMAGE_MIME_TYPE
import com.wire.android.util.ImageUtil.Companion.postProcessCapturedAvatar
import com.wire.android.util.getMimeType
import com.wire.android.util.getWritableAvatarUri
import com.wire.android.util.toByteArray
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
    private val context: Context
) : ViewModel() {

    var pictureState by mutableStateOf<PictureState>(PictureState.Initial(Uri.EMPTY))
        private set

    var errorMessageCode by mutableStateOf<ErrorCodes?>(null)

    init {
        loadAvatar()
    }

    private fun loadAvatar() = viewModelScope.launch {
        try {
            dataStore.avatarAssetId.first()?.apply {
                val avatarRaw = (getUserAvatar(this) as PublicAssetResult.Success).asset
                val currentAvatarUri = getWritableAvatarUri(avatarRaw, context)
                pictureState = PictureState.Initial(currentAvatarUri)
            }
        } catch (e: ClassCastException) {
            appLogger.e("There was an error loading the user avatar", e)
        }
    }

    fun uploadNewPickedAvatarAndBack(imgUri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val data = imgUri.toByteArray(context)
                val mimeType = imgUri.getMimeType(context) ?: DEFAULT_IMAGE_MIME_TYPE
                val result = uploadUserAvatar(imageData = data)
                if (result is UploadAvatarResult.Success) {
                    getWritableAvatarUri(data, context)
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
                postProcessCapturedAvatar(imgUri, context)
                pictureState = PictureState.Picked(imgUri)
            }
        }
    }

    fun navigateBack() = viewModelScope.launch { navigationManager.navigateBack() }

    fun clearErrorMessage() {
        errorMessageCode = null
    }

    sealed class ErrorCodes {
        object UploadAvatarError : ErrorCodes()
        object NoNetworkError : ErrorCodes()
    }
}
