package com.wire.android.ui.conversation.call

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
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.SurfaceBackgroundWrapper
import com.wire.android.ui.conversation.all.model.toUserInfoLabel
import com.wire.android.ui.conversation.call.model.Call
import com.wire.android.ui.conversation.call.model.CallEvent
import com.wire.android.ui.conversation.call.model.CallTime
import com.wire.android.ui.conversation.common.FolderHeader
import com.wire.android.ui.conversation.common.MissedCallBadge
import com.wire.android.ui.conversation.common.UserInfoLabel
import com.wire.android.ui.conversation.common.UserLabel
import com.wire.android.ui.theme.Dimensions
import com.wire.android.ui.theme.subLine1

@Preview
@Composable
fun Call(viewModel: CallViewModel = CallViewModel()) {
    val uiState by viewModel.state.collectAsState()

    CallsScreen(uiState = uiState)
}

@Composable
private fun CallsScreen(uiState: CallState) {
    Scaffold(
        content = { CallContent(uiState) }
    )
}

@Composable
fun CallContent(uiState: CallState) {
    with(uiState) {
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
}

@Composable
fun MissedCallRowItem(missedCall: Call) {
    SurfaceBackgroundWrapper(
        content = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(Dimensions.CONVERSATION_ITEM_ROW_PADDING)
            ) {
                CallLabel(missedCall)
                Box(modifier = Modifier.fillMaxWidth()) {
                    MissedCallBadge(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 8.dp)
                    )
                }
            }
        }, modifier = Modifier.padding(0.5.dp)
    )
}

@Composable
private fun CallHistoryRowItem(callHistory: Call) {
    SurfaceBackgroundWrapper(
        content = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(8.dp)
            ) {
                CallLabel(callHistory)
            }
        }, modifier = Modifier.padding(0.5.dp)
    )
}

@Composable
private fun CallLabel(call: Call) {
    Column {
        UserLabel(call.conversation.toUserInfoLabel())
        with(call.callInfo) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TimeLabel(callTime = callTime)
                Spacer(modifier = Modifier.width(6.dp))
                CallEventIcon(callEvent = callEvent)
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

