package com.wire.android.ui.home.conversations.model

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.ui.home.conversations.MessageItem
import com.wire.android.ui.home.conversations.SystemMessageItem
import com.wire.android.ui.home.conversations.mock.mockAssetMessage
import com.wire.android.ui.home.conversations.mock.mockMessageWithText
import com.wire.android.ui.home.conversations.mock.mockedImageUIMessage
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.message.Message

@Preview(showBackground = true)
@Composable
fun PreviewMessage() {
    MessageItem(
        message = mockMessageWithText.copy(
            messageHeader = mockMessageWithText.messageHeader.copy(
                username = UIText.DynamicString(
                    "Pablo Diego José Francisco de Paula Juan Nepomuceno María de los Remedios Cipriano de la Santísima Trinidad " +
                            "Ruiz y Picasso"
                )
            )
        ),
        onLongClicked = {},
        onAssetMessageClicked = {},
        onImageMessageClicked = { _, _ -> },
        onAvatarClicked = { _, _ -> }
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewDeletedMessage() {
    MessageItem(
        message = mockMessageWithText.let {
            it.copy(messageHeader = it.messageHeader.copy(messageStatus = MessageStatus.Edited("")))
        },
        onLongClicked = {},
        onAssetMessageClicked = {},
        onImageMessageClicked = { _, _ -> },
        onAvatarClicked = { _, _ -> }
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewAssetMessage() {
    MessageItem(
        message = mockAssetMessage(),
        onLongClicked = {},
        onAssetMessageClicked = {},
        onImageMessageClicked = { _, _ -> },
        onAvatarClicked = { _, _ -> }
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
        onAvatarClicked = { _, _ -> }
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
        onAvatarClicked = { _, _ -> }
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
        onAvatarClicked = { _, _ -> }
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
            onAvatarClicked = { _, _ -> })
        SystemMessageItem(UIMessageContent.SystemMessage.MissedCall.YouCalled(UIText.DynamicString("You")))
        SystemMessageItem(
            UIMessageContent.SystemMessage.MemberAdded(
                UIText.DynamicString("You"),
                listOf(UIText.DynamicString("Adam Smith"))
            )
        )
    }
}
