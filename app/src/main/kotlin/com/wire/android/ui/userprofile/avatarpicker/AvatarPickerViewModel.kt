package com.wire.android.ui.userprofile.avatarpicker

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.datastore.UserDataStore
import com.wire.android.model.SnackBarMessage
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.AvatarImageManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.toByteArray
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.feature.asset.GetAvatarAssetUseCase
import com.wire.kalium.logic.feature.asset.PublicAssetResult
import com.wire.kalium.logic.feature.user.UploadAvatarResult
import com.wire.kalium.logic.feature.user.UploadUserAvatarUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okio.Path
import javax.inject.Inject

@HiltViewModel
@Suppress("LongParameterList")
class AvatarPickerViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val dataStore: UserDataStore,
    private val getAvatarAsset: GetAvatarAssetUseCase,
    private val uploadUserAvatar: UploadUserAvatarUseCase,
    private val avatarImageManager: AvatarImageManager,
    private val dispatchers: DispatcherProvider,
    private val kaliumFileSystem: KaliumFileSystem,
    private val qualifiedIdMapper: QualifiedIdMapper,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    var pictureState by mutableStateOf<PictureState>(PictureState.Empty)
        private set

    private val _infoMessage = MutableSharedFlow<UIText>()
    val infoMessage = _infoMessage.asSharedFlow()

    init {
        loadInitialAvatarState()
    }

    fun loadInitialAvatarState() {
        viewModelScope.launch {
            try {
                dataStore.avatarAssetId.first()?.apply {
                    val qualifiedAsset = qualifiedIdMapper.fromStringToQualifiedID(this)
                    val avatarRawPath = (getAvatarAsset(assetKey = qualifiedAsset) as PublicAssetResult.Success).assetPath
                    val currentAvatarUri = avatarImageManager.getWritableAvatarUri(avatarRawPath)
                    pictureState = PictureState.Initial(currentAvatarUri)
                }
            } catch (e: ClassCastException) {
                appLogger.e("There was an error loading the user avatar", e)
            }
        }
    }

    fun updatePickedAvatarUri(updatedUri: Uri) = viewModelScope.launch(dispatchers.main()) {
        pictureState = PictureState.Picked(updatedUri)
    }

    fun uploadNewPickedAvatarAndBack() {
        val imgUri = pictureState.avatarUri
        pictureState = PictureState.Uploading(imgUri)

        viewModelScope.launch {
            val avatarPath = defaultAvatarPath()
            val imageDataSize = imgUri.toByteArray(appContext, dispatchers).size.toLong()

            val result = uploadUserAvatar(avatarPath, imageDataSize)
            if (result is UploadAvatarResult.Success) {
                dataStore.updateUserAvatarAssetId(result.userAssetId.toString())
                navigateBack()
            } else {
                when ((result as UploadAvatarResult.Failure).coreFailure) {
                    is NetworkFailure.NoNetworkConnection -> showInfoMessage(InfoMessageType.NoNetworkError)
                    else -> showInfoMessage(InfoMessageType.UploadAvatarError)
                }
            }
        }
    }

    private suspend fun showInfoMessage(type: SnackBarMessage) {
        _infoMessage.emit(type.uiText)
    }

    fun navigateBack() = viewModelScope.launch(dispatchers.main()) { navigationManager.navigateBack() }

    fun defaultAvatarPath(): Path = kaliumFileSystem.selfUserAvatarPath()

    fun getTemporaryAvatarUri(filePath: Path): Uri {
        return avatarImageManager.getShareableTempAvatarUri(filePath)
    }

    sealed class PictureState(open val avatarUri: Uri) {
        data class Uploading(override val avatarUri: Uri) : PictureState(avatarUri)
        data class Initial(override val avatarUri: Uri) : PictureState(avatarUri)
        data class Picked(override val avatarUri: Uri) : PictureState(avatarUri)
        object Empty : PictureState("".toUri())
    }

    sealed class InfoMessageType(override val uiText: UIText) : SnackBarMessage {
        object UploadAvatarError : InfoMessageType(UIText.StringResource(R.string.error_uploading_user_avatar))
        object NoNetworkError : InfoMessageType(UIText.StringResource(R.string.error_no_network_message))
    }

}
