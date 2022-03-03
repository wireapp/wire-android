package com.wire.android.ui.userprofile.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.datastore.UserDataStore
import com.wire.android.navigation.EXTRA_INITIAL_AVATAR_URI
import com.wire.android.navigation.NavigationManager
import com.wire.android.navigation.parseArgumentsStringToUri
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.UploadUserAvatarUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@ExperimentalMaterial3Api
@HiltViewModel
class AvatarPickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val dataStore: UserDataStore,
) : ViewModel() {

    suspend fun avatarUri(): Uri = dataStore.userAvatarUri.first()

    fun changeUserAvatar(avatarBitmap: Bitmap, shouldNavigateBack: Boolean = false) {
//            val backupBitmap = userProfileState.avatarBitmap
//            viewModelScope.launch {
//                withContext(Dispatchers.IO) {
//                    // Update the user avatar on the userProfileState object with the local bitmap
//                    userProfileState = userProfileState.copy(avatarBitmap = avatarBitmap, isAvatarLoading = true)
//
//                    // Upload the Avatar image
//                    userProfileState = when (uploadUserAvatar("image/png", avatarBitmap.toByteArray())) {
//                        // Fallback
//                        is Either.Left -> {
//                            userProfileState.copy(
//                                avatarBitmap = backupBitmap,
//                                isAvatarLoading = false,
//                                errorMessage = "Image could not be uploaded"
//                            )
//                        }
//
//                        // Happy path
//                        else -> userProfileState.copy(isAvatarLoading = false)
//                    }
//
//                    if (shouldNavigateBack) navigateBack()
//                }
//            }
    }

    fun getMockedImg(context: Context): Bitmap {
        return BitmapFactory.decodeResource(context.resources, R.drawable.mock_message_image)
    }

    fun navigateBack() = viewModelScope.launch { navigationManager.navigateBack() }
}
