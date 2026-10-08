/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

package com.wire.android.notification

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import com.wire.android.R
import com.wire.android.util.CurrentTimeProvider
import com.wire.android.util.DateAndTimeParsers
import com.wire.kalium.logic.data.meeting.MeetingReminder
import com.wire.kalium.logic.data.notification.LocalNotification
import com.wire.kalium.logic.data.user.UserId
import dev.zacsweers.metro.Inject
import kotlinx.datetime.Instant

class MeetingNotificationBuilder @Inject constructor(
    private val context: Context,
    private val currentTimeProvider: CurrentTimeProvider,
) {
    fun build(meeting: LocalNotification.Meeting, userId: UserId): Notification = when (meeting) {
            is LocalNotification.Meeting.Invite -> buildInvite(meeting, userId)
            is LocalNotification.Meeting.Update -> buildUpdate(meeting, userId)
            is LocalNotification.Meeting.Cancel -> buildCancel(meeting, userId)
        }

    private fun build(userId: UserId, title: String, smallText: String, bigText: String, time: Instant): Notification =
        NotificationCompat.Builder(context, NotificationConstants.getMeetingsChannelId(userId))
            .setSmallIcon(com.wire.android.feature.notification.R.drawable.notification_icon_small)
            .setGroup(NotificationConstants.getMeetingsGroupKey(userId))
            .setContentTitle(title)
            .setContentText(smallText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setWhen(time.toEpochMilliseconds())
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(meetingsPendingIntent(context, userId.toString()))
            .build()

    private fun buildInvite(invite: LocalNotification.Meeting.Invite, userId: UserId): Notification {
        val currentTime = currentTimeProvider.invoke()
        val isOngoing = invite.startTime <= currentTime && invite.endTime >= currentTime
        val title = invite.meetingTitle
            .ifBlank { context.getString(R.string.notification_meeting_title) }
            .let { if (isOngoing) context.getString(R.string.notification_meeting_title_ongoing, it) else it }
        val date = DateAndTimeParsers.meetingDateShort(invite.startTime)
        val startTime = DateAndTimeParsers.meetingTime(invite.startTime)
        val endTime = DateAndTimeParsers.meetingTime(invite.endTime)
        val smallText = context.getString(R.string.notification_meeting_invite_short)
        val bigText = invite.author?.name?.takeIf { it.isNotBlank() }
            ?.let { name -> context.getString(R.string.notification_meeting_invite, name, date, startTime, endTime) }
            ?: context.getString(R.string.notification_meeting_invite_unknown, date, startTime, endTime)
        return build(userId = userId, title = title, smallText = smallText, bigText = bigText, time = invite.time)
    }

    private fun buildUpdate(update: LocalNotification.Meeting.Update, userId: UserId): Notification {
        val currentTime = currentTimeProvider.invoke()
        val isOngoing = update.startTime <= currentTime && update.endTime >= currentTime
        val title = update.meetingTitle
            .ifBlank { context.getString(R.string.notification_meeting_title) }
            .let { if (isOngoing) context.getString(R.string.notification_meeting_title_ongoing, it) else it }
        val date = DateAndTimeParsers.meetingDateShort(update.startTime)
        val startTime = DateAndTimeParsers.meetingTime(update.startTime)
        val endTime = DateAndTimeParsers.meetingTime(update.endTime)
        val smallText = context.getString(R.string.notification_meeting_update_short)
        val bigText = update.author?.name?.takeIf { it.isNotBlank() }
            ?.let { name -> context.getString(R.string.notification_meeting_update, name, date, startTime, endTime) }
            ?: context.getString(R.string.notification_meeting_update_unknown, date, startTime, endTime)
        return build(userId = userId, title = title, smallText = smallText, bigText = bigText, time = update.time)
    }

    private fun buildCancel(cancel: LocalNotification.Meeting.Cancel, userId: UserId): Notification {
        val title = cancel.meetingTitle.ifBlank { context.getString(R.string.notification_meeting_title) }
        val smallText = context.getString(R.string.notification_meeting_cancel_short)
        val bigText = cancel.author?.name?.takeIf { it.isNotBlank() }
            ?.let { name -> context.getString(R.string.notification_meeting_cancel, name) }
            ?: context.getString(R.string.notification_meeting_cancel_short)
        return build(userId = userId, title = title, smallText = smallText, bigText = bigText, time = cancel.time)
    }

    fun buildReminder(reminder: MeetingReminder, userId: UserId): Notification {
        val title = reminder.title.ifBlank { context.getString(R.string.notification_meeting_title) }
        val text = context.getString(R.string.notification_meeting_starting_at, DateAndTimeParsers.meetingTime(reminder.startTime))
        return NotificationCompat.Builder(context, NotificationConstants.getMeetingsChannelId(userId))
            .setSmallIcon(com.wire.android.feature.notification.R.drawable.notification_icon_small)
            .setGroup(NotificationConstants.getMeetingsGroupKey(userId))
            .setContentTitle(title)
            .setContentText(text)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setWhen(reminder.reminderTime.toEpochMilliseconds())
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(meetingsPendingIntent(context, userId.toString()))
            .build()
    }
}
