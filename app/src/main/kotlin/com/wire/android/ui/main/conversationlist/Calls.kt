package com.wire.android.ui.main.conversationlist

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.main.conversationlist.common.MissedCallBadge
import com.wire.android.ui.main.conversationlist.common.RowItem
import com.wire.android.ui.main.conversationlist.common.UserLabel
import com.wire.android.ui.main.conversationlist.common.folderWithElements
import com.wire.android.ui.main.conversationlist.model.Call
import com.wire.android.ui.main.conversationlist.model.CallEvent
import com.wire.android.ui.main.conversationlist.model.CallTime
import com.wire.android.ui.main.conversationlist.model.toUserInfoLabel
import com.wire.android.ui.theme.subline01
import com.wire.android.ui.theme.wireColorScheme

@Composable
fun CallScreen(
    missedCalls: List<Call> = emptyList(),
    callHistory: List<Call> = emptyList(),
    onCallItemClick: () -> Unit
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
    missedCalls: List<Call>,
    callHistory: List<Call>,
    onCallItemClick: () -> Unit
) {
    LazyColumn {
        folderWithElements(
            header = { stringResource(id = R.string.calls_label_missed_calls) },
            items = missedCalls
        ) { missedCall ->
            MissedCallRowItem(
                missedCall = missedCall,
                onCallItemClick = onCallItemClick
            )
        }

        folderWithElements(
            header = { stringResource(id = R.string.calls_label_calls_history) },
            items = callHistory
        ) { callHistory ->
            CallHistoryRowItem(
                callHistory = callHistory,
                onCallItemClick = onCallItemClick
            )
        }
    }
}

@Composable
fun MissedCallRowItem(
    missedCall: Call,
    onCallItemClick: () -> Unit
) {
    RowItem(onRowItemClick = onCallItemClick) {
        CallLabel(missedCall)
        Box(modifier = Modifier.fillMaxWidth()) {
            MissedCallBadge(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
            )
        }
    }
}

@Composable
private fun CallHistoryRowItem(callHistory: Call, onCallItemClick: () -> Unit) {
    RowItem(onRowItemClick = onCallItemClick) {
        CallLabel(callHistory)
    }
}

@Composable
private fun CallLabel(call: Call) {
    with(call) {
        UserProfileAvatar(avatarUrl = conversation.userInfo.avatarUrl, onClick = {})
        Column {
            UserLabel(conversation.toUserInfoLabel())
            with(callInfo) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TimeLabel(callTime = callTime)
                    Spacer(modifier = Modifier.width(6.dp))
                    CallEventIcon(callEvent = callEvent)
                }
            }
        }
    }
}

@Composable
private fun CallEventIcon(callEvent: CallEvent, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = callEvent.drawableResourceId),
        contentDescription = null,
        modifier = modifier
    )
}

@Composable
private fun TimeLabel(callTime: CallTime) {
    Text(text = callTime.toLabel(), style = MaterialTheme.typography.subline01, color = MaterialTheme.wireColorScheme.secondaryText)
}

