package com.wire.android.ui.home.conversationslist

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.extension.rememberLazyListState
import com.wire.android.ui.home.conversations.common.ConversationItemFactory
import com.wire.android.ui.home.conversationslist.model.ConversationFolder
import com.wire.android.ui.home.conversationslist.model.ConversationType
import com.wire.android.ui.home.conversationslist.model.GeneralConversation
import com.wire.android.ui.home.conversationslist.model.NewActivity
import com.wire.kalium.logic.data.conversation.ConversationId

@Composable
fun AllConversationScreen(
    newActivities: List<NewActivity>,
    conversations: Map<ConversationFolder, List<GeneralConversation>>,
    onOpenConversationClick: (ConversationId) -> Unit,
    onEditConversationItem: (ConversationType) -> Unit,
    onScrollPositionChanged: (Int) -> Unit = {}
) {
    val lazyListState = rememberLazyListState { firstVisibleItemIndex ->
        onScrollPositionChanged(firstVisibleItemIndex)
    }

    AllConversationContent(
        lazyListState = lazyListState,
        newActivities = newActivities,
        conversations = conversations,
        onConversationItemClick = onOpenConversationClick,
        onEditConversationItem = onEditConversationItem
    )
}

@Composable
private fun AllConversationContent(
    lazyListState: LazyListState,
    newActivities: List<NewActivity>,
    conversations: Map<ConversationFolder, List<GeneralConversation>>,
    onConversationItemClick: (ConversationId) -> Unit,
    onEditConversationItem: (ConversationType) -> Unit,
) {
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = dimensions().topBarSearchFieldHeight,
        )
    ) {
        folderWithElements(
            header = { stringResource(id = R.string.conversation_label_new_activity) },
            items = newActivities
        ) { newActivity ->
            with(newActivity) {
                ConversationItemFactory(
                    conversation = conversationItem,
                    eventType = eventType,
                    onConversationItemClick = { onConversationItemClick(conversationItem.id) },
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
                    onConversationItemClick = { onConversationItemClick(generalConversation.id) },
                    onConversationItemLongClick = { onEditConversationItem(generalConversation.conversationType) }
                )
            }
        }
    }
}
