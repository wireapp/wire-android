package com.wire.android.ui.home.conversationslist

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.ui.home.conversations.common.ConversationUserAvatar
import com.wire.android.ui.home.conversations.common.GroupConversationAvatar
import com.wire.android.ui.home.conversations.common.GroupName
import com.wire.android.ui.home.conversationslist.model.CallEvent
import com.wire.android.ui.home.conversationslist.model.CallTime
import com.wire.android.ui.home.conversationslist.model.ConversationMissedCall
import com.wire.android.ui.home.conversationslist.model.ConversationType
import com.wire.android.ui.home.conversationslist.model.EventType
import com.wire.android.ui.home.conversationslist.model.toUserInfoLabel
import com.wire.android.ui.main.conversationlist.common.UserLabel
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

@Composable
fun CallConversationItem(
    conversationMissedCall: ConversationMissedCall,
    eventType: EventType? = null,
    onCallItemClick: () -> Unit,
    onCallItemLongClick: () -> Unit
) {
    with(conversationMissedCall) {
        when (val conversationType = conversationMissedCall.conversationType) {
            is ConversationType.GroupConversation -> {
                RowItemTemplate(
                    leadingIcon = {
                        GroupConversationAvatar(colorValue = conversationType.groupColorValue)
                    },
                    title = { GroupName(conversationType.groupName) },
                    subTitle = {
                        with(callInfo) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TimeLabel(callTime = callTime)
                                Spacer(modifier = Modifier.width(6.dp))
                                CallEventIcon(callEvent = callEvent)
                            }
                        }
                    },
                    eventType = eventType,
                    onRowItemClicked = onCallItemClick,
                    onRowItemLongClicked = onCallItemLongClick
                )
            }
            is ConversationType.PrivateConversation -> {
                RowItemTemplate(
                    leadingIcon = {
                        ConversationUserAvatar("")
                    },
                    title = { UserLabel(conversationType.toUserInfoLabel()) },
                    subTitle = {
                        with(callInfo) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TimeLabel(callTime = callTime)
                                Spacer(modifier = Modifier.width(6.dp))
                                CallEventIcon(callEvent = callEvent)
                            }
                        }
                    },
                    eventType = eventType,
                    onRowItemClicked = onCallItemClick,
                    onRowItemLongClicked = onCallItemLongClick,
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
    Text(text = callTime.toLabel(), style = MaterialTheme.wireTypography.subline01, color = MaterialTheme.wireColorScheme.secondaryText)
}
