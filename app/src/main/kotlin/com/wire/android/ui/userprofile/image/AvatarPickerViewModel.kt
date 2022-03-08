package com.wire.android.ui.userprofile.image

import android.content.Context
import android.net.Uri
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.datastore.UserDataStore
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.toByteArray
import com.wire.kalium.logic.data.user.UserAssetId
import com.wire.kalium.logic.feature.asset.GetPublicAssetUseCase
import com.wire.kalium.logic.feature.asset.PublicAssetResult
import com.wire.kalium.logic.feature.user.UploadUserAvatarUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@ExperimentalMaterial3Api
@HiltViewModel
class AvatarPickerViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val dataStore: UserDataStore,
    private val getUserAvatar: GetPublicAssetUseCase,
    private val uploadUserAvatar: UploadUserAvatarUseCase,
) : ViewModel() {

    var avatarImageByteArray by mutableStateOf<ByteArray?>(null)
        private set

    init {
        viewModelScope.launch {
            avatarImageByteArray = getAvatarAsByteArrayOrNull(dataStore.avatarAssetId.first())
        }
    }

    suspend fun getAvatarAsByteArrayOrNull(avatarAssetId: UserAssetId?): ByteArray? {
        return try {
            avatarAssetId?.let {
                (getUserAvatar(it) as PublicAssetResult.Success).asset
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun uploadNewPickedAvatarAndBack(chosenImgUri: Uri, context: Context) {
        viewModelScope.launch {
            val data = chosenImgUri.toByteArray(context)
            uploadUserAvatar(mimeType = "image/jpg", imageData = data)
            navigateBack()
        }
    }

    fun navigateBack() = viewModelScope.launch { navigationManager.navigateBack() }
}
