package com.wire.android.ui.home.conversationslist.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.home.conversationslist.model.EventType
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Composable
fun EventBadgeFactory(eventType: EventType, modifier: Modifier = Modifier) {
    when (eventType) {
        EventType.MissedCall -> MissedCallBadge(modifier)
        EventType.UnreadMention -> UnreadMentionBadge(modifier)
        is EventType.UnreadMessage -> UnreadMessageEventBadge(unreadMessageCount = eventType.unreadMessageCount, modifier)
        EventType.UnreadReply -> UnreadReplyBadge(modifier)
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
        })
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
        })
}

@Composable
private fun UnreadMessageEventBadge(unreadMessageCount: Int, modifier: Modifier = Modifier) {
    if (unreadMessageCount > 0) {
        NotificationBadgeContainer(
            notificationIcon = {
                Text(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(
                            start = 8.dp,
                            top = 1.dp,
                            bottom = 1.dp,
                            end = 8.dp
                        ),
                    text = unReadMessageCountStringify(unreadMessageCount),
                    color = MaterialTheme.wireColorScheme.onBadge,
                    style = MaterialTheme.wireTypography.label02,
                )
            })
    }
}

@Composable
private fun NotificationBadgeContainer(notificationIcon: @Composable (() -> Unit), modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.wireColorScheme.badge,
                shape = RoundedCornerShape(MaterialTheme.wireDimensions.notificationBadgeRadius)
            )
            .height(MaterialTheme.wireDimensions.notificationBadgeHeight)
            .wrapContentWidth(),
        contentAlignment = Alignment.Center
    ) { notificationIcon() }
}

private const val MAX_UNREAD_MESSAGE_COUNT = 99

private fun unReadMessageCountStringify(unreadMessageCount: Int) =
    if (unreadMessageCount > MAX_UNREAD_MESSAGE_COUNT) "$MAX_UNREAD_MESSAGE_COUNT+" else unreadMessageCount.toString()
