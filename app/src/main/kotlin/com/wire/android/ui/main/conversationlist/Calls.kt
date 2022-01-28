package com.wire.android.ui.main.conversationlist

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.wire.android.ui.main.conversationlist.common.ConversationItem
import com.wire.android.ui.main.conversationlist.common.GroupConversationAvatar
import com.wire.android.ui.main.conversationlist.common.GroupName
import com.wire.android.ui.main.conversationlist.common.UserLabel
import com.wire.android.ui.main.conversationlist.common.folderWithElements
import com.wire.android.ui.main.conversationlist.model.Call
import com.wire.android.ui.main.conversationlist.model.CallEvent
import com.wire.android.ui.main.conversationlist.model.CallTime
import com.wire.android.ui.main.conversationlist.model.Conversation
import com.wire.android.ui.main.conversationlist.model.EventType
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
            CallItem(missedCall, eventType = EventType.MissedCall)
        }

        folderWithElements(
            header = { stringResource(id = R.string.calls_label_calls_history) },
            items = callHistory
        ) { callHistory ->
            CallItem(call = callHistory)
        }
    }
}

@Composable
fun CallItem(call: Call, eventType: EventType? = null) {
    with(call) {
        when (conversation) {
            is Conversation.GroupConversation -> {
                ConversationItem(
                    avatar = {
                        GroupConversationAvatar(colorValue = conversation.groupColorValue)
                    },
                    title = { GroupName(conversation.groupName) },
                    eventType = eventType
                )
            }
            is Conversation.PrivateConversation -> {
                ConversationItem(
                    avatar = {
                        UserProfileAvatar()
                    },
                    title = { UserLabel(conversation.toUserInfoLabel()) },
                    subTitle = {
                        with(callInfo) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TimeLabel(callTime = callTime)
                                Spacer(modifier = Modifier.width(6.dp))
                                CallEventIcon(callEvent = callEvent)
                            }
                        }
                    },
                    eventType = eventType
                )
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
