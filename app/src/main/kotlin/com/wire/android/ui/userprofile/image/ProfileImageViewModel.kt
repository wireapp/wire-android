package com.wire.android.ui.userprofile.image

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProfileImageViewModel @Inject constructor(
) : ViewModel() {

    var state by mutableStateOf(ProfileImageViewModelState())
        private set

    fun onAvatarBitmapChange(avatarBitmap: Bitmap) {
        state = state.copy(
            hasPickedAvatar = true,
            avatarBitmap = avatarBitmap
        )
    }

}

data class ProfileImageViewModelState(
    val hasPickedAvatar: Boolean = false,
    val avatarBitmap: Bitmap = Bitmap.createBitmap(36, 36, Bitmap.Config.ARGB_8888),
)

sealed class UploadStatus {
    object Initial : UploadStatus()
    object Error : UploadStatus()
    object Success : UploadStatus()
}
