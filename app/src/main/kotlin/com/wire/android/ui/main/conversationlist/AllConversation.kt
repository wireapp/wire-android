package com.wire.android.ui.main.conversationlist

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.main.conversationlist.common.ConversationItem
import com.wire.android.ui.main.conversationlist.common.GroupConversationAvatar
import com.wire.android.ui.main.conversationlist.common.GroupName
import com.wire.android.ui.main.conversationlist.common.UserLabel
import com.wire.android.ui.main.conversationlist.common.folderWithElements
import com.wire.android.ui.main.conversationlist.model.Conversation
import com.wire.android.ui.main.conversationlist.model.Conversation.GroupConversation
import com.wire.android.ui.main.conversationlist.model.Conversation.PrivateConversation
import com.wire.android.ui.main.conversationlist.model.ConversationFolder
import com.wire.android.ui.main.conversationlist.model.EventType
import com.wire.android.ui.main.conversationlist.model.NewActivity
import com.wire.android.ui.main.conversationlist.model.toUserInfoLabel


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
            with(newActivity) {
                AllConversationItem(
                    conversation = conversation,
                    eventType = eventType
                )
            }
        }

        conversations.forEach { (conversationFolder, conversationList) ->
            folderWithElements(
                header = { conversationFolder.folderName },
                items = conversationList
            ) { conversation ->
                AllConversationItem(
                    conversation = conversation,
                )
            }
        }
    }
}

@Composable
private fun AllConversationItem(conversation: Conversation, eventType: EventType? = null) {
    when (conversation) {
        is GroupConversation -> {
            with(conversation) {
                ConversationItem(
                    leadingIcon = {
                        GroupConversationAvatar(colorValue = groupColorValue)
                    },
                    title = { GroupName(groupName) },
                    eventType = eventType
                )
            }
        }
        is PrivateConversation -> {
            ConversationItem(
                leadingIcon = {
                    UserProfileAvatar()
                },
                title = { UserLabel(conversation.toUserInfoLabel()) },
                eventType = eventType
            )
        }
    }
}
