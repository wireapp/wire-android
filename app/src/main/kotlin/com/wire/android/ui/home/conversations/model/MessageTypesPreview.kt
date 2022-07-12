package com.wire.android.ui.home.conversations.model

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.ui.home.conversations.MessageItem
import com.wire.android.ui.home.conversations.SystemMessageItem
import com.wire.android.ui.home.conversations.mock.mockAssetMessage
import com.wire.android.ui.home.conversations.mock.mockMessageWithText
import com.wire.android.util.ui.UIText

@Preview(showBackground = true)
@Composable
fun PreviewMessage() {
    MessageItem(mockMessageWithText, {}, {}, { _, _ -> })
}

@Preview(showBackground = true)
@Composable
fun PreviewDeletedMessage() {
    MessageItem(mockMessageWithText.let {
        it.copy(messageHeader = it.messageHeader.copy(messageStatus = MessageStatus.Edited("")))
    }, {}, {}, { _, _ -> })
}

@Preview(showBackground = true)
@Composable
fun PreviewAssetMessage() {
    MessageItem(mockAssetMessage, {}, {}, { _, _ -> })
}

@Preview(showBackground = true)
@Composable
fun PreviewMessageWithSystemMessage() {
    Column {
        MessageItem(mockMessageWithText, {}, {}, { _, _ -> })
        SystemMessageItem(MessageContent.SystemMessage.MissedCall(UIText.DynamicString("You")))
        SystemMessageItem(
            MessageContent.SystemMessage.MemberAdded(
                UIText.DynamicString("You"),
                listOf(UIText.DynamicString("Adam Smith"))
            )
        )
    }
}
