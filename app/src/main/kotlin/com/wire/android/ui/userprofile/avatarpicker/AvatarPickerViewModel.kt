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

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.CurrentAccount
import com.wire.android.model.SnackBarMessage
import com.wire.android.util.ui.UIText
import com.wire.content.external.ExternalContentReference
import com.wire.content.external.PlatformResult
import com.wire.content.media.ContentImageSource
import com.wire.content.media.ImageProcessingRequest
import com.wire.content.media.ImageProcessor
import com.wire.content.media.ImageResizeProfile
import com.wire.content.media.ImageTargetProvider
import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.asset.GetAvatarAssetUseCase
import com.wire.kalium.logic.feature.asset.PublicAssetResult
import com.wire.kalium.logic.feature.user.UploadAvatarResult
import com.wire.kalium.logic.feature.user.UploadUserAvatarUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okio.Path
import dev.zacsweers.metro.Inject
@Suppress("LongParameterList")
class AvatarPickerViewModel @Inject constructor(
    userDataStoreProvider: UserDataStoreProvider,
    @CurrentAccount selfUserId: UserId,
    private val getAvatarAsset: GetAvatarAssetUseCase,
    private val uploadUserAvatar: UploadUserAvatarUseCase,
    private val imageProcessor: ImageProcessor,
    private val imageTargetProvider: ImageTargetProvider,
    private val kaliumFileSystem: KaliumFileSystem,
    private val qualifiedIdMapper: QualifiedIdMapper,
) : ViewModel() {
    private val dataStore = userDataStoreProvider.getOrCreate(selfUserId)

    var pictureState by mutableStateOf<PictureState>(PictureState.Empty)
        private set
    private var initialPictureLoadingState by mutableStateOf<InitialPictureLoadingState>(InitialPictureLoadingState.None)
    private val _infoMessage = MutableSharedFlow<UIText>()
    val infoMessage = _infoMessage.asSharedFlow()
    val defaultAvatarPath: Path
        get() = kaliumFileSystem.selfUserAvatarPath()
    val temporaryAvatarTarget: PlatformResult<ExternalContentReference> = imageTargetProvider.createTarget(defaultAvatarPath)

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
                    val currentAvatarSource = ContentImageSource.Local(avatarRawPath)
                    initialPictureLoadingState = InitialPictureLoadingState.Loaded(currentAvatarSource)
                    pictureState = PictureState.Initial(currentAvatarSource)
                } ?: run {
                    initialPictureLoadingState = InitialPictureLoadingState.None
                    pictureState = PictureState.Empty
                }
            } catch (e: Exception) {
                appLogger.e("There was an error loading the user avatar", e)
                initialPictureLoadingState = InitialPictureLoadingState.None
            }
        }
    }
    fun updatePickedAvatar(
        originalReference: ExternalContentReference,
        updatedSource: ContentImageSource,
    ) = viewModelScope.launch {
        when (sanitizeAvatarImage(originalReference, defaultAvatarPath)) {
            is PlatformResult.Success -> pictureState = PictureState.Picked(updatedSource)
            else -> showInfoMessage(InfoMessageType.ImageProcessError)
        }
    }

    /**
     * Resamples the image and removes unnecessary metadata before uploading it.
     * This to avoid uploading unnecessarily large images for profile pictures and sensitive metadata.
     */
    private suspend fun sanitizeAvatarImage(originalReference: ExternalContentReference, avatarPath: Path) =
        imageProcessor.process(
            ImageProcessingRequest(
                source = originalReference,
                destination = avatarPath,
                resizeProfile = ImageResizeProfile.AVATAR,
                removeMetadata = true,
            )
        )

    fun uploadNewPickedAvatar() {
        val imageSource = pictureState.imageSource ?: return
        viewModelScope.launch {
            pictureState = PictureState.Uploading(imageSource)
            val avatarPath = defaultAvatarPath
            val imageDataSize = kaliumFileSystem.size(avatarPath)
            if (imageDataSize == null) {
                showInfoMessage(InfoMessageType.ImageProcessError)
                return@launch
            }
            when (val result = uploadUserAvatar(avatarPath, imageDataSize)) {
                is UploadAvatarResult.Success -> {
                    dataStore.updateUserAvatarAssetId(result.userAssetId.toString())
                    pictureState = PictureState.Completed(imageSource, dataStore.avatarAssetId.first())
                }
                is UploadAvatarResult.Failure -> {
                    when (result.coreFailure) {
                        is NetworkFailure.NoNetworkConnection -> showInfoMessage(InfoMessageType.NoNetworkError)
                        else -> showInfoMessage(InfoMessageType.UploadAvatarError)
                    }
                    with(initialPictureLoadingState) {
                        pictureState = when (this) {
                            is InitialPictureLoadingState.Loaded -> PictureState.Initial(this.imageSource)
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
        data class Loaded(val imageSource: ContentImageSource) : InitialPictureLoadingState()
    }

    @Stable
    sealed class PictureState(open val imageSource: ContentImageSource?) {
        data class Uploading(override val imageSource: ContentImageSource) : PictureState(imageSource)
        data class Initial(override val imageSource: ContentImageSource) : PictureState(imageSource)
        data class Picked(override val imageSource: ContentImageSource) : PictureState(imageSource)
        data class Completed(override val imageSource: ContentImageSource, val assetId: String?) : PictureState(imageSource)
        data object Empty : PictureState(null)
    }
    sealed class InfoMessageType(override val uiText: UIText) : SnackBarMessage {
        data object UploadAvatarError : InfoMessageType(UIText.StringResource(R.string.error_uploading_user_avatar))
        data object NoNetworkError : InfoMessageType(UIText.StringResource(R.string.error_no_network_message))
        data object ImageProcessError : InfoMessageType(UIText.StringResource(R.string.error_process_user_avatar))
    }
}
