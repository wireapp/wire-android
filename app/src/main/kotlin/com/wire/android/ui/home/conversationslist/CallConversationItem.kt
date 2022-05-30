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
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.conversationColor
import com.wire.android.ui.home.conversationslist.common.GroupConversationAvatar
import com.wire.android.ui.home.conversationslist.common.ConversationTitle
import com.wire.android.ui.home.conversationslist.common.ConversationUserAvatar
import com.wire.android.ui.home.conversationslist.common.UserLabel
import com.wire.android.ui.home.conversationslist.model.CallEvent
import com.wire.android.ui.home.conversationslist.model.CallTime
import com.wire.android.ui.home.conversationslist.model.ConversationMissedCall
import com.wire.android.ui.home.conversationslist.model.ConversationType
import com.wire.android.ui.home.conversationslist.model.EventType
import com.wire.android.ui.home.conversationslist.model.toUserInfoLabel
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

@Composable
fun CallConversationItem(
    conversationMissedCall: ConversationMissedCall,
    eventType: EventType? = null,
    onCallItemClick: (ConversationMissedCall) -> Unit,
    onCallItemLongClick: (ConversationMissedCall) -> Unit
) {
    with(conversationMissedCall) {
        when (val conversationType = conversationMissedCall.conversationType) {
            is ConversationType.GroupConversation -> {
                RowItemTemplate(
                    leadingIcon = {
                        GroupConversationAvatar(color = colorsScheme().conversationColor(id = conversationType.conversationId))
                    },
                    title = { ConversationTitle(name = conversationType.groupName, isLegalHold = conversationType.isLegalHold) },
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
                    onRowItemClicked = { onCallItemClick(conversationMissedCall) },
                    onRowItemLongClicked = { onCallItemLongClick(conversationMissedCall) }
                )
            }
            is ConversationType.PrivateConversation -> {
                RowItemTemplate(
                    leadingIcon = { with(conversationType.userInfo) { ConversationUserAvatar(avatarAsset, availabilityStatus) } },
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
                    onRowItemClicked = { onCallItemClick(conversationMissedCall) },
                    onRowItemLongClicked = { onCallItemLongClick(conversationMissedCall) }
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
