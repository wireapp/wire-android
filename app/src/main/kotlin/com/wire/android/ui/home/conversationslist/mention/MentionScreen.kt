package com.wire.android.ui.home.conversationslist.mention

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.wire.android.R
import com.wire.android.ui.home.conversationslist.common.ConversationItemFactory
import com.wire.android.ui.home.conversationslist.folderWithElements
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import androidx.compose.foundation.lazy.rememberLazyListState

@Composable
fun MentionScreen(
    unreadMentions: List<ConversationItem> = emptyList(),
    allMentions: List<ConversationItem> = emptyList(),
    onMentionItemClick: (ConversationId) -> Unit,
    onEditConversationItem: (ConversationItem) -> Unit,
    onOpenUserProfile: (UserId) -> Unit,
    openConversationNotificationsSettings: (ConversationItem) -> Unit,
    onJoinCall: (ConversationId) -> Unit
) {
    val lazyListState = rememberLazyListState()

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
                openConversation = onMentionItemClick,
                openMenu = onEditConversationItem,
                openUserProfile = onOpenUserProfile,
                openNotificationsOptions = openConversationNotificationsSettings,
                joinCall = onJoinCall,
                searchQuery = ""
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
                joinCall = onJoinCall,
                searchQuery = ""
            )
        }
    }
}


