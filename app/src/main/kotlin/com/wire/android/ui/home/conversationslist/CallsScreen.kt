package com.wire.android.ui.home.conversationslist

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.home.conversationslist.common.ConversationItemFactory
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.home.conversationslist.model.EventType
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId

@Composable
fun CallsScreen(
    missedCalls: List<ConversationItem> = emptyList(),
    callHistory: List<ConversationItem> = emptyList(),
    onCallItemClick: (ConversationId) -> Unit,
    onEditConversationItem: (ConversationItem) -> Unit,
    onScrollPositionChanged: (Int) -> Unit = {},
    onOpenUserProfile: (UserId) -> Unit,
    openConversationNotificationsSettings: (ConversationItem) -> Unit,
) {
    val lazyListState = com.wire.android.ui.common.extension.rememberLazyListState { firstVisibleItemIndex ->
        onScrollPositionChanged(firstVisibleItemIndex)
    }

    CallContent(
        lazyListState = lazyListState,
        missedCalls = missedCalls,
        callHistory = callHistory,
        onCallItemClick = onCallItemClick,
        onEditConversationItem = onEditConversationItem,
        onOpenUserProfile = onOpenUserProfile,
        openConversationNotificationsSettings = openConversationNotificationsSettings
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallContent(
    lazyListState: LazyListState,
    missedCalls: List<ConversationItem>,
    callHistory: List<ConversationItem>,
    onCallItemClick: (ConversationId) -> Unit,
    onEditConversationItem: (ConversationItem) -> Unit,
    onOpenUserProfile: (UserId) -> Unit,
    openConversationNotificationsSettings: (ConversationItem) -> Unit,
) {
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize()
    ) {
        folderWithElements(
            header = { stringResource(id = R.string.calls_label_missed_calls) },
            items = missedCalls
        ) { missedCall ->
            ConversationItemFactory(
                conversation = missedCall,
                eventType = EventType.MissedCall,
                openConversation = onCallItemClick,
                openMenu = onEditConversationItem,
                openUserProfile = onOpenUserProfile,
                openNotificationsOptions = openConversationNotificationsSettings,
            )
        }

        folderWithElements(
            header = { stringResource(id = R.string.calls_label_calls_history) },
            items = callHistory
        ) { callHistory ->
            ConversationItemFactory(
                conversation = callHistory,
                openConversation = onCallItemClick,
                openMenu = onEditConversationItem,
                openUserProfile = onOpenUserProfile,
                openNotificationsOptions = openConversationNotificationsSettings,
            )
        }
    }
}
