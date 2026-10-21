package com.wire.android.notification

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import androidx.test.core.app.ApplicationProvider
import com.wire.android.framework.TestUser
import com.wire.android.session.FakeSessionAdmission
import com.wire.android.util.CurrentTimeProvider
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.GlobalKaliumScope
import com.wire.kalium.logic.data.call.Call
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.meeting.MeetingReminder
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.feature.call.CallsScope
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.meeting.MeetingRemindersUseCase
import com.wire.kalium.logic.feature.meeting.MeetingScope
import com.wire.kalium.logic.feature.session.DoesValidSessionExistResult
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.logic.feature.session.DoesValidSessionExistUseCase
import com.wire.kalium.logic.feature.user.ObserveIsMeetingsEnabledUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [36])
@OptIn(ExperimentalCoroutinesApi::class)
class MeetingReminderSchedulerTest {
    @Test
    fun givenMeetingsDisabled_whenObserving_thenDoNotScheduleAlarm() = runTest {
        val arrangement = Arrangement().apply {
            upcomingStarts += FIRST_START
            meetingsEnabled = flowOf(false)
        }

        backgroundScope.launch { arrangement.scheduler.observe() }
        runCurrent()

        assertEquals(0, arrangement.scheduledAlarms.size)
        coVerify(exactly = 0) { arrangement.reminders.next(any()) }
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun givenScheduledAlarm_whenMeetingsDisabledAndEnabledAgain_thenCancelAndResumeScheduling() = runTest {
        val enabled = MutableStateFlow(true)
        val changes = MutableStateFlow(0)
        val arrangement = Arrangement().apply {
            upcomingStarts += FIRST_START
            meetingsEnabled = enabled
            reminderChanges = changes.map {}
        }
        backgroundScope.launch { arrangement.scheduler.observe() }
        runCurrent()
        assertEquals(1, arrangement.scheduledAlarms.size)

        enabled.value = false
        runCurrent()
        verify(exactly = 1) { arrangement.alarmManager.cancel(arrangement.scheduledAlarms.single()) }

        arrangement.upcomingStarts.clear()
        arrangement.upcomingStarts += LATER_START
        changes.value++
        runCurrent()
        assertEquals(1, arrangement.scheduledAlarms.size)
        coVerify(exactly = 1) { arrangement.reminders.next(any()) }

        enabled.value = true
        runCurrent()
        assertEquals(2, arrangement.scheduledAlarms.size)
        coVerify(exactly = 2) { arrangement.reminders.next(any()) }
    }

    @Test
    fun givenScheduledMeetingIsRemoved_whenMeetingsChange_thenReplaceOrCancelItsAlarm() = runTest {
        val arrangement = Arrangement().apply { upcomingStarts += listOf(FIRST_START, LATER_START) }
        arrangement.scheduler.restore()
        assertEquals(1, arrangement.scheduledAlarms.size)

        arrangement.upcomingStarts.remove(FIRST_START)
        backgroundScope.launch { arrangement.scheduler.observe() }
        runCurrent()
        assertEquals(2, arrangement.scheduledAlarms.size)
        assertEquals(arrangement.scheduledAlarms[0], arrangement.scheduledAlarms[1])

        arrangement.upcomingStarts.clear()
        arrangement.now = Instant.parse("2026-10-07T15:20:30Z")
        backgroundScope.launch { arrangement.scheduler.observe() }
        runCurrent()
        verify(exactly = 1) { arrangement.alarmManager.cancel(arrangement.scheduledAlarms.last()) }
    }

    @Test
    fun givenMeetingChanges_whenAlarmIsDue_thenKeepItAndCatchUpAfterDelivery() = runTest {
        val arrangement = Arrangement().apply { upcomingStarts += LATER_START }
        arrangement.scheduler.restore()
        arrangement.upcomingStarts.add(0, FIRST_START)
        arrangement.now = Instant.parse("2026-10-07T14:40:30Z")
        backgroundScope.launch { arrangement.scheduler.observe() }
        runCurrent()
        assertEquals(2, arrangement.scheduledAlarms.size)
        assertEquals(arrangement.scheduledAlarms[0], arrangement.scheduledAlarms[1])

        arrangement.upcomingStarts.add(1, SECOND_START)
        val reminder = arrangement.reminder(SECOND_START)
        arrangement.remindersInWindow += reminder
        arrangement.now = Instant.parse("2026-10-07T14:50:30Z")
        arrangement.scheduledAlarms.last().cancel() // The system may release the PendingIntent after dispatching the alarm.
        val restartedScheduler = arrangement.newScheduler()
        backgroundScope.launch { restartedScheduler.observe() }
        runCurrent()
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
        backgroundScope.launch { arrangement.scheduler.observe() }
        runCurrent()
        arrangement.now = ALARM_TIME

        arrangement.scheduler.onAlarm(arrangement.user.id, FIRST_START)

        verify(exactly = 0) { arrangement.notificationManager.showReminder(any(), any()) }
        assertEquals(2, arrangement.scheduledAlarms.size)
    }

    @Test
    fun givenPendingAdmission_whenObserving_thenKeepStoredAlarmUntilRawAccountRemoval() = runTest {
        val arrangement = Arrangement().apply { upcomingStarts += FIRST_START }
        arrangement.scheduler.restore()
        arrangement.admission.publish(arrangement.user.id, null)
        backgroundScope.launch { arrangement.scheduler.observe() }
        runCurrent()
        verify(exactly = 0) { arrangement.alarmManager.cancel(any<PendingIntent>()) }
        coVerify(exactly = 1) { arrangement.reminders.next(any()) } // The initial restore only.

        arrangement.rawAccounts.value = GetAllSessionsResult.Success(emptyList())
        runCurrent()
        verify(exactly = 1) { arrangement.alarmManager.cancel(arrangement.scheduledAlarms.single()) }
        verify(exactly = 0) { arrangement.coreLogic.getSessionScope(any()) }
    }

    @Test
    fun givenSuspendedReminderQuery_whenAccountIsRemoved_thenDrainWithoutSchedulingLateAlarm() = runTest {
        val entered = CompletableDeferred<Unit>()
        val drained = CompletableDeferred<Unit>()
        val arrangement = Arrangement()
        coEvery { arrangement.reminders.next(any()) } coAnswers {
            entered.complete(Unit)
            try { awaitCancellation() } finally { drained.complete(Unit) }
        }
        backgroundScope.launch { arrangement.scheduler.observe() }
        entered.await()
        arrangement.rawAccounts.value = GetAllSessionsResult.Success(emptyList())
        runCurrent()
        assertTrue(drained.isCompleted)
        assertEquals(0, arrangement.scheduledAlarms.size)
        verify(exactly = 0) { arrangement.coreLogic.getSessionScope(any()) }
    }

    @Test
    fun givenPendingAccount_whenAnotherAccountIsReady_thenScheduleTheReadyAccountIndependently() = runTest {
        val arrangement = Arrangement()
        val otherId = TestUser.OTHER_USER.id
        val other = mockk<UserSessionScope>(relaxed = true)
        coEvery { other.observeIsMeetingsEnabled() } returns flowOf(true)
        every { other.meetings.meetingReminders.changes() } returns flowOf(Unit)
        coEvery { other.meetings.meetingReminders.next(any()) } returns null
        arrangement.admission.publish(arrangement.user.id, null)
        arrangement.admission.publish(otherId, other)
        arrangement.rawAccounts.value = GetAllSessionsResult.Success(
            listOf(AccountInfo.Valid(arrangement.user.id), AccountInfo.Valid(otherId))
        )
        backgroundScope.launch { arrangement.scheduler.observe() }
        runCurrent()
        coVerify(exactly = 1) { other.meetings.meetingReminders.next(any()) }
        verify(exactly = 0) { arrangement.coreLogic.getSessionScope(any()) }
    }

    @Test
    fun givenPendingRestoreAccount_whenAnotherAccountIsReady_thenRestoreItWithoutWaitingForTheFirst() = runTest {
        val arrangement = Arrangement()
        val otherId = TestUser.OTHER_USER.id
        val other = mockk<UserSessionScope>(relaxed = true)
        coEvery { other.meetings.meetingReminders.next(any()) } returns null
        arrangement.admission.publish(arrangement.user.id, null)
        arrangement.admission.publish(otherId, other)
        arrangement.rawAccounts.value = GetAllSessionsResult.Success(
            listOf(AccountInfo.Valid(arrangement.user.id), AccountInfo.Valid(otherId))
        )
        val restoring = async { arrangement.scheduler.restore() }
        runCurrent()
        assertTrue(restoring.isActive)
        coVerify(exactly = 1) { other.meetings.meetingReminders.next(any()) }
        restoring.cancel()
        restoring.join()
        assertTrue(restoring.isCancelled)
    }

    @Test
    fun givenAlarmWithPendingAdmission_whenPublished_thenConsumeStoredOwnershipAndNotifyOnce() = runTest {
        val arrangement = Arrangement().apply { upcomingStarts += FIRST_START }
        val reminder = arrangement.reminder(FIRST_START)
        arrangement.remindersInWindow += reminder
        arrangement.scheduler.restore()
        arrangement.now = ALARM_TIME
        arrangement.admission.publish(arrangement.user.id, null)
        val alarm = async { arrangement.scheduler.onAlarm(arrangement.user.id, FIRST_START) }
        runCurrent()
        assertTrue(alarm.isActive)
        verify(exactly = 0) { arrangement.notificationManager.showReminder(any(), any()) }
        arrangement.admission.publish(arrangement.user.id, arrangement.session)
        alarm.await()
        arrangement.scheduler.onAlarm(arrangement.user.id, FIRST_START)
        verify(exactly = 1) { arrangement.notificationManager.showReminder(reminder, arrangement.user.id) }
        verify(exactly = 0) { arrangement.coreLogic.getSessionScope(any()) }
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
        var meetingsEnabled: Flow<Boolean> = flowOf(true)
        var reminderChanges: Flow<Unit> = flowOf(Unit)
        private val observeIsMeetingsEnabled = mockk<ObserveIsMeetingsEnabledUseCase>()
        private val meetings = mockk<MeetingScope>()
        private val calls = mockk<CallsScope>()
        private val establishedCall = mockk<ObserveEstablishedCallsUseCase>()
        val session = mockk<UserSessionScope>()
        private val global = mockk<GlobalKaliumScope>()
        private val validSession = mockk<DoesValidSessionExistUseCase>()
        val coreLogic = mockk<CoreLogic>()
        val rawAccounts = MutableStateFlow<GetAllSessionsResult>(GetAllSessionsResult.Success(listOf(AccountInfo.Valid(user.id))))
        val admission = FakeSessionAdmission(coreLogic) { session }
        private val context = ApplicationProvider.getApplicationContext<Application>()
        val scheduler = newScheduler()

        init {
            every { coreLogic.getGlobalScope() } returns global
            coEvery { global.observeAllValidSessionsFlow() } returns rawAccounts
            every { global.doesValidSessionExist } returns validSession
            coEvery { validSession(user.id) } returns DoesValidSessionExistResult.Success(true)
            every { session.meetings } returns meetings
            every { session.observeIsMeetingsEnabled } returns observeIsMeetingsEnabled
            coEvery { observeIsMeetingsEnabled() } answers { meetingsEnabled }
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
            every { reminders.changes() } answers { reminderChanges }
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
