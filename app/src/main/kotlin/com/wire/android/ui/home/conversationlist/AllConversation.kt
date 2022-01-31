package com.wire.android.ui.home.conversationlist

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
import com.wire.android.ui.home.conversationlist.common.EventBadgeFactory
import com.wire.android.ui.home.conversationlist.common.RowItem
import com.wire.android.ui.main.conversationlist.common.UserLabel
import com.wire.android.ui.home.conversationlist.common.folderWithElements
import com.wire.android.ui.home.conversationlist.model.Conversation
import com.wire.android.ui.home.conversationlist.model.ConversationFolder
import com.wire.android.ui.home.conversationlist.model.NewActivity
import com.wire.android.ui.home.conversationlist.model.toUserInfoLabel


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
                ConversationRowItem(
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
private fun ConversationRowItem(conversation: Conversation, onConversationItemClick: () -> Unit) {
    RowItem(onRowItemClick = onConversationItemClick) {
        ConversationLabel(conversation)
    }
}

@Composable
private fun ConversationLabel(conversation: Conversation) {
    UserProfileAvatar(avatarUrl = conversation.userInfo.avatarUrl, onClick = {})
    UserLabel(conversation.toUserInfoLabel())
}



