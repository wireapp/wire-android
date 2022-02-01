package com.wire.android.ui.home.conversationslist

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
import com.wire.android.ui.home.conversations.common.ConversationUserAvatar
import com.wire.android.ui.home.conversationslist.common.EventBadgeFactory
import com.wire.android.ui.home.conversationslist.common.RowItem
import com.wire.android.ui.home.conversationslist.common.folderWithElements
import com.wire.android.ui.home.conversationslist.model.Conversation
import com.wire.android.ui.home.conversationslist.model.ConversationFolder
import com.wire.android.ui.home.conversationslist.model.NewActivity
import com.wire.android.ui.home.conversationslist.model.toUserInfoLabel
import com.wire.android.ui.main.conversationlist.common.UserLabel


@Composable
fun AllConversationScreen(
    newActivities: List<NewActivity>,
    conversations: Map<ConversationFolder, List<Conversation>>,
    onOpenConversationClick: (String) -> Unit
) {
    AllConversationContent(
        newActivities = newActivities,
        conversations = conversations,
        onOpenConversationClick
    )
}

@Composable
private fun AllConversationContent(
    newActivities: List<NewActivity>,
    conversations: Map<ConversationFolder, List<Conversation>>,
    onOpenConversationClick: (String) -> Unit,
) {
    LazyColumn {
        folderWithElements(
            header = { stringResource(id = R.string.conversation_label_new_activity) },
            items = newActivities
        ) { newActivity ->
            NewActivityRowItem(
                newActivity = newActivity,
                onConversationItemClick = onOpenConversationClick
            )
        }

        conversations.forEach { (conversationFolder, conversationList) ->
            folderWithElements(
                header = { conversationFolder.folderName },
                items = conversationList
            ) { conversation ->
                ConversationRowItem(
                    conversation = conversation,
                    onConversationItemClick = onOpenConversationClick
                )
            }
        }
    }
}

@Composable
private fun NewActivityRowItem(
    newActivity: NewActivity,
    onConversationItemClick: (String) -> Unit
) {
    RowItem(onRowItemClick = { onConversationItemClick("someId") }) {
        ConversationLabel(conversation = newActivity.conversation)
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
private fun ConversationRowItem(conversation: Conversation, onConversationItemClick: (String) -> Unit) {
    RowItem(onRowItemClick = { onConversationItemClick("someId") } ) {
        ConversationLabel(conversation)
    }
}

@Composable
private fun ConversationLabel(conversation: Conversation) {
    ConversationUserAvatar(avatarUrl = conversation.userInfo.avatarUrl)
    UserLabel(conversation.toUserInfoLabel())
}



