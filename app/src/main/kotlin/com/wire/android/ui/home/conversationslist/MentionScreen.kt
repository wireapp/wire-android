package com.wire.android.ui.home.conversationslist

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.extension.rememberLazyListState
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.home.conversationslist.model.ConversationUnreadMention
import com.wire.android.ui.home.conversationslist.model.EventType

@Composable
fun MentionScreen(
    unreadMentions: List<ConversationUnreadMention> = emptyList(),
    allMentions: List<ConversationUnreadMention> = emptyList(),
    onMentionItemClick: (ConversationItem) -> Unit,
    onEditConversationItem: (ConversationItem) -> Unit,
    onScrollPositionChanged: (Int) -> Unit = {}
) {
    val lazyListState = rememberLazyListState { firstVisibleItemIndex ->
        onScrollPositionChanged(firstVisibleItemIndex)
    }

    MentionContent(
        lazyListState = lazyListState,
        unreadMentions = unreadMentions,
        allMentions = allMentions,
        onMentionItemClick = onMentionItemClick,
        onEditConversationItem = onEditConversationItem
    )
}

@Composable
private fun MentionContent(
    lazyListState: LazyListState,
    unreadMentions: List<ConversationUnreadMention>,
    allMentions: List<ConversationUnreadMention>,
    onMentionItemClick: (ConversationItem) -> Unit,
    onEditConversationItem: (ConversationItem) -> Unit
) {
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize()
    ) {
        folderWithElements(
            header = { stringResource(id = R.string.mention_label_unread_mentions) },
            items = unreadMentions
        ) { unreadMention ->
            MentionConversationItem(
                mention = unreadMention,
                eventType = EventType.UnreadMention,
                onMentionItemClick = onMentionItemClick,
                onConversationItemLongClick = onEditConversationItem
            )
        }

        folderWithElements(
            header = { stringResource(R.string.mention_label_all_mentions) },
            items = allMentions
        ) { mention ->
            MentionConversationItem(
                mention = mention,
                onMentionItemClick = onMentionItemClick,
                onConversationItemLongClick = onEditConversationItem
            )
        }
    }
}


