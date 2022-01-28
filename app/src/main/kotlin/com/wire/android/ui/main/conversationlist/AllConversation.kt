package com.wire.android.ui.main.conversationlist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.main.conversationlist.common.EventBadgeFactory
import com.wire.android.ui.main.conversationlist.common.GroupConversationAvatar
import com.wire.android.ui.main.conversationlist.common.RowItem
import com.wire.android.ui.main.conversationlist.common.UserLabel
import com.wire.android.ui.main.conversationlist.common.folderWithElements
import com.wire.android.ui.main.conversationlist.model.Conversation
import com.wire.android.ui.main.conversationlist.model.Conversation.GroupConversation
import com.wire.android.ui.main.conversationlist.model.Conversation.PrivateConversation
import com.wire.android.ui.main.conversationlist.model.ConversationFolder
import com.wire.android.ui.main.conversationlist.model.NewActivity
import com.wire.android.ui.main.conversationlist.model.toUserInfoLabel
import com.wire.android.ui.theme.body02


@Composable
fun AllConversationScreen(
    newActivities: List<NewActivity>,
    conversations: Map<ConversationFolder, List<Conversation>>,
    //TODO: This is going to be replaced with proper lambda, test purpose only
    onConversationItemClick: () -> Unit
) {
    AllConversationContent(
        newActivities = newActivities,
        conversations = conversations,
        onConversationItemClick
    )
}

@Composable
private fun AllConversationContent(
    newActivities: List<NewActivity>,
    conversations: Map<ConversationFolder, List<Conversation>>,
    onConversationItemClick: () -> Unit,
) {
    LazyColumn {
        folderWithElements(
            header = { stringResource(id = R.string.conversation_label_new_activity) },
            items = newActivities
        ) { newActivity ->
            NewActivityRowItem(
                newActivity = newActivity,
                onConversationItemClick
            )
        }

        conversations.forEach { (conversationFolder, conversationList) ->
            folderWithElements(
                header = { conversationFolder.folderName },
                items = conversationList
            ) { conversation ->
                ConversationItem(
                    conversation = conversation,
                    onConversationItemClick
                )
            }
        }
    }
}

@Composable
private fun NewActivityRowItem(
    newActivity: NewActivity,
    onConversationItemClick: () -> Unit
) {
    RowItem(onRowItemClick = onConversationItemClick) {
        createConversationLabel(conversation = newActivity.conversation)
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
private fun createConversationLabel(conversation: Conversation) {
    when (conversation) {
        is GroupConversation -> {
            with(conversation) {
                GroupConversationAvatar(groupColorValue)
                GroupName(groupName)
            }
        }
        is PrivateConversation -> {
            with(conversation) {
                UserProfileAvatar(avatarUrl = userInfo.avatarUrl, onClick = {})
                UserLabel(toUserInfoLabel())
            }
        }
    }
}

@Composable
private fun ConversationItem(conversation: Conversation, onConversationItemClick: () -> Unit) {
    RowItem(onRowItemClick = onConversationItemClick) {
        createConversationLabel(conversation = conversation)
    }
}

@Composable
 fun GroupName(name: String) {
    Text(
        text = name,
        style = MaterialTheme.typography.body02
    )
}


