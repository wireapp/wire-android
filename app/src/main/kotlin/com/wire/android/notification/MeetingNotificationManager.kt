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

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.wire.kalium.logic.data.notification.LocalNotification
import com.wire.kalium.logic.data.user.UserId
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@SingleIn(AppScope::class)
class MeetingNotificationManager @Inject constructor(
    private val context: Context,
    private val notificationManager: NotificationManager,
    private val notificationManagerCompat: NotificationManagerCompat,
    private val builder: MeetingNotificationBuilder,
) {
    fun handleNotifications(notifications: List<LocalNotification.Meeting>, userId: UserId) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) {
            notifications.forEach { notification ->
                notificationManagerCompat.notify(
                    NotificationConstants.getMeetingTag(userId, notification.meetingId.toString(), notification.eventId),
                    NotificationIds.MEETING_NOTIFICATION_ID.ordinal,
                    builder.build(notification, userId)
                )
            }
        }
    }
    fun hideAllNotificationsForUser(userId: UserId) = hideMatching(NotificationConstants.getMeetingsGroupKey(userId))
    fun hideAllNotifications() = hideMatching()
    private fun hideMatching(groupKey: String? = null) {
        notificationManager.activeNotifications.orEmpty()
            .filter { it.id == NotificationIds.MEETING_NOTIFICATION_ID.ordinal }
            .filter { groupKey == null || it.notification.group == groupKey }
            .forEach { notificationManagerCompat.cancel(it.tag, it.id) }
    }
}
