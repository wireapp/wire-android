package com.wire.android.ui.home.conversationslist

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.home.conversations.common.ConversationItemFactory
import com.wire.android.ui.home.conversationslist.common.folderWithElements
import com.wire.android.ui.home.conversationslist.model.ConversationFolder
import com.wire.android.ui.home.conversationslist.model.ConversationType
import com.wire.android.ui.home.conversationslist.model.GeneralConversation
import com.wire.android.ui.home.conversationslist.model.NewActivity


@Composable
fun AllConversationScreen(
    newActivities: List<NewActivity>,
    conversations: Map<ConversationFolder, List<GeneralConversation>>,
    onOpenConversationClick: (String) -> Unit,
    onEditConversationItem: (ConversationType) -> Unit
) {
    AllConversationContent(
        newActivities = newActivities,
        conversations = conversations,
        onOpenConversationClick,
        onEditConversationItem
    )
}

@Composable
private fun AllConversationContent(
    newActivities: List<NewActivity>,
    conversations: Map<ConversationFolder, List<GeneralConversation>>,
    onConversationItemClick: (String) -> Unit,
    onEditConversationItem: (ConversationType) -> Unit,
) {
    LazyColumn {
        folderWithElements(
            header = { stringResource(id = R.string.conversation_label_new_activity) },
            items = newActivities
        ) { newActivity ->
            with(newActivity) {
                ConversationItemFactory(
                    conversation = conversationItem,
                    eventType = eventType,
                    onConversationItemClick = { onConversationItemClick("someId") },
                    onConversationItemLongClick = { onEditConversationItem(conversationItem.conversationType) }
                )
            }
        }

        conversations.forEach { (conversationFolder, conversationList) ->
            folderWithElements(
                header = { conversationFolder.folderName },
                items = conversationList
            ) { generalConversation ->
                GeneralConversationItem(
                    generalConversation = generalConversation,
                    onConversationItemClick = { onConversationItemClick("someId") },
                    onConversationItemLongClick = { onEditConversationItem(generalConversation.conversationType) }
                )
            }
        }
    }
}
