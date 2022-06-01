package com.wire.android.ui.home.conversationslist

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.extension.rememberLazyListState
import com.wire.android.ui.home.conversationslist.common.ConversationItemFactory
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.home.conversationslist.model.EventType
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId

@Composable
fun MentionScreen(
    unreadMentions: List<ConversationItem> = emptyList(),
    allMentions: List<ConversationItem> = emptyList(),
    onMentionItemClick: (ConversationId) -> Unit,
    onEditConversationItem: (ConversationItem) -> Unit,
    onScrollPositionChanged: (Int) -> Unit = {},
    onOpenUserProfile: (UserId) -> Unit,
) {
    val lazyListState = rememberLazyListState { firstVisibleItemIndex ->
        onScrollPositionChanged(firstVisibleItemIndex)
    }

    MentionContent(
        lazyListState = lazyListState,
        unreadMentions = unreadMentions,
        allMentions = allMentions,
        onMentionItemClick = onMentionItemClick,
        onEditConversationItem = onEditConversationItem,
        onOpenUserProfile = onOpenUserProfile,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MentionContent(
    lazyListState: LazyListState,
    unreadMentions: List<ConversationItem>,
    allMentions: List<ConversationItem>,
    onMentionItemClick: (ConversationId) -> Unit,
    onEditConversationItem: (ConversationItem) -> Unit,
    onOpenUserProfile: (UserId) -> Unit,
) {
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize()
    ) {
        folderWithElements(
            header = { stringResource(id = R.string.mention_label_unread_mentions) },
            items = unreadMentions
        ) { unreadMention ->
            ConversationItemFactory(
                conversation = unreadMention,
                eventType = EventType.UnreadMention,
                openConversation = onMentionItemClick,
                openMenu = onEditConversationItem,
                openUserProfile = onOpenUserProfile,
            )
        }

        folderWithElements(
            header = { stringResource(R.string.mention_label_all_mentions) },
            items = allMentions
        ) { mention ->
            ConversationItemFactory(
                conversation = mention,
                openConversation = onMentionItemClick,
                openMenu = onEditConversationItem,
                openUserProfile = onOpenUserProfile,
            )
        }
    }
}


