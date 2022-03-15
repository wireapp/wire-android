package com.wire.android.ui.userprofile.image

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.datastore.UserDataStore
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.toByteArray
import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.feature.asset.GetPublicAssetUseCase
import com.wire.kalium.logic.feature.asset.PublicAssetResult
import com.wire.kalium.logic.feature.user.UploadAvatarResult
import com.wire.kalium.logic.feature.user.UploadUserAvatarUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ExperimentalMaterial3Api
@HiltViewModel
class AvatarPickerViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val dataStore: UserDataStore,
    private val getUserAvatar: GetPublicAssetUseCase,
    private val uploadUserAvatar: UploadUserAvatarUseCase,
) : ViewModel() {

    var avatarRaw by mutableStateOf<ByteArray?>(null)
        private set

    var errorMessageCode by mutableStateOf<ErrorCodes?>(null)

    init {
        loadAvatar()
    }

    private fun loadAvatar() = viewModelScope.launch {
        try {
            dataStore.avatarAssetId.first()?.apply {
                avatarRaw = (getUserAvatar(this) as PublicAssetResult.Success).asset
            }
        } catch (e: ClassCastException) {
        }
    }

    fun uploadNewPickedAvatarAndBack(imgUri: Uri, context: Context) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val data = imgUri.toByteArray(context)
                val extension = MimeTypeMap.getFileExtensionFromUrl(imgUri.path)
                val mimeType = context.contentResolver.getType(imgUri)
                    ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                    ?: "image/$extension"
                val result = uploadUserAvatar(mimeType = mimeType, imageData = data)
                if (result is UploadAvatarResult.Success) {
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

    fun navigateBack() = viewModelScope.launch { navigationManager.navigateBack() }

    fun clearErrorMessage() {
        errorMessageCode = null
    }

    sealed class ErrorCodes {
        object UploadAvatarError : ErrorCodes()
        object NoNetworkError : ErrorCodes()
    }
}
