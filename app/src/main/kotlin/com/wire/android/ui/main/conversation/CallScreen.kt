package com.wire.android.ui.main.conversation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.main.conversation.all.model.toUserInfoLabel
import com.wire.android.ui.main.conversation.call.model.Call
import com.wire.android.ui.main.conversation.call.model.CallEvent
import com.wire.android.ui.main.conversation.call.model.CallTime
import com.wire.android.ui.main.conversation.common.FolderHeader
import com.wire.android.ui.main.conversation.common.MissedCallBadge
import com.wire.android.ui.main.conversation.common.RowItem
import com.wire.android.ui.main.conversation.common.UserLabel
import com.wire.android.ui.theme.subLine1

@Composable
fun CallScreen(missedCalls: List<Call> = emptyList(), callHistory: List<Call> = emptyList()) {
    CallContent(missedCalls, callHistory)
}

@Composable
fun CallContent(missedCalls: List<Call>, callHistory: List<Call>) {
    LazyColumn {
        if (missedCalls.isNotEmpty()) {
            item { FolderHeader(name = stringResource(R.string.calls_label_missed_calls)) }
            items(missedCalls) { missedCall ->
                MissedCallRowItem(missedCall)
            }
        }

        if (callHistory.isNotEmpty()) {
            item { FolderHeader(name = stringResource(R.string.calls_label_calls_history)) }
            items(callHistory) { callHistory ->
                CallHistoryRowItem(callHistory)
            }
        }
    }
}

@Composable
fun MissedCallRowItem(missedCall: Call) {
    RowItem {
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
private fun CallHistoryRowItem(callHistory: Call) {
    RowItem {
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
    Text(text = callTime.toLabel(), style = MaterialTheme.typography.subLine1)
}

