/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.notification.LocalNotification
import com.wire.kalium.logic.data.notification.LocalNotificationMessageAuthor
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [36])
class MeetingNotificationManagerTest {
    @Test
    fun givenAccount_whenCreatingChannels_thenCreateMeetingsInAccountGroup() {
        val (arrangement, _) = Arrangement().arrange()
        val user = com.wire.android.framework.TestUser.SELF_USER
        NotificationChannelsManager(arrangement.context, arrangement.compat).createUserNotificationChannels(listOf(user))
        val channel = arrangement.platform.getNotificationChannel(NotificationConstants.getMeetingsChannelId(user.id))
        assertEquals("Meetings", channel.name)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
        assertEquals(NotificationConstants.getChanelGroupIdForUser(user.id), channel.group)
    }

    @Test
    fun givenDeniedNotificationPermission_whenPosting_thenDoNotBuildOrPostInvitation() {
        val (arrangement, manager) = Arrangement()
            .withDeniedNotificationPermission()
            .arrange()
        manager.handleNotifications(listOf(invite), user1)
        verify(exactly = 0) { arrangement.builder.build(any(), any()) }
        assertEquals(0, arrangement.platform.activeNotifications.size)
    }

    @Test
    fun givenRepeatedInvitesForTwoAccounts_whenPostingAndLoggingOut_thenKeepAccountsAndOtherNotificationsSeparate() {
        val (arrangement, manager) = Arrangement()
            .withGrantedNotificationPermission()
            .withBuiltNotifications()
            .withUnrelatedNotification()
            .arrange()
        manager.handleNotifications(listOf(invite), user1)
        assertEquals(2, arrangement.platform.activeNotifications.size)
        manager.handleNotifications(listOf(invite), user1)
        assertEquals(2, arrangement.platform.activeNotifications.size)
        manager.handleNotifications(listOf(invite), user2)
        assertEquals(3, arrangement.platform.activeNotifications.size)
        manager.hideAllNotificationsForUser(user1)
        assertEquals(2, arrangement.platform.activeNotifications.size)
        manager.hideAllNotifications()
        assertEquals("unrelated", arrangement.platform.activeNotifications.single().tag)
    }

    @Test
    fun givenDifferentEventsForOneMeeting_whenPosting_thenKeepEachEventAndUpdateRedelivery() {
        val (arrangement, manager) = Arrangement()
            .withGrantedNotificationPermission()
            .withBuiltNotifications()
            .arrange()
        val firstEvent = invite.copy(eventId = "Aa")
        val secondEvent = invite.copy(eventId = "BB")

        manager.handleNotifications(listOf(firstEvent, secondEvent, firstEvent), user1)

        val notifications = arrangement.platform.activeNotifications
        assertEquals(2, notifications.size)
        assertEquals(setOf(NotificationIds.MEETING_NOTIFICATION_ID.ordinal), notifications.map { it.id }.toSet())
        assertEquals(
            setOf(firstEvent, secondEvent).map {
                NotificationConstants.getMeetingTag(user1, it.meetingId.toString(), it.eventId)
            }.toSet(),
            notifications.map { it.tag }.toSet()
        )
        manager.hideAllNotificationsForUser(user1)
        assertEquals(0, arrangement.platform.activeNotifications.size)
    }

    private class Arrangement {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val platform = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val compat = NotificationManagerCompat.from(context)
        val builder = mockk<MeetingNotificationBuilder>()
        fun withGrantedNotificationPermission() = apply {
            org.robolectric.Shadows.shadowOf(context).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        fun withDeniedNotificationPermission() = apply {
            org.robolectric.Shadows.shadowOf(context).denyPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        fun withBuiltNotifications() = apply {
            every { builder.build(any(), any()) } answers {
                NotificationCompat.Builder(context, NotificationConstants.getMeetingsChannelId(secondArg()))
                    .setGroup(NotificationConstants.getMeetingsGroupKey(secondArg()))
                    .build()
            }
        }
        fun withUnrelatedNotification() = apply {
            compat.notify("unrelated", 1, Notification())
        }
        fun arrange() = this to MeetingNotificationManager(context, platform, compat, builder)
    }

    private val user1 = QualifiedID("first", "example.com")
    private val user2 = QualifiedID("second", "example.com")
    private val invite = LocalNotification.Meeting.Invite(
        "event",
        QualifiedID("meeting", "example.com"),
        QualifiedID("conversation", "example.com"),
        "Planning",
        LocalNotificationMessageAuthor("Alice", null),
        Instant.fromEpochMilliseconds(1234),
        Instant.parse("2022-08-03T09:00:00Z"),
        Instant.parse("2022-08-04T04:45:00Z"),
    )
}
