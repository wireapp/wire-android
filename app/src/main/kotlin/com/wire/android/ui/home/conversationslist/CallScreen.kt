package com.wire.android.ui.home.conversationslist

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.home.conversationslist.model.ConversationMissedCall
import com.wire.android.ui.home.conversationslist.model.ConversationType
import com.wire.android.ui.home.conversationslist.model.EventType
import com.wire.kalium.logic.data.conversation.ConversationId

@Composable
fun CallScreen(
    missedCalls: List<ConversationMissedCall> = emptyList(),
    callHistory: List<ConversationMissedCall> = emptyList(),
    onCallItemClick: (ConversationId) -> Unit,
    onEditConversationItem: (ConversationType) -> Unit,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallContent(
    lazyListState: LazyListState,
    missedCalls: List<ConversationMissedCall>,
    callHistory: List<ConversationMissedCall>,
    onCallItemClick: (ConversationId) -> Unit,
    onEditConversationItem: (ConversationType) -> Unit
) {
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize()
    ) {
        folderWithElementsAndRegularHeader(
            header = { stringResource(id = R.string.calls_label_missed_calls) },
            items = missedCalls
        ) { missedCall ->
            CallConversationItem(
                conversationMissedCall = missedCall,
                eventType = EventType.MissedCall,
                onCallItemClick = { onCallItemClick(missedCall.id) },
                onCallItemLongClick = { onEditConversationItem(missedCall.conversationType) }
            )
        }

        folderWithElementsAndRegularHeader(
            header = { stringResource(id = R.string.calls_label_calls_history) },
            items = callHistory
        ) { callHistory ->
            CallConversationItem(
                conversationMissedCall = callHistory,
                onCallItemClick = { onCallItemClick(callHistory.id) },
                onCallItemLongClick = { onEditConversationItem(callHistory.conversationType) }
            )
        }
    }
}

