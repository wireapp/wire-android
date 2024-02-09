/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.android.ui.userprofile.avatarpicker

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.datastore.UserDataStore
import com.wire.android.model.SnackBarMessage
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

    private var initialPictureLoadingState by mutableStateOf<InitialPictureLoadingState>(InitialPictureLoadingState.None)

    private val _infoMessage = MutableSharedFlow<UIText>()
    val infoMessage = _infoMessage.asSharedFlow()

    val defaultAvatarPath: Path
        get() = kaliumFileSystem.selfUserAvatarPath()

    val temporaryAvatarUri: Uri = avatarImageManager.getShareableTempAvatarUri(defaultAvatarPath)

    init {
        loadInitialAvatarState()
    }

    @Suppress("TooGenericExceptionCaught")
    fun loadInitialAvatarState() {
        viewModelScope.launch {
            initialPictureLoadingState = InitialPictureLoadingState.Loading
            try {
                dataStore.avatarAssetId.first()?.apply {
                    val qualifiedAsset = qualifiedIdMapper.fromStringToQualifiedID(this)
                    val avatarRawPath = (getAvatarAsset(assetKey = qualifiedAsset) as PublicAssetResult.Success).assetPath
                    val currentAvatarUri = avatarImageManager.getWritableAvatarUri(avatarRawPath)
                    initialPictureLoadingState = InitialPictureLoadingState.Loaded(currentAvatarUri)
                    if (pictureState is PictureState.Empty) {
                        pictureState = PictureState.Initial(currentAvatarUri)
                    }
                }
            } catch (e: Exception) {
                appLogger.e("There was an error loading the user avatar", e)
                initialPictureLoadingState = InitialPictureLoadingState.None
            }
        }
    }

    fun updatePickedAvatarUri(updatedUri: Uri) = viewModelScope.launch(dispatchers.main()) {
        pictureState = PictureState.Picked(updatedUri)
    }

    fun uploadNewPickedAvatar(onComplete: (avatarAssetId: String?) -> Unit) {
        val imgUri = pictureState.avatarUri

        viewModelScope.launch {
            pictureState = PictureState.Uploading(imgUri)

            val avatarPath = defaultAvatarPath
            val imageDataSize = imgUri.toByteArray(appContext, dispatchers).size.toLong()
            when (val result = uploadUserAvatar(avatarPath, imageDataSize)) {
                is UploadAvatarResult.Success -> {
                    dataStore.updateUserAvatarAssetId(result.userAssetId.toString())
                    onComplete(dataStore.avatarAssetId.first())
                }
                is UploadAvatarResult.Failure -> {
                    when (result.coreFailure) {
                        is NetworkFailure.NoNetworkConnection -> showInfoMessage(InfoMessageType.NoNetworkError)
                        else -> showInfoMessage(InfoMessageType.UploadAvatarError)
                    }
                    with(initialPictureLoadingState) {
                        pictureState = when (this) {
                            is InitialPictureLoadingState.Loaded -> PictureState.Initial(avatarUri)
                            else -> PictureState.Empty
                        }
                    }
                }
            }
        }
    }

    private suspend fun showInfoMessage(type: SnackBarMessage) {
        _infoMessage.emit(type.uiText)
    }

    @Stable
    private sealed class InitialPictureLoadingState {
        data object None : InitialPictureLoadingState()
        data object Loading : InitialPictureLoadingState()
        data class Loaded(val avatarUri: Uri) : InitialPictureLoadingState()
    }

    @Stable
    sealed class PictureState(open val avatarUri: Uri) {
        data class Uploading(override val avatarUri: Uri) : PictureState(avatarUri)
        data class Initial(override val avatarUri: Uri) : PictureState(avatarUri)
        data class Picked(override val avatarUri: Uri) : PictureState(avatarUri)
        data object Empty : PictureState("".toUri())
    }

    sealed class InfoMessageType(override val uiText: UIText) : SnackBarMessage {
        data object UploadAvatarError : InfoMessageType(UIText.StringResource(R.string.error_uploading_user_avatar))
        data object NoNetworkError : InfoMessageType(UIText.StringResource(R.string.error_no_network_message))
    }
}
