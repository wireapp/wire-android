package com.wire.android.ui.home.conversationslist

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.home.conversationslist.model.ConversationMissedCall
import com.wire.android.ui.home.conversationslist.model.EventType

@Composable
fun CallsScreen(
    missedCalls: List<ConversationMissedCall> = emptyList(),
    callHistory: List<ConversationMissedCall> = emptyList(),
    onCallItemClick: (ConversationItem) -> Unit,
    onEditConversationItem: (ConversationItem) -> Unit,
    onScrollPositionChanged: (Int) -> Unit = {}
) {
    val lazyListState = com.wire.android.ui.common.extension.rememberLazyListState { firstVisibleItemIndex ->
        onScrollPositionChanged(firstVisibleItemIndex)
    }

    CallContent(
        lazyListState = lazyListState,
        missedCalls = missedCalls,
        callHistory = callHistory,
        onCallItemClick = onCallItemClick,
        onEditConversationItem = onEditConversationItem
    )
}

@Composable
fun CallContent(
    lazyListState: LazyListState,
    missedCalls: List<ConversationMissedCall>,
    callHistory: List<ConversationMissedCall>,
    onCallItemClick: (ConversationItem) -> Unit,
    onEditConversationItem: (ConversationItem) -> Unit
) {
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize()
    ) {
        folderWithElements(
            header = { stringResource(id = R.string.calls_label_missed_calls) },
            items = missedCalls
        ) { missedCall ->
            CallConversationItem(
                conversationMissedCall = missedCall,
                eventType = EventType.MissedCall,
                onCallItemClick = onCallItemClick,
                onCallItemLongClick = onEditConversationItem
            )
        }

        folderWithElements(
            header = { stringResource(id = R.string.calls_label_calls_history) },
            items = callHistory
        ) { callHistory ->
            CallConversationItem(
                conversationMissedCall = callHistory,
                onCallItemClick = onCallItemClick,
                onCallItemLongClick = onEditConversationItem
            )
        }
    }
}

