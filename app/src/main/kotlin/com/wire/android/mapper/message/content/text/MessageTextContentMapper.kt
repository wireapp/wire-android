package com.wire.android.mapper.message.content.text

import com.wire.android.R
import com.wire.android.mapper.message.content.MessageResourceProvider
import com.wire.android.model.ImageAsset
import com.wire.android.ui.home.conversations.model.AttachmentType
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.QuotedMessageUIData
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.util.time.ISOFormatter
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.MessageContent
import javax.inject.Inject

class MessageTextContentMapper @Inject constructor(
    private val messageResourceProvider: MessageResourceProvider,
    private val wireSessionImageLoader: WireSessionImageLoader,
    private val isoFormatter: ISOFormatter
) {

    fun toTextMessage(conversationId: ConversationId, content: MessageContent): UIMessageContent.TextMessage {
        val messageBody = MessageBody(
            when (content) {
                is MessageContent.Text -> UIText.DynamicString(content.value, content.mentions)
                is MessageContent.Unknown -> UIText.StringResource(
                    messageResourceProvider.sentAMessageWithContent, content.typeName ?: "Unknown"
                )

                is MessageContent.FailedDecryption -> UIText.StringResource(R.string.label_message_decryption_failure_message)
                else -> UIText.StringResource(messageResourceProvider.sentAMessageWithContent, "Unknown")
            },
            quotedMessage = (content as? MessageContent.Text)?.quotedMessageDetails?.let { mapQuoteData(conversationId, it) }
        )

        return UIMessageContent.TextMessage(messageBody = messageBody)
    }

    private fun mapQuoteData(conversationId: ConversationId, it: MessageContent.QuotedMessageDetails) = QuotedMessageUIData(
        messageId = it.messageId,
        senderId = it.senderId,
        senderName = it.senderName.orUnknownName(),
        originalMessageDateDescription = UIText.StringResource(
            R.string.label_quote_original_message_date,
            isoFormatter.fromISO8601ToTimeFormat(it.timeInstant.toString())
        ),
        editedTimeDescription = it.editInstant?.let { instant ->
            UIText.StringResource(R.string.label_message_status_edited_with_date, isoFormatter.fromISO8601ToTimeFormat(instant.toString()))
        },
        quotedContent = when (val quotedContent = it.quotedContent) {
            is MessageContent.QuotedMessageDetails.Asset -> when (AttachmentType.fromMimeTypeString(quotedContent.assetMimeType)) {
                AttachmentType.IMAGE -> QuotedMessageUIData.DisplayableImage(
                    displayable = ImageAsset.PrivateAsset(
                        wireSessionImageLoader,
                        conversationId,
                        it.messageId,
                        it.isQuotingSelfUser
                    )
                )

                AttachmentType.AUDIO -> QuotedMessageUIData.AudioMessage(assetName = quotedContent.assetName)

                AttachmentType.GENERIC_FILE -> QuotedMessageUIData.GenericAsset(
                    assetName = quotedContent.assetName,
                    assetMimeType = quotedContent.assetMimeType
                )
            }

            is MessageContent.QuotedMessageDetails.Text -> QuotedMessageUIData.Text(quotedContent.value)
            MessageContent.QuotedMessageDetails.Deleted -> QuotedMessageUIData.Deleted
            MessageContent.QuotedMessageDetails.Invalid -> QuotedMessageUIData.Invalid
        }
    )

    private fun String?.orUnknownName(): UIText = when {
        this != null -> UIText.DynamicString(this)
        else -> UIText.StringResource(R.string.username_unavailable_label)
    }
}
