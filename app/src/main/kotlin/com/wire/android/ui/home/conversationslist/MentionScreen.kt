package com.wire.android.ui.home.conversationslist

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wire.android.R
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
    onScrollPositionProviderChanged: (() -> Int) -> Unit,
    onOpenUserProfile: (UserId) -> Unit,
    openConversationNotificationsSettings: (ConversationItem) -> Unit,
    onJoinCall: (ConversationId) -> Unit
) {
    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()

    onScrollPositionProviderChanged { lazyListState.firstVisibleItemIndex }

    MentionContent(
        lazyListState = lazyListState,
        unreadMentions = unreadMentions,
        allMentions = allMentions,
        onMentionItemClick = onMentionItemClick,
        onEditConversationItem = onEditConversationItem,
        onOpenUserProfile = onOpenUserProfile,
        openConversationNotificationsSettings = openConversationNotificationsSettings,
        onJoinCall = onJoinCall
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
    openConversationNotificationsSettings: (ConversationItem) -> Unit,
    onJoinCall: (ConversationId) -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize()
    ) {
        folderWithElements(
            header = context.getString(R.string.mention_label_unread_mentions),
            items = unreadMentions.associateBy { it.conversationId.toString() }
        ) { unreadMention ->
            ConversationItemFactory(
                conversation = unreadMention,
                eventType = EventType.UnreadMention,
                openConversation = onMentionItemClick,
                openMenu = onEditConversationItem,
                openUserProfile = onOpenUserProfile,
                openNotificationsOptions = openConversationNotificationsSettings,
                joinCall = onJoinCall
            )
        }

        folderWithElements(
            header = context.getString(R.string.mention_label_all_mentions),
            items = allMentions.associateBy { it.conversationId.toString() }
        ) { mention ->
            ConversationItemFactory(
                conversation = mention,
                openConversation = onMentionItemClick,
                openMenu = onEditConversationItem,
                openUserProfile = onOpenUserProfile,
                openNotificationsOptions = openConversationNotificationsSettings,
                joinCall = onJoinCall
            )
        }
    }
}


