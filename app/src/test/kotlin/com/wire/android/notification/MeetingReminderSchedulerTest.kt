package com.wire.android.notification

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import androidx.test.core.app.ApplicationProvider
import com.wire.android.framework.TestUser
import com.wire.android.util.CurrentTimeProvider
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.GlobalKaliumScope
import com.wire.kalium.logic.data.call.Call
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.meeting.MeetingReminder
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.feature.call.CallsScope
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.meeting.MeetingRemindersUseCase
import com.wire.kalium.logic.feature.meeting.MeetingScope
import com.wire.kalium.logic.feature.session.DoesValidSessionExistResult
import com.wire.kalium.logic.feature.session.DoesValidSessionExistUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [36])
class MeetingReminderSchedulerTest {
    @Test
    fun givenScheduledMeetingIsRemoved_whenMeetingsChange_thenReplaceOrCancelItsAlarm() = runTest {
        val arrangement = Arrangement().apply { upcomingStarts += listOf(FIRST_START, LATER_START) }
        arrangement.scheduler.restore()
        assertEquals(1, arrangement.scheduledAlarms.size)

        arrangement.upcomingStarts.remove(FIRST_START)
        arrangement.scheduler.observe()
        assertEquals(2, arrangement.scheduledAlarms.size)
        assertEquals(arrangement.scheduledAlarms[0], arrangement.scheduledAlarms[1])

        arrangement.upcomingStarts.clear()
        arrangement.now = Instant.parse("2026-10-07T15:20:30Z")
        arrangement.scheduler.observe()
        verify(exactly = 1) { arrangement.alarmManager.cancel(arrangement.scheduledAlarms.last()) }
    }

    @Test
    fun givenMeetingChanges_whenAlarmIsDue_thenKeepItAndCatchUpAfterDelivery() = runTest {
        val arrangement = Arrangement().apply { upcomingStarts += LATER_START }
        arrangement.scheduler.restore()
        arrangement.upcomingStarts.add(0, FIRST_START)
        arrangement.now = Instant.parse("2026-10-07T14:40:30Z")
        arrangement.scheduler.observe()
        assertEquals(2, arrangement.scheduledAlarms.size)
        assertEquals(arrangement.scheduledAlarms[0], arrangement.scheduledAlarms[1])

        arrangement.upcomingStarts.add(1, SECOND_START)
        val reminder = arrangement.reminder(SECOND_START)
        arrangement.remindersInWindow += reminder
        arrangement.now = Instant.parse("2026-10-07T14:50:30Z")
        arrangement.scheduledAlarms.last().cancel() // The system may release the PendingIntent after dispatching the alarm.
        val restartedScheduler = arrangement.newScheduler()
        restartedScheduler.observe()
        assertEquals(2, arrangement.scheduledAlarms.size)
        verify(exactly = 0) { arrangement.alarmManager.cancel(any<PendingIntent>()) }

        arrangement.now = Instant.parse("2026-10-07T14:51:10Z")
        restartedScheduler.onAlarm(arrangement.user.id, FIRST_START)
        assertEquals(3, arrangement.scheduledAlarms.size)
        coVerify(exactly = 1) { arrangement.reminders.within(FIRST_START, Instant.parse("2026-10-07T15:02:00Z")) }
        verify(exactly = 1) { arrangement.notificationManager.showReminder(reminder, arrangement.user.id) }
    }

    @Test
    fun givenNoEstablishedCall_whenAlarmFires_thenShowReminder() = runTest {
        val arrangement = Arrangement().apply { upcomingStarts += FIRST_START }
        val reminder = arrangement.reminder(FIRST_START)
        arrangement.remindersInWindow += reminder
        arrangement.scheduler.restore()
        arrangement.now = ALARM_TIME
        arrangement.scheduler.onAlarm(arrangement.user.id, FIRST_START)
        verify(exactly = 1) { arrangement.notificationManager.showReminder(reminder, arrangement.user.id) }
    }

    @Test
    fun givenEstablishedCallForMeetingConversation_whenAlarmFires_thenSkipReminder() = runTest {
        val arrangement = Arrangement().apply { upcomingStarts += FIRST_START }
        val reminder = arrangement.reminder(FIRST_START)
        arrangement.remindersInWindow += reminder
        arrangement.establishedCalls += arrangement.call(reminder.conversationId)
        arrangement.scheduler.restore()
        arrangement.now = ALARM_TIME
        arrangement.scheduler.onAlarm(arrangement.user.id, FIRST_START)
        verify(exactly = 0) { arrangement.notificationManager.showReminder(any(), any()) }
    }

    @Test
    fun givenCallInOneConversation_whenMultipleRemindersAreDue_thenShowOnlyOtherConversation() = runTest {
        val arrangement = Arrangement().apply { upcomingStarts += FIRST_START }
        val meetingInCall = arrangement.reminder(FIRST_START, "conversation-in-call")
        val otherMeeting = arrangement.reminder(FIRST_START, "other-conversation")
        arrangement.remindersInWindow += listOf(meetingInCall, otherMeeting)
        arrangement.establishedCalls += arrangement.call(meetingInCall.conversationId)
        arrangement.scheduler.restore()
        arrangement.now = ALARM_TIME
        arrangement.scheduler.onAlarm(arrangement.user.id, FIRST_START)
        verify(exactly = 0) { arrangement.notificationManager.showReminder(meetingInCall, arrangement.user.id) }
        verify(exactly = 1) { arrangement.notificationManager.showReminder(otherMeeting, arrangement.user.id) }
    }

    @Test
    fun givenMeetingWasRemovedBeforeAlarmDelivery_whenAlarmFires_thenShowNothing() = runTest {
        val arrangement = Arrangement().apply { upcomingStarts += FIRST_START }
        arrangement.scheduler.restore()
        arrangement.upcomingStarts.clear()
        arrangement.now = ALARM_TIME
        arrangement.scheduler.onAlarm(arrangement.user.id, FIRST_START)
        verify(exactly = 0) { arrangement.notificationManager.showReminder(any(), any()) }
    }

    @Test
    fun givenStaleAlarmAfterMeetingWasRescheduled_whenAlarmFires_thenShowNothing() = runTest {
        val arrangement = Arrangement().apply { upcomingStarts += FIRST_START }
        arrangement.scheduler.restore()
        arrangement.upcomingStarts.clear()
        arrangement.upcomingStarts += LATER_START
        arrangement.scheduler.observe()
        arrangement.now = ALARM_TIME

        arrangement.scheduler.onAlarm(arrangement.user.id, FIRST_START)

        verify(exactly = 0) { arrangement.notificationManager.showReminder(any(), any()) }
        assertEquals(2, arrangement.scheduledAlarms.size)
    }

    private class Arrangement {
        val user = TestUser.SELF_USER
        var now = Instant.parse("2026-10-07T14:40:00Z")
        val upcomingStarts = mutableListOf<Instant>()
        val remindersInWindow = mutableListOf<MeetingReminder>()
        val establishedCalls = mutableListOf<Call>()
        val scheduledAlarms = mutableListOf<PendingIntent>()
        val reminders = mockk<MeetingRemindersUseCase>()
        val alarmManager = mockk<AlarmManager>(relaxed = true)
        val notificationManager = mockk<MeetingNotificationManager>(relaxUnitFun = true)
        private val meetings = mockk<MeetingScope>()
        private val calls = mockk<CallsScope>()
        private val establishedCall = mockk<ObserveEstablishedCallsUseCase>()
        private val session = mockk<UserSessionScope>()
        private val global = mockk<GlobalKaliumScope>()
        private val validSession = mockk<DoesValidSessionExistUseCase>()
        private val coreLogic = mockk<CoreLogic>()
        private val context = ApplicationProvider.getApplicationContext<Application>()
        val scheduler = newScheduler()

        init {
            every { coreLogic.getGlobalScope() } returns global
            coEvery { global.observeValidAccounts() } returns flowOf(listOf(user to null))
            every { global.doesValidSessionExist } returns validSession
            coEvery { validSession(user.id) } returns DoesValidSessionExistResult.Success(true)
            every { coreLogic.getSessionScope(user.id) } returns session
            every { session.meetings } returns meetings
            every { meetings.meetingReminders } returns reminders
            every { session.calls } returns calls
            every { calls.establishedCall } returns establishedCall
            every { establishedCall() } answers { flowOf(establishedCalls.toList()) }
            coEvery { reminders.next(any()) } answers {
                val from = firstArg<Instant>()
                upcomingStarts.firstOrNull { it > from }?.let(::reminder)
            }
            coEvery { reminders.within(any(), any()) } answers {
                val from = firstArg<Instant>()
                val end = secondArg<Instant>()
                remindersInWindow.filter { it.startTime >= from && it.startTime < end }
            }
            every { reminders.changes() } returns flowOf(Unit)
            every { alarmManager.canScheduleExactAlarms() } returns true
            every { alarmManager.setExactAndAllowWhileIdle(any(), any(), capture(scheduledAlarms)) } returns Unit
        }

        fun newScheduler() = MeetingReminderScheduler(
            context = context,
            coreLogic = coreLogic,
            alarmManager = alarmManager,
            meetingNotificationManager = notificationManager,
            currentTimeProvider = CurrentTimeProvider {
                now
            }
        )

        fun reminder(startTime: Instant, conversation: String = "conversation") = MeetingReminder(
            occurrenceId = "occurrence-$conversation",
            meetingId = QualifiedID("meeting-$conversation", "domain"),
            title = "Meeting",
            startTime = startTime,
            conversationId = QualifiedID(conversation, "domain"),
        )

        fun call(conversationId: QualifiedID) = mockk<Call> {
            every { this@mockk.conversationId } returns conversationId
        }
    }

    private companion object {
        val FIRST_START = Instant.parse("2026-10-07T15:00:00Z")
        val SECOND_START = Instant.parse("2026-10-07T15:00:30Z")
        val LATER_START = Instant.parse("2026-10-07T15:30:00Z")
        val ALARM_TIME = Instant.parse("2026-10-07T14:50:00Z")
    }
}
