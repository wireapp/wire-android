package com.wire.android.ui.home.conversations.model

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.home.conversations.MessageItem
import com.wire.android.ui.home.conversations.MessageItemTest
import com.wire.android.ui.home.conversations.MessageText
import com.wire.android.ui.home.conversations.SystemMessageItem
import com.wire.android.ui.home.conversations.mock.mockAssetContent
import com.wire.android.ui.home.conversations.mock.mockAssetMessage
import com.wire.android.ui.home.conversations.mock.mockMessageBody
import com.wire.android.ui.home.conversations.mock.mockMessageWithText
import com.wire.android.ui.home.conversations.mock.mockedImageUIMessage
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
        onOpenProfile = { _ -> },
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
        onOpenProfile = { _ -> },
        onReactionClicked = { _, _ -> },
        onResetSessionClicked = { _, _ -> },
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewImageMessageUploaded() {
    MessageItem(
        message = mockedImageUIMessage(Message.UploadStatus.UPLOADED),
        onLongClicked = {},
        onAssetMessageClicked = {},
        onImageMessageClicked = { _, _ -> },
        onOpenProfile = { _ -> },
        onReactionClicked = { _, _ -> },
        onResetSessionClicked = { _, _ -> },
        onAudioClick = { _ -> }
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewImageMessageUploading() {
    MessageItem(
        message = mockedImageUIMessage(Message.UploadStatus.UPLOAD_IN_PROGRESS),
        onLongClicked = {},
        onAssetMessageClicked = {},
        onImageMessageClicked = { _, _ -> },
        onOpenProfile = { _ -> },
        onReactionClicked = { _, _ -> },
        onResetSessionClicked = { _, _ -> },
        onAudioClick = { _ -> }
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewImageMessageFailedUpload() {
    MessageItem(
        message = mockedImageUIMessage(Message.UploadStatus.FAILED_UPLOAD),
        onLongClicked = {},
        onAssetMessageClicked = {},
        onImageMessageClicked = { _, _ -> },
        onOpenProfile = { _ -> },
        onReactionClicked = { _, _ -> },
        onResetSessionClicked = { _, _ -> },
        onAudioClick = { _ -> }
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewMessageWithSystemMessage() {
    Column {
        MessageItem(
            message = mockMessageWithText,
            onLongClicked = {},
            onAssetMessageClicked = {},
            onImageMessageClicked = { _, _ -> },
            onOpenProfile = { _ -> },
            onReactionClicked = { _, _ -> },
            onResetSessionClicked = { _, _ -> },
            onAudioClick = { _ -> }
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
