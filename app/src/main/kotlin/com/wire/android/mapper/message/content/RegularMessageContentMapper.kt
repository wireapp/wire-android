package com.wire.android.mapper.message.content

import com.wire.android.R
import com.wire.android.model.ImageAsset
import com.wire.android.ui.home.conversations.model.AttachmentType
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.QuotedMessageUIData
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.util.time.ISOFormatter
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.asset.isDisplayableImageMimeType
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.AssetContent
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.data.user.AssetId
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.User
import com.wire.kalium.logic.util.isGreaterThan

class RegularMessageContentMapper(
    private val wireSessionImageLoader: WireSessionImageLoader,
    private val isoFormatter: ISOFormatter,
    private val messageResourceProvider: MessageResourceProvider
) {

    fun mapRegularMessage(
        message: Message.Regular,
        sender: User?
    ): UIMessageContent =
        when (val content = message.content) {
            is MessageContent.Asset ->
                toRegularAsset(
                    message = message,
                    assetMessageContent = content,
                    sender = sender
                )

            is MessageContent.RestrictedAsset ->
                toRestrictedAsset(
                    mimeType = content.mimeType,
                    assetSize = content.sizeInBytes,
                    assetName = content.name
                )

            else ->
                toMessageBody(
                    conversationId = message.conversationId,
                    content = content
                )
        }


    private fun toRegularAsset(
        message: Message,
        assetMessageContent: MessageContent.Asset,
        sender: User?
    ): UIMessageContent {
        return when (val metadata = assetMessageContent.value.metadata) {
            is AssetContent.AssetMetadata.Audio -> {
                mapAudio(message, metadata)
            }

            is AssetContent.AssetMetadata.Image -> {
                mapImage(message, assetMessageContent, metadata)
            }

            is AssetContent.AssetMetadata.Video -> {
                mapVideo(message, assetMessageContent, metadata)
            }

            null -> {

            }
        }
    }

    private fun mapAudio(
        message: Message,
        metadata: AssetContent.AssetMetadata.Audio
    ) {

    }

    private fun mapImage(
        message: Message,
        assetContent: AssetContent,
        metadata: AssetContent.AssetMetadata.Image
    ) {
        val assetMessageContentMetadata = AssetMessageContentMetadata(assetContent)

        val imgWidth = metadata.width
        val imgHeight = metadata.height

        with(metadata) {
            when {
                shouldBeDisplayed -> {
                    UIMessageContent.PreviewAssetMessage
                }

                // If it's an image, we delegate the download it right away to coil
                assetMessageContentMetadata.isDisplayableImage() -> {
                    UIMessageContent.ImageMessage(
                        assetId = AssetId(remoteData.assetId, remoteData.assetDomain.orEmpty()),
                        asset = ImageAsset.PrivateAsset(
                            wireSessionImageLoader,
                            message.conversationId,
                            message.id,
                            sender is SelfUser
                        ),
                        width = imgWidth,
                        height = imgHeight,
                        uploadStatus = uploadStatus,
                        downloadStatus = downloadStatus
                    )
                }

                // It's a generic Asset Message so let's not download it yet
                else -> {
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
        }
    }

    private fun mapVideo(
        message: Message,
        metadata: AssetContent.AssetMetadata.Video
    ) {

    }

    private fun toRestrictedAsset(
        mimeType: String,
        assetSize: Long,
        assetName: String
    ): UIMessageContent {
        return UIMessageContent.RestrictedAsset(
            mimeType = mimeType,
            assetSizeInBytes = assetSize,
            assetName = assetName
        )
    }

    private fun String?.orUnknownName(): UIText = when {
        this != null -> UIText.DynamicString(this)
        else -> UIText.StringResource(R.string.username_unavailable_label)

    }

    private fun toMessageBody(conversationId: ConversationId, content: MessageContent) = MessageBody(
        when (content) {
            is MessageContent.Text -> UIText.DynamicString(content.value, content.mentions)
            is MessageContent.Unknown -> UIText.StringResource(
                messageResourceProvider.sentAMessageWithContent, content.typeName ?: "Unknown"
            )

            is MessageContent.FailedDecryption -> UIText.StringResource(R.string.label_message_decryption_failure_message)
            else -> UIText.StringResource(messageResourceProvider.sentAMessageWithContent, "Unknown")
        },
        quotedMessage = (content as? MessageContent.Text)?.quotedMessageDetails?.let { mapQuoteData(conversationId, it) }
    ).let { messageBody -> UIMessageContent.TextMessage(messageBody = messageBody) }

    private fun mapQuoteData(conversationId: ConversationId, it: MessageContent.QuotedMessageDetails) = QuotedMessageUIData(
        it.messageId,
        it.senderId,
        it.senderName.orUnknownName(),
        UIText.StringResource(R.string.label_quote_original_message_date, isoFormatter.fromISO8601ToTimeFormat(it.timeInstant.toString())),
        it.editInstant?.let { instant ->
            UIText.StringResource(R.string.label_message_status_edited_with_date, isoFormatter.fromISO8601ToTimeFormat(instant.toString()))
        },
        when (val quotedContent = it.quotedContent) {
            is MessageContent.QuotedMessageDetails.Asset -> when (AttachmentType.fromMimeTypeString(quotedContent.assetMimeType)) {
                AttachmentType.IMAGE -> QuotedMessageUIData.DisplayableImage(
                    ImageAsset.PrivateAsset(
                        wireSessionImageLoader,
                        conversationId,
                        it.messageId,
                        it.isQuotingSelfUser
                    )
                )

                AttachmentType.GENERIC_FILE -> QuotedMessageUIData.GenericAsset(
                    quotedContent.assetName,
                    quotedContent.assetMimeType
                )
            }

            is MessageContent.QuotedMessageDetails.Text -> QuotedMessageUIData.Text(quotedContent.value)
            MessageContent.QuotedMessageDetails.Deleted -> QuotedMessageUIData.Deleted
            MessageContent.QuotedMessageDetails.Invalid -> QuotedMessageUIData.Invalid
        }
    )

}

class AssetMessageContentMetadata(val assetMessageContent: AssetContent) {
    val imgWidth
        get() = when (val md = assetMessageContent.metadata) {
            is AssetContent.AssetMetadata.Image -> md.width
            else -> 0
        }

    val imgHeight
        get() = when (val md = assetMessageContent.metadata) {
            is AssetContent.AssetMetadata.Image -> md.height
            else -> 0
        }

    fun isDisplayableImage(): Boolean = isDisplayableImageMimeType(assetMessageContent.mimeType) &&
            imgWidth.isGreaterThan(0) && imgHeight.isGreaterThan(0)
}
