/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
 */
@file:Suppress("TooManyFunctions")

package com.wire.android.feature.meetings.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import com.wire.android.feature.meetings.R
import com.wire.android.feature.meetings.model.MeetingItem
import com.wire.android.feature.meetings.model.MeetingItem.BelongingType
import com.wire.android.feature.meetings.model.MeetingItem.RepeatingInterval
import com.wire.android.feature.meetings.model.MeetingItem.Status
import com.wire.android.feature.meetings.ui.mock.endedPrivateChannelMeeting
import com.wire.android.feature.meetings.ui.mock.grouplessOngoingMeeting
import com.wire.android.feature.meetings.ui.mock.ongoingAttendingOneOnOneMeeting
import com.wire.android.feature.meetings.ui.mock.scheduledChannelMeetingStartingSoon
import com.wire.android.feature.meetings.ui.mock.scheduledRepeatingGroupMeeting
import com.wire.android.feature.meetings.ui.util.PreviewMultipleThemes
import com.wire.android.feature.meetings.ui.util.rememberCurrentTimeProvider
import com.wire.android.ui.common.avatar.UserProfileAvatar
import com.wire.android.ui.common.avatar.UserProfileAvatarsRow
import com.wire.android.ui.common.button.WireItemLabel
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.rowitem.RowItemDivider
import com.wire.android.ui.common.rowitem.RowItemTemplate
import com.wire.android.ui.common.typography
import com.wire.android.ui.home.conversationslist.common.ChannelConversationAvatar
import com.wire.android.ui.home.conversationslist.common.RegularGroupConversationAvatar
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.DateAndTimeParsers
import com.wire.kalium.logic.data.id.ConversationId
import kotlinx.coroutines.delay
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import com.wire.android.ui.common.R as UICommonR
import com.wire.android.ui.common.R as commonR

@Composable
fun MeetingItem(
    meeting: MeetingItem,
    modifier: Modifier = Modifier,
    openMeetingOptions: (meetingId: String) -> Unit = {},
) {
    RowItemTemplate(
        modifier = modifier.padding(start = dimensions().spacing8x),
        titleStartPadding = dimensions().spacing0x,
        actionsEndPadding = dimensions().spacing0x,
        contentBottomPadding = dimensions().spacing0x,
        verticalAlignment = Alignment.Top,
        backgroundColor = when (meeting.status) {
            is Status.Ongoing -> colorsScheme().primaryVariant
            else -> colorsScheme().surface
        },
        leadingIcon = { MeetingLeadingIcon() },
        title = {
            Text(
                text = meeting.title,
                style = typography().body02,
                color = colorsScheme().onSurface,
            )
        },
        subtitle = {
            Column {
                MeetingTimeInfoRow(status = meeting.status)
                MeetingBelongingInfoRow(conversationId = meeting.conversationId, type = meeting.belongingType)
                MeetingOngoingAttendingRow(status = meeting.status, onJoinClick = { /* TODO */ })
            }
        },
        actions = {
            MeetingMoreButton {
                openMeetingOptions(meeting.meetingId)
            }
        },
        divider = { MeetingItemDivider() },
    )
}

@Composable
private fun MeetingMoreButton(onMenuClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(dimensions().spacing48x)
            .clip(CircleShape)
            .clickable(onClick = onMenuClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(commonR.drawable.ic_more_vert),
            contentDescription = stringResource(R.string.content_description_more_menu_icon),
        )
    }
}

@Composable
private fun MeetingItemDivider() {
    RowItemDivider(
        height = dimensions().dividerThickness,
        startPadding = dimensions().spacing48x,
        color = colorsScheme().divider
    )
}

@Composable
internal fun VideoCallIcon(tint: Color, modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(id = UICommonR.drawable.ic_video_call),
        contentDescription = stringResource(R.string.content_description_meeting_icon),
        tint = tint,
        modifier = modifier
            .padding(start = dimensions().spacing2x) // visually center the icon as it is not perfectly centered in the asset
            .height(dimensions().spacing14x)
    )
}

@Composable
internal fun MeetingLeadingIcon() {
    val (cornerRadius, borderWidth) = dimensions().groupAvatarCornerRadius to dimensions().avatarBorderWidth
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(dimensions().avatarClickablePadding)
            .size(dimensions().avatarDefaultSize)
            .background(color = colorsScheme().surface, shape = RoundedCornerShape(cornerRadius + borderWidth))
            .border(color = colorsScheme().outline, width = borderWidth, shape = RoundedCornerShape(cornerRadius + borderWidth))
    ) {
        VideoCallIcon(tint = colorsScheme().onSurface)
    }
}

@Composable
private fun MeetingOngoingDurationTimeSublineText(startedTime: Instant) {
    val currentTime = rememberCurrentTimeProvider()
    var currentDuration by remember { mutableStateOf(currentTime().minus(startedTime)) }
    LaunchedEffect(currentDuration) {
        val durationInWholeMinutes = currentDuration.inWholeMinutes.toDuration(DurationUnit.MINUTES)
        val durationToNextFullMinute = durationInWholeMinutes.plus(1.minutes) - currentDuration
        delay(durationToNextFullMinute.inWholeMilliseconds)
        currentDuration = currentTime().minus(startedTime)
    }
    SublineText(text = "%d:%02d".format(currentDuration.inWholeMinutes / 60, currentDuration.inWholeMinutes % 60))
}

@Composable
private fun RepeatingIntervalInfoLabel(repeatingInterval: RepeatingInterval?) {
    repeatingInterval?.let {
        WireItemLabel(text = stringResource(repeatingInterval.nameResId))
    }
}

@Composable
private fun MeetingTimeInfoRow(status: Status) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensions().spacing3x)
    ) {
        when (status) {
            is Status.Scheduled -> {
                SublineText(DateAndTimeParsers.meetingTime(status.startTime) + " - " + DateAndTimeParsers.meetingTime(status.endTime))
                RepeatingIntervalInfoLabel(status.repeatingInterval)
            }

            is Status.Ongoing -> {
                SublineText(text = stringResource(R.string.meeting_started_at, DateAndTimeParsers.meetingTime(status.startTime)))
                SublineText(text = "•")
                MeetingOngoingDurationTimeSublineText(startedTime = status.startTime)
            }

            is Status.Ended -> {
                SublineText(text = DateAndTimeParsers.meetingDate(status.startTime))
                SublineText(text = "•")
                SublineText(text = stringResource(R.string.meeting_started_at, DateAndTimeParsers.meetingTime(status.startTime)))
                SublineText(text = "•")
                SublineText(text = "%d:%02d".format(status.duration.inWholeMinutes / 60, status.duration.inWholeMinutes % 60))
            }
        }
    }
}

@Composable
private fun MeetingBelongingInfoRow(conversationId: ConversationId, type: BelongingType) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensions().spacing4x)
    ) {
        when (type) {
            is BelongingType.Channel -> {
                ChannelConversationAvatar(
                    conversationId = conversationId,
                    isPrivateChannel = type.isPrivateChannel,
                    size = dimensions().spacing18x,
                    borderWidth = dimensions().spacing1x,
                    cornerRadius = dimensions().spacing6x,
                    padding = dimensions().spacing0x,
                )
                SublineText(text = type.name)
            }

            is BelongingType.Group -> {
                RegularGroupConversationAvatar(
                    conversationId = conversationId,
                    size = dimensions().spacing18x,
                    borderWidth = dimensions().spacing1x,
                    borderColor = colorsScheme().outline,
                    cornerRadius = dimensions().spacing6x,
                    padding = dimensions().spacing0x,
                )
                SublineText(text = type.name)
            }

            is BelongingType.OneOnOne -> {
                UserProfileAvatar(
                    avatarData = type.avatar,
                    size = dimensions().spacing18x,
                    avatarBorderWidth = dimensions().spacing1x,
                    avatarBorderColor = colorsScheme().outline,
                    padding = dimensions().spacing0x,
                )
                SublineText(text = type.username)
            }

            is BelongingType.Groupless -> {
                UserProfileAvatarsRow(
                    avatars = type.avatars.take(type.limit),
                    avatarSize = dimensions().spacing18x,
                    overlapSize = dimensions().spacing4x,
                    borderWidth = dimensions().spacing1x,
                    borderColor = colorsScheme().outline,
                )
                if (type.avatars.size > type.limit) {
                    SublineText(text = "+${type.avatars.size - type.limit}")
                }
            }
        }
    }
}

@Composable
private fun JoinMeetingPrimaryButton(onClick: () -> Unit) {
    WirePrimaryButton(
        fillMaxWidth = false,
        minSize = dimensions().buttonMediumMinSize,
        minClickableSize = dimensions().buttonMediumMinSize.plus(DpSize(dimensions().spacing0x, dimensions().spacing16x)),
        contentPadding = PaddingValues(dimensions().spacing8x),
        onClick = onClick,
        leadingIcon = {
            VideoCallIcon(
                tint = colorsScheme().onPrimary,
                modifier = Modifier
                    .padding(end = dimensions().spacing4x)
                    .height(dimensions().spacing12x)
            )
        },
        text = stringResource(R.string.meeting_join_button),
    )
}

@Composable
private fun MeetingAttendingPrimaryBodyText() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensions().spacing4x)
    ) {
        VideoCallIcon(tint = colorsScheme().primary)
        PrimaryBodyText(text = stringResource(R.string.meeting_attending))
    }
}

@Composable
private fun MeetingStartingInPrimaryBodyText(startTime: Instant) {
    val currentTime = rememberCurrentTimeProvider()
    var remainingDuration by remember {
        mutableStateOf(startTime.minus(currentTime()))
    }
    if (remainingDuration.inWholeSeconds > 5.minutes.inWholeSeconds) {
        // when more than 5 minutes remaining, wait until 5 minutes are left to start updating every second
        LaunchedEffect(remainingDuration) {
            delay(remainingDuration.minus(5.minutes))
            remainingDuration = startTime.minus(currentTime())
        }
    } else if (remainingDuration.isPositive()) {
        // when 5 or less minutes remaining, update every second and show countdown
        LaunchedEffect(remainingDuration) {
            val durationInWholeSeconds = remainingDuration.inWholeSeconds.toDuration(DurationUnit.SECONDS)
            val durationToNextFullSecond = remainingDuration - durationInWholeSeconds
            delay(durationToNextFullSecond.inWholeMilliseconds)
            remainingDuration = startTime.minus(currentTime())
        }
        val remainingDurationText = "%d:%02d".format(remainingDuration.inWholeSeconds / 60, remainingDuration.inWholeSeconds % 60)
        PrimaryBodyText(text = stringResource(R.string.meeting_starting_in, remainingDurationText))
    }
}

@Composable
private fun MeetingOngoingAttendingRow(status: Status, onJoinClick: () -> Unit) {
    Box(modifier = Modifier.heightIn(min = dimensions().spacing8x)) {
        if (status is Status.Ongoing) {
            if (status.ongoingCallStatus?.isSelfUserAttending == true) {
                MeetingAttendingPrimaryBodyText()
            } else {
                JoinMeetingPrimaryButton(onClick = onJoinClick)
            }
        } else if (status is Status.Scheduled) {
            MeetingStartingInPrimaryBodyText(startTime = status.startTime)
        }
    }
}

@Composable
private fun SublineText(text: String) {
    Text(
        text = text,
        style = typography().subline01,
        color = colorsScheme().secondaryText,
        modifier = Modifier.padding(vertical = dimensions().spacing4x),
    )
}

@Composable
private fun PrimaryBodyText(text: String) {
    Text(
        text = text,
        style = typography().body03,
        color = colorsScheme().primary,
        modifier = Modifier.padding(vertical = dimensions().spacing8x),
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewEndedPrivateChannelMeeting() = WireTheme {
    MeetingItem(meeting = rememberCurrentTimeProvider().endedPrivateChannelMeeting)
}

@PreviewMultipleThemes
@Composable
fun PreviewOngoingAttendingOneOnOneMeeting() = WireTheme {
    MeetingItem(meeting = rememberCurrentTimeProvider().ongoingAttendingOneOnOneMeeting)
}

@PreviewMultipleThemes
@Composable
fun PreviewGrouplessOngoingMeeting() = WireTheme {
    MeetingItem(meeting = rememberCurrentTimeProvider().grouplessOngoingMeeting)
}

@PreviewMultipleThemes
@Composable
fun PreviewScheduledChannelMeetingStartingSoon() = WireTheme {
    MeetingItem(meeting = rememberCurrentTimeProvider().scheduledChannelMeetingStartingSoon)
}

@PreviewMultipleThemes
@Composable
fun PreviewScheduledRepeatingGroupMeeting() = WireTheme {
    MeetingItem(meeting = rememberCurrentTimeProvider().scheduledRepeatingGroupMeeting)
}
