package com.wire.android.ui.main.conversation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.main.conversation.model.Conversation
import com.wire.android.ui.main.conversation.model.ConversationFolder
import com.wire.android.ui.main.conversation.all.model.NewActivity
import com.wire.android.ui.main.conversation.model.toUserInfoLabel
import com.wire.android.ui.main.conversation.common.components.EventBadgeFactory
import com.wire.android.ui.main.conversation.common.components.RowItem
import com.wire.android.ui.main.conversation.common.components.UserLabel
import com.wire.android.ui.main.conversation.common.extension.folderWithElements


@Composable
fun AllConversationScreen(
    newActivities: List<NewActivity>,
    conversations: Map<ConversationFolder, List<Conversation>>
) {
    AllConversationContent(
        newActivities = newActivities,
        conversations = conversations
    )
}

@Composable
private fun AllConversationContent(
    newActivities: List<NewActivity>,
    conversations: Map<ConversationFolder, List<Conversation>>
) {
    LazyColumn {
        folderWithElements(
            header = { stringResource(id = R.string.conversation_label_new_activity) },
            items = newActivities
        ) { newActivity ->
            NewActivityRowItem(newActivity = newActivity)
        }

        conversations.forEach { (conversationFolder, conversationList) ->
            folderWithElements(
                header = { conversationFolder.folderName },
                items = conversationList
            ) { conversation ->
                ConversationRowItem(conversation = conversation)
            }
        }
    }
}

@Composable
private fun NewActivityRowItem(newActivity: NewActivity) {
    RowItem {
        ConversationLabel(newActivity.conversation)
        Box(modifier = Modifier.fillMaxWidth()) {
            EventBadgeFactory(
                eventType = newActivity.eventType,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
            )
        }
    }
}

@Composable
private fun ConversationRowItem(conversation: Conversation) {
    RowItem {
        ConversationLabel(conversation)
    }
}

@Composable
private fun ConversationLabel(conversation: Conversation) {
    UserProfileAvatar(avatarUrl = conversation.userInfo.avatarUrl, onClick = {})
    UserLabel(conversation.toUserInfoLabel())
}



