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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import com.wire.android.feature.meetings.R
import com.wire.android.feature.meetings.model.MeetingItem
import com.wire.android.feature.meetings.model.MeetingItem.BelongingType
import com.wire.android.feature.meetings.model.MeetingItem.Status
import com.wire.android.feature.meetings.ui.mock.endedPrivateChannelMeeting
import com.wire.android.feature.meetings.ui.mock.grouplessOngoingMeeting
import com.wire.android.feature.meetings.ui.mock.ongoingAttendingOneOnOneMeeting
import com.wire.android.feature.meetings.ui.mock.scheduledChannelMeetingStartingSoon
import com.wire.android.feature.meetings.ui.mock.scheduledRepeatingGroupMeeting
import com.wire.android.feature.meetings.ui.util.PreviewMultipleThemes
import com.wire.android.feature.meetings.ui.util.audioPermissionCheckFlow
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
import com.wire.android.util.rememberCurrentTimeProvider
import com.wire.kalium.logic.data.id.ConversationId
import kotlinx.coroutines.delay
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaZoneId
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import com.wire.android.ui.common.R as commonR

@Composable
fun MeetingItem(
    meeting: MeetingItem,
    modifier: Modifier = Modifier,
    displayTimeZone: TimeZone = TimeZone.currentSystemDefault(),
    openMeetingOptions: (occurrenceId: String) -> Unit = {},
    startCall: (conversationId: ConversationId) -> Unit = {},
    joinCall: (conversationId: ConversationId) -> Unit = {},
    returnToCall: (conversationId: ConversationId) -> Unit = {},
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
                MeetingTimeInfoRow(
                    status = meeting.status,
                    displayTimeZone = displayTimeZone,
                    repeatingInterval = meeting.repeatingInterval
                )
                MeetingBelongingInfoRow(
                    conversationId = meeting.conversationId,
                    type = meeting.belongingType
                )
                MeetingOngoingAttendingRow(
                    conversationId = meeting.conversationId,
                    status = meeting.status,
                    startCall = startCall,
                    joinCall = joinCall,
                    returnToCall = returnToCall,
                )
            }
        },
        actions = {
            MeetingMoreButton {
                openMeetingOptions(meeting.occurrenceId)
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
        painter = painterResource(id = commonR.drawable.ic_video_call),
        contentDescription = null,
        tint = tint,
        modifier = modifier
            .padding(start = dimensions().spacing2x) // visually center the icon as it is not perfectly centered in the asset
            .height(dimensions().spacing14x)
    )
}

@Composable
internal fun CalendarIcon(tint: Color, modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(id = commonR.drawable.ic_calendar),
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(dimensions().spacing16x)
    )
}

@Composable
internal fun MeetingLeadingIcon(
    padding: PaddingValues = PaddingValues(all = dimensions().avatarClickablePadding)
) {
    val (cornerRadius, borderWidth) = dimensions().groupAvatarCornerRadius to dimensions().avatarBorderWidth
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(padding)
            .size(dimensions().avatarDefaultSize)
            .background(color = colorsScheme().surface, shape = RoundedCornerShape(cornerRadius + borderWidth))
            .border(color = colorsScheme().outline, width = borderWidth, shape = RoundedCornerShape(cornerRadius + borderWidth))
    ) {
        CalendarIcon(tint = colorsScheme().onSurface)
    }
}

@Composable
private fun RepeatingIntervalInfoLabel(repeatingInterval: MeetingItem.RepeatingInterval?) {
    if (repeatingInterval != null) {
        WireItemLabel(text = repeatingInterval.label.asString(), textStyle = typography().label01)
    }
}

@Composable
private fun MeetingTimeInfoRow(status: Status, repeatingInterval: MeetingItem.RepeatingInterval?, displayTimeZone: TimeZone) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensions().spacing3x)
    ) {
        val zoneId = displayTimeZone.toJavaZoneId()
        SublineText(
            DateAndTimeParsers.meetingTime(instant = status.startTime, zoneId = zoneId) + " - " +
                    DateAndTimeParsers.meetingTime(instant = status.endTime, zoneId = zoneId)
        )
        RepeatingIntervalInfoLabel(repeatingInterval)
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
    val audioPermissionCheck = audioPermissionCheckFlow(onPermissionGranted = onClick)
    WirePrimaryButton(
        fillMaxWidth = false,
        minSize = dimensions().buttonMediumMinSize,
        minClickableSize = dimensions().buttonMediumMinSize.plus(DpSize(dimensions().spacing0x, dimensions().spacing16x)),
        contentPadding = PaddingValues(dimensions().spacing8x),
        onClick = audioPermissionCheck::launch,
        leadingIcon = {
            VideoCallIcon(
                tint = colorsScheme().onPrimary,
                modifier = Modifier
                    .padding(end = dimensions().spacing4x)
                    .height(dimensions().spacing12x)
            )
        },
        text = stringResource(R.string.meeting_join_button),
        onClickDescription = stringResource(R.string.content_description_join_meeting_label),
    )
}

@Composable
private fun MeetingAttendingPrimaryBodyText(onClick: () -> Unit, startNegativePadding: Dp = dimensions().spacing4x) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensions().spacing4x),
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.placeRelative(-startNegativePadding.roundToPx(), 0)
                }
            }
            .clip(RoundedCornerShape(dimensions().buttonCornerSize))
            .clickable(
                onClick = onClick,
                onClickLabel = stringResource(R.string.content_description_attending_meeting_label),
                role = Role.Button,
            )
            .padding(horizontal = startNegativePadding)
    ) {
        VideoCallIcon(tint = colorsScheme().primary)
        PrimaryBodyText(text = stringResource(R.string.meeting_attending))
    }
}

@Composable
private fun MeetingStartingInPrimaryBodyText(status: Status.Scheduled) {
    val currentTime = rememberCurrentTimeProvider()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val remainingDuration by produceState(status.remainingDuration, status, currentTime, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // A new scheduled status or foregrounding cancels the old delay and recalculates from wall-clock time.
            value = status.startTime - currentTime()
            while (value.isPositive()) {
                val untilNextUpdate = if (value > 5.minutes) {
                    value - 5.minutes
                } else {
                    value - value.inWholeSeconds.toDuration(DurationUnit.SECONDS)
                }
                delay(untilNextUpdate.inWholeMilliseconds.coerceAtLeast(1))
                value = status.startTime - currentTime()
            }
        }
    }
    if (remainingDuration.isPositive() && remainingDuration <= 5.minutes) {
        val remainingDurationText = "%d:%02d".format(remainingDuration.inWholeSeconds / 60, remainingDuration.inWholeSeconds % 60)
        PrimaryBodyText(text = stringResource(R.string.meeting_starting_in, remainingDurationText))
    }
}

@Composable
private fun MeetingOngoingAttendingRow(
    conversationId: ConversationId,
    status: Status,
    joinCall: (conversationId: ConversationId) -> Unit,
    startCall: (conversationId: ConversationId) -> Unit,
    returnToCall: (conversationId: ConversationId) -> Unit,
) {
    Box(modifier = Modifier.heightIn(min = dimensions().spacing8x)) {
        if (status is Status.Ongoing) {
            when {
                status.ongoingCallStatus?.isSelfUserAttending == true -> {
                    // user is attending the ongoing call, show "Attending" text with option to return to the call
                    MeetingAttendingPrimaryBodyText(onClick = { returnToCall(conversationId) })
                }

                status.ongoingCallStatus != null -> {
                    // there is an ongoing call but the user is not attending, show option to join the call
                    JoinMeetingPrimaryButton(onClick = { joinCall(conversationId) })
                }

                else -> {
                    // there is no ongoing call, show option to start the call
                    JoinMeetingPrimaryButton(onClick = { startCall(conversationId) })
                }
            }
        } else if (status is Status.Scheduled) {
            MeetingStartingInPrimaryBodyText(status = status)
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
