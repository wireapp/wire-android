/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 *
 */

package com.wire.android.ui.home.conversationslist.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
        BadgeEventType.Knock -> UnreadKnockBadge(modifier)
        BadgeEventType.None -> {}
    }
}

@Composable
private fun MissedCallBadge(modifier: Modifier = Modifier) {
    NotificationBadgeContainer(
        modifier = modifier
            .width(dimensions().spacing24x)
            .height(dimensions().spacing20x),
        notificationIcon = {
            Image(
                painter = painterResource(id = R.drawable.ic_event_badge_missed_call),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.wireColorScheme.onBadge),
                modifier = Modifier.height(dimensions().spacing18x)
            )
        }
    )
}

@Composable
private fun UnreadMentionBadge(modifier: Modifier = Modifier) {
    NotificationBadgeContainer(
        modifier = modifier
            .width(dimensions().spacing24x)
            .height(dimensions().spacing20x),
        notificationIcon = {
            Image(
                painter = painterResource(id = R.drawable.ic_event_badge_unread_mention),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.wireColorScheme.onBadge),
                modifier = Modifier.height(dimensions().spacing18x)
            )
        }
    )
}

@Composable
private fun UnreadReplyBadge(modifier: Modifier = Modifier) {
    NotificationBadgeContainer(
        modifier = modifier
            .width(dimensions().spacing24x)
            .height(dimensions().spacing20x),
        notificationIcon = {
            Image(
                painter = painterResource(id = R.drawable.ic_event_badge_unread_reply),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.wireColorScheme.onBadge),
                modifier = Modifier.height(dimensions().spacing18x)
            )
        }
    )
}

@Composable
fun UnreadKnockBadge(modifier: Modifier = Modifier) {
    NotificationBadgeContainer(
        modifier = modifier
            .width(dimensions().spacing24x)
            .height(dimensions().spacing20x),
        notificationIcon = {
            Image(
                painter = painterResource(id = R.drawable.ic_event_badge_unread_knock),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.wireColorScheme.onBadge),
                modifier = Modifier.height(dimensions().spacing18x)
            )
        }
    )
}

@Composable
fun ConnectRequestBadge(modifier: Modifier = Modifier) {
    NotificationBadgeContainer(
        modifier = modifier
            .width(dimensions().spacing24x)
            .height(dimensions().spacing20x),
        notificationIcon = {
            Image(
                painter = painterResource(id = R.drawable.ic_event_badge_connect_request),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.wireColorScheme.onBadge),
                modifier = Modifier.height(dimensions().spacing18x)
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
fun UnreadMessageEventBadge(modifier: Modifier = Modifier, unreadMessageCount: Int) {
    if (unreadMessageCount > 0) {
        NotificationBadgeContainer(
            modifier = modifier,
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
