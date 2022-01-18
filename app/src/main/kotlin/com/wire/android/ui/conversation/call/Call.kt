package com.wire.android.ui.conversation.call

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.conversation.call.model.CallTime
import com.wire.android.ui.conversation.call.model.CallEvent
import com.wire.android.ui.conversation.common.FolderHeader
import com.wire.android.ui.conversation.common.UserLabel
import com.wire.android.ui.conversation.common.WhiteBackgroundWrapper

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
fun MissedCallRowItem(missedCall: CallEvent) {
    WhiteBackgroundWrapper(content = {
        Row {
            with(missedCall) {
                Column {
                    UserLabel(conversationInfo = conversation.conversationInfo)
                    TimeLabel(callTime = missedCall.callTime)
                }
            }
        }
    })
}

@Composable
private fun TimeLabel(callTime: CallTime) {
    Text(text = callTime.toLabel())
}


@Composable
fun CallHistoryRowItem(callHistory: CallEvent) {

}

