package com.wire.android.ui.home.conversations.model

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.home.conversations.MessageItemTest
import com.wire.android.ui.home.conversations.MessageText
import com.wire.android.ui.home.conversations.SystemMessageItem
import com.wire.android.ui.home.conversations.mock.mockAssetContent
import com.wire.android.ui.home.conversations.mock.mockAssetMessage
import com.wire.android.ui.home.conversations.mock.mockMessageBody
import com.wire.android.ui.home.conversations.mock.mockMessageWithText
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.user.UserId

private val previewUserId = UserId("value", "domain")

@Preview(showBackground = true)
@Composable
fun PreviewMessage() {
    MessageItemTest(
        message = mockMessageWithText.copy(
            messageHeader = mockMessageWithText.messageHeader.copy(
                username = UIText.DynamicString(
                    "Pablo Diego José Francisco de Paula Juan Nepomuceno María de los Remedios Cipriano de la Santísima Trinidad " +
                            "Ruiz y Picasso"
                )
            )
        ),
        messageContent = {
            MessageText(
                messageBody = mockMessageBody,
                onOpenProfile = {},
                onLongClick = {}
            )
        },
        onLongClicked = {},
        onOpenProfile = {},
        onReactionClicked = { _, _ -> },
        onResetSessionClicked = { _, _ -> }
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewMessageWithReply() {
    MessageItemTest(
        message = mockMessageWithText.copy(
            messageHeader = mockMessageWithText.messageHeader.copy(
                username = UIText.DynamicString(
                    "Don Joe"
                )
            )
        ),
        onLongClicked = {},
        onOpenProfile = { _ -> },
        onReactionClicked = { _, _ -> },
        onResetSessionClicked = { _, _ -> },
        messageContent = {
            MessageText(
                messageBody = MessageBody(
                    message = UIText.DynamicString("Sure, go ahead!"),
                    quotedMessage = QuotedMessageUIData(
                        messageId = "asdoij",
                        senderId = previewUserId,
                        senderName = UIText.DynamicString("John Doe"),
                        originalMessageDateDescription = UIText.StringResource(R.string.label_quote_original_message_date, "10:30"),
                        editedTimeDescription = UIText.StringResource(R.string.label_message_status_edited_with_date, "10:32"),
                        quotedContent = QuotedMessageUIData.Text("Hey, can I call right now?")
                    )
                ),
                onLongClick = { },
                onOpenProfile = { }
            )
        }
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewEditedMessage() {
    MessageItemTest(
        message = mockMessageWithText.let {
            it.copy(messageHeader = it.messageHeader.copy(messageStatus = MessageStatus.Edited("")))
        },
        messageContent = {
            MessageText(
                messageBody = mockMessageBody,
                onOpenProfile = {},
                onLongClick = {}
            )
        },
        onLongClicked = {},
        onOpenProfile = {},
        onReactionClicked = { _, _ -> },
        onResetSessionClicked = { _, _ -> }
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewAssetMessage() {
    MessageItemTest(
        message = mockAssetMessage,
        messageContent = {
            MessageGenericAsset(
                assetName = mockAssetContent.assetName,
                assetExtension = mockAssetContent.assetExtension,
                assetSizeInBytes = mockAssetContent.assetSizeInBytes,
                assetUploadStatus = mockAssetContent.uploadStatus,
                assetDownloadStatus = mockAssetContent.downloadStatus,
                onAssetClick = Clickable()
            )
        },
        onLongClicked = {},
        onOpenProfile = {},
        onReactionClicked = { _, _ -> },
        onResetSessionClicked = { _, _ -> },
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewImageMessageUploading() {
    MessageItemTest(
        message = mockAssetMessage,
        messageContent = {
            MessageGenericAsset(
                assetName = mockAssetContent.assetName,
                assetExtension = mockAssetContent.assetExtension,
                assetSizeInBytes = mockAssetContent.assetSizeInBytes,
                assetUploadStatus = Message.UploadStatus.UPLOAD_IN_PROGRESS,
                assetDownloadStatus = mockAssetContent.downloadStatus,
                onAssetClick = Clickable()
            )
        },
        onLongClicked = {},
        onOpenProfile = {},
        onReactionClicked = { _, _ -> },
        onResetSessionClicked = { _, _ -> },
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewImageMessageFailedUpload() {
    MessageItemTest(
        message = mockAssetMessage,
        messageContent = {
            MessageGenericAsset(
                assetName = mockAssetContent.assetName,
                assetExtension = mockAssetContent.assetExtension,
                assetSizeInBytes = mockAssetContent.assetSizeInBytes,
                assetUploadStatus = Message.UploadStatus.FAILED_UPLOAD,
                assetDownloadStatus = mockAssetContent.downloadStatus,
                onAssetClick = Clickable()
            )
        },
        onLongClicked = {},
        onOpenProfile = {},
        onReactionClicked = { _, _ -> },
        onResetSessionClicked = { _, _ -> },
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewMessageWithSystemMessage() {
    Column {
        MessageItemTest(
            message = mockMessageWithText,
            messageContent = {
                MessageText(
                    messageBody = mockMessageBody,
                    onOpenProfile = {},
                    onLongClick = {}
                )
            },
            onLongClicked = {},
            onOpenProfile = {},
            onReactionClicked = { _, _ -> },
            onResetSessionClicked = { _, _ -> }
        )
        SystemMessageItem(UIMessageContent.SystemMessage.MissedCall.YouCalled(UIText.DynamicString("You")))
        SystemMessageItem(
            UIMessageContent.SystemMessage.MemberAdded(
                UIText.DynamicString("You"),
                listOf(UIText.DynamicString("Adam Smith"))
            )
        )
    }
}
