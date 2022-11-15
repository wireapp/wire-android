package com.wire.android.ui.home.conversationslist.call

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.wire.android.R
import com.wire.android.ui.home.conversationslist.common.ConversationItemFactory
import com.wire.android.ui.home.conversationslist.folderWithElements
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId

@Composable
fun CallsScreen(
    missedCalls: List<ConversationItem> = emptyList(),
    callHistory: List<ConversationItem> = emptyList(),
    onCallItemClick: (ConversationId) -> Unit,
    onEditConversationItem: (ConversationItem) -> Unit,
    onOpenUserProfile: (UserId) -> Unit,
    openConversationNotificationsSettings: (ConversationItem) -> Unit,
    onJoinCall: (ConversationId) -> Unit
) {
    val lazyListState = rememberLazyListState()

    CallContent(
        lazyListState = lazyListState,
        missedCalls = missedCalls,
        callHistory = callHistory,
        onCallItemClick = onCallItemClick,
        onEditConversationItem = onEditConversationItem,
        onOpenUserProfile = onOpenUserProfile,
        openConversationNotificationsSettings = openConversationNotificationsSettings,
        onJoinCall = onJoinCall
    )
}

@Composable
fun CallContent(
    lazyListState: LazyListState,
    missedCalls: List<ConversationItem>,
    callHistory: List<ConversationItem>,
    onCallItemClick: (ConversationId) -> Unit,
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
            header = context.getString(R.string.calls_label_missed_calls),
            items = missedCalls.associateBy { it.conversationId.toString() }
        ) { missedCall ->
            ConversationItemFactory(
                conversation = missedCall,
                openConversation = onCallItemClick,
                openMenu = onEditConversationItem,
                openUserProfile = onOpenUserProfile,
                openNotificationsOptions = openConversationNotificationsSettings,
                joinCall = onJoinCall,
                searchQuery = ""
            )
        }

        folderWithElements(
            header = context.getString(R.string.calls_label_calls_history),
            items = callHistory.associateBy { it.conversationId.toString() }
        ) { callHistory ->
            ConversationItemFactory(
                conversation = callHistory,
                openConversation = onCallItemClick,
                openMenu = onEditConversationItem,
                openUserProfile = onOpenUserProfile,
                openNotificationsOptions = openConversationNotificationsSettings,
                joinCall = onJoinCall,
                searchQuery = " "
            )
        }
    }
}
