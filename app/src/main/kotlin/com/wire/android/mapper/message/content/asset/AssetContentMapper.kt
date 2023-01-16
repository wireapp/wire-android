package com.wire.android.mapper.message.content.asset

import com.wire.android.model.ImageAsset
import com.wire.android.ui.home.conversations.model.AudioMessageDuration
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.asset.isDisplayableImageMimeType
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.AssetContent
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.user.AssetId
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.User
import com.wire.kalium.logic.util.isGreaterThan
import javax.inject.Inject

class AssetContentMapper @Inject constructor(
    private val wireSessionImageLoader: WireSessionImageLoader
) {

    fun toRegularAsset(
        message: Message,
        assetContent: AssetContent,
        sender: User?
    ): UIMessageContent {
        return when (val metadata = assetContent.metadata) {
            is AssetContent.AssetMetadata.Audio -> {
                mapAudio(
                    assetContent = assetContent,
                    metadata = metadata
                )
            }

            is AssetContent.AssetMetadata.Image -> {
                mapImage(
                    message = message,
                    assetContent = assetContent,
                    sender = sender
                )
            }

            is AssetContent.AssetMetadata.Video -> {
                mapVideo(
                    message = message,
                    assetContent = assetContent,
                    metadata = metadata,
                    sender = sender
                )
            }

            // no meta data information, we do not know what it is,
            // so we load is lazily and let the user decide what to do with it
            null -> {
                lazyLoadedAsset(
                    assetContent = assetContent
                )
            }
        }
    }

    private fun mapAudio(
        assetContent: AssetContent,
        metadata: AssetContent.AssetMetadata.Audio,
    ): UIMessageContent {
        with(assetContent) {
            return UIMessageContent.AudioAssetMessage(
                assetName = name ?: "",
                assetExtension = mimeType,
                assetId = AssetId(remoteData.assetId, remoteData.assetDomain.orEmpty()),
                audioMessageDuration = AudioMessageDuration(metadata.durationMs ?: 0),
                uploadStatus = uploadStatus,
                downloadStatus = downloadStatus
            )
        }
    }

    private fun mapImage(
        message: Message,
        assetContent: AssetContent,
        sender: User?
    ): UIMessageContent {
        val assetMessageContentMetadata = AssetMessageContentMetadata(assetContent)

        return with(assetContent) {
            when {
                !shouldBeDisplayed -> {
                    UIMessageContent.PreviewAssetMessage
                }

                // If it's an image, we delegate the download it right away to coil
                assetMessageContentMetadata.isDisplayableImage() -> {
                    displayableImageAsset(
                        conversationId = message.conversationId,
                        messageId = message.id,
                        assetMessageContentMetadata = assetMessageContentMetadata,
                        isSelfUserTheSender = sender is SelfUser
                    )
                }

                // It's a generic Asset Message so let's not download it yet,
                // let the user decide when to download on  asset click
                else -> {
                    lazyLoadedAsset(
                        assetContent = assetContent
                    )
                }
            }
        }
    }

    private fun displayableImageAsset(
        conversationId: ConversationId,
        messageId: String,
        assetMessageContentMetadata: AssetMessageContentMetadata,
        isSelfUserTheSender: Boolean,
    ): UIMessageContent.ImageMessage {
        return with(assetMessageContentMetadata.assetMessageContent) {
            UIMessageContent.ImageMessage(
                assetId = AssetId(remoteData.assetId, remoteData.assetDomain.orEmpty()),
                asset = ImageAsset.PrivateAsset(
                    imageLoader = wireSessionImageLoader,
                    conversationId = conversationId,
                    messageId = messageId,
                    isSelfAsset = isSelfUserTheSender
                ),
                width = assetMessageContentMetadata.imgWidth,
                height = assetMessageContentMetadata.imgHeight,
                uploadStatus = uploadStatus,
                downloadStatus = downloadStatus
            )
        }
    }

    private fun lazyLoadedAsset(
        assetContent: AssetContent
    ): UIMessageContent.AssetMessage {
        return with(assetContent) {
            UIMessageContent.AssetMessage(
                assetName = name ?: "",
                assetExtension = name?.split(".")?.last() ?: "",
                assetId = AssetId(remoteData.assetId, remoteData.assetDomain.orEmpty()),
                assetSizeInBytes = sizeInBytes,
                uploadStatus = uploadStatus,
                downloadStatus = downloadStatus
            )
        }
    }

    // TODO: once we support video, we will expand this logic, for now we just return a generic asset message
    private fun mapVideo(
        message: Message,
        assetContent: AssetContent,
        metadata: AssetContent.AssetMetadata.Video,
        sender: User?
    ): UIMessageContent {
        return lazyLoadedAsset(assetContent)
    }

}

class AssetMessageContentMetadata(val assetMessageContent: AssetContent) {
    val imgWidth: Int
        get() = when (val md = assetMessageContent.metadata) {
            is AssetContent.AssetMetadata.Image -> md.width
            else -> 0
        }

    val imgHeight: Int
        get() = when (val md = assetMessageContent.metadata) {
            is AssetContent.AssetMetadata.Image -> md.height
            else -> 0
        }

    fun isDisplayableImage(): Boolean = isDisplayableImageMimeType(assetMessageContent.mimeType) &&
            imgWidth.isGreaterThan(0) && imgHeight.isGreaterThan(0)
}


data class AudioMessageDuration(val durationMs: Long = 0, val currentPositionMs: Long = 0) {

    private val timeInSeconds = durationMs / 1000
    val formattedTimeLeft
        get() = run {
            val minutes = timeInSeconds / 60
            val seconds = timeInSeconds % 60
            val formattedSeconds = String.format("%02d", seconds)

            "$minutes:$formattedSeconds"
        }
}
