package com.wire.android.ui.home.conversationslist.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.BlockedLabel
import com.wire.android.ui.common.DeletedLabel
import com.wire.android.ui.common.button.WireItemLabel
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversationslist.model.BadgeEventType
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Composable
fun EventBadgeFactory(eventType: BadgeEventType, modifier: Modifier = Modifier) {
    when (eventType) {
        BadgeEventType.MissedCall -> MissedCallBadge(modifier)
        BadgeEventType.UnreadMention -> UnreadMentionBadge(modifier)
        is BadgeEventType.UnreadMessage -> UnreadMessageEventBadge(unreadMessageCount = eventType.unreadMessageCount)
        BadgeEventType.UnreadReply -> UnreadReplyBadge(modifier)
        BadgeEventType.ReceivedConnectionRequest -> ConnectRequestBadge(modifier)
        BadgeEventType.SentConnectRequest -> ConnectPendingRequestBadge(modifier)
        BadgeEventType.Blocked -> BlockedLabel(modifier)
        BadgeEventType.Deleted -> DeletedLabel(modifier)
        // TODO BadgeEventType.Knock -> KnockBadge(modifier)
        BadgeEventType.None -> {}
    }
}

@Composable
private fun MissedCallBadge(modifier: Modifier = Modifier) {
    NotificationBadgeContainer(
        notificationIcon = {
            Image(
                painter = painterResource(id = R.drawable.ic_event_badge_missed_call),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.wireColorScheme.onBadge),
                modifier = modifier
            )
        }
    )
}

@Composable
private fun UnreadMentionBadge(modifier: Modifier = Modifier) {
    NotificationBadgeContainer(
        notificationIcon = {
            Image(
                painter = painterResource(id = R.drawable.ic_event_badge_unread_mention),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.wireColorScheme.onBadge),
                modifier = modifier
            )
        }
    )
}

@Composable
private fun UnreadReplyBadge(modifier: Modifier = Modifier) {
    NotificationBadgeContainer(
        notificationIcon = {
            Image(
                painter = painterResource(id = R.drawable.ic_event_badge_unread_reply),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.wireColorScheme.onBadge),
                modifier = modifier
            )
        }
    )
}

@Composable
fun ConnectRequestBadge(modifier: Modifier = Modifier) {
    NotificationBadgeContainer(
        notificationIcon = {
            Image(
                painter = painterResource(id = R.drawable.ic_event_badge_connect_request),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.wireColorScheme.onBadge),
                modifier = modifier
            )
        }
    )
}

@Composable
fun ConnectPendingRequestBadge(modifier: Modifier = Modifier) {
    WireItemLabel(
        text = stringResource(id = R.string.connection_pending_label),
        modifier = modifier
    )
}

@Composable
private fun UnreadMessageEventBadge(unreadMessageCount: Int) {
    if (unreadMessageCount > 0) {
        NotificationBadgeContainer(
            notificationIcon = {
                Text(
                    modifier = Modifier
                        .padding(
                            horizontal = dimensions().spacing8x,
                            vertical = dimensions().spacing2x
                        ),
                    text = unReadMessageCountStringify(unreadMessageCount),
                    color = MaterialTheme.wireColorScheme.onBadge,
                    style = MaterialTheme.wireTypography.label02,
                )
            }
        )
    }
}

@Composable
private fun NotificationBadgeContainer(notificationIcon: @Composable () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.wireColorScheme.badge,
                shape = RoundedCornerShape(MaterialTheme.wireDimensions.notificationBadgeRadius)
            )
            .wrapContentSize(Alignment.Center)
    ) { notificationIcon() }
}

private const val MAX_UNREAD_MESSAGE_COUNT = 99

private fun unReadMessageCountStringify(unreadMessageCount: Int) =
    if (unreadMessageCount > MAX_UNREAD_MESSAGE_COUNT) "$MAX_UNREAD_MESSAGE_COUNT+" else unreadMessageCount.toString()
