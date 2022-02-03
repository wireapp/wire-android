package com.wire.android.ui.home.conversationslist

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.home.conversationslist.model.ConversationMissedCall
import com.wire.android.ui.home.conversationslist.model.EventType

@Composable
fun CallScreen(
    missedCalls: List<ConversationMissedCall> = emptyList(),
    callHistory: List<ConversationMissedCall> = emptyList(),
    onCallItemClick: (String) -> Unit
) {
    CallContent(
        missedCalls = missedCalls,
        callHistory = callHistory,
        onCallItemClick = onCallItemClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallContent(
    missedCalls: List<ConversationMissedCall>,
    callHistory: List<ConversationMissedCall>,
    onCallItemClick: (String) -> Unit
) {
    LazyColumn {
        folderWithElements(
            header = { stringResource(id = R.string.calls_label_missed_calls) },
            items = missedCalls
        ) { missedCall ->
            CallConversationItem(
                conversationMissedCall = missedCall,
                eventType = EventType.MissedCall,
                onCallItemClick = { onCallItemClick("someId") }
            )
        }

        folderWithElements(
            header = { stringResource(id = R.string.calls_label_calls_history) },
            items = callHistory
        ) { callHistory ->
            CallConversationItem(
                conversationMissedCall = callHistory,
                onCallItemClick = { onCallItemClick("someId") }
            )
        }
    }
}

