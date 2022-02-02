package com.wire.android.ui.home.conversationslist

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.home.conversations.common.ConversationItemFactory
import com.wire.android.ui.home.conversationslist.common.folderWithElements
import com.wire.android.ui.home.conversationslist.model.ConversationFolder
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.home.conversationslist.model.NewActivity


@Composable
fun AllConversationScreen(
    newActivities: List<NewActivity>,
    conversations: Map<ConversationFolder, List<ConversationItem>>,
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
    conversations: Map<ConversationFolder, List<ConversationItem>>,
    onConversationItemClick: (String) -> Unit,
) {
    LazyColumn {
        folderWithElements(
            header = { stringResource(id = R.string.conversation_label_new_activity) },
            items = newActivities
        ) { newActivity ->
            ConversationItemFactory(
                conversation = newActivity.conversationItem,
                eventType = newActivity.eventType,
                onConversationItemClick = { onConversationItemClick("someId") }
            )
        }

        conversations.forEach { (conversationFolder, conversationList) ->
            folderWithElements(
                header = { conversationFolder.folderName },
                items = conversationList
            ) { conversation ->
                ConversationItemFactory(
                    conversation = conversation,
                    onConversationItemClick = { onConversationItemClick("someId") }
                )
            }
        }
    }
}
