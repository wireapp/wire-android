package com.wire.android.ui.home.conversationslist

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversationslist.model.ConversationUnreadMention
import com.wire.android.ui.home.conversationslist.model.EventType

@Composable
fun MentionScreen(
    unreadMentions: List<ConversationUnreadMention> = emptyList(),
    allMentions: List<ConversationUnreadMention> = emptyList(),
    onMentionItemClick: (String) -> Unit,
    onScrollPositionChanged: (Int) -> Unit = {}
) {
    val lazyListState = rememberLazyListState()
    onScrollPositionChanged(lazyListState.firstVisibleItemIndex)

    MentionContent(
        lazyListState = lazyListState,
        unreadMentions = unreadMentions,
        allMentions = allMentions,
        onMentionItemClick = onMentionItemClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MentionContent(
    unreadMentions: List<ConversationUnreadMention>,
    allMentions: List<ConversationUnreadMention>,
    onMentionItemClick: (String) -> Unit,
    lazyListState: LazyListState
) {
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = dimensions().topBarSearchFieldHeight)
    ) {
        folderWithElements(
            header = { stringResource(id = R.string.mention_label_unread_mentions) },
            items = unreadMentions
        ) { unreadMention ->
            MentionConversationItem(
                mention = unreadMention,
                eventType = EventType.UnreadMention,
                onMentionItemClick = { onMentionItemClick("someId") }
            )
        }

        folderWithElements(
            header = { stringResource(R.string.mention_label_all_mentions) },
            items = allMentions
        ) { mention ->
            MentionConversationItem(
                mention = mention,
                onMentionItemClick = { onMentionItemClick("someId") }
            )
        }
    }
}


