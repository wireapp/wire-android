package com.wire.android.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.notification.broadcastreceivers.MeetingReminderReceiver
import com.wire.android.util.CurrentTimeProvider
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.meeting.MeetingReminder
import com.wire.kalium.logic.data.meeting.MeetingReminder.Companion.REMINDER_NOTIFICATION_LEAD_TIME
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.session.DoesValidSessionExistResult
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds

@SingleIn(AppScope::class)
class MeetingReminderScheduler @Inject constructor(
    private val context: Context,
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val alarmManager: AlarmManager,
    private val meetingNotificationManager: MeetingNotificationManager,
    private val currentTimeProvider: CurrentTimeProvider,
) {
    private val schedulingMutex = Mutex()
    private val scheduledReminderTimes = ScheduledReminderTimeStore(context)

    suspend fun observe() {
        coreLogic.getGlobalScope().observeValidAccounts()
            .map { accounts -> accounts.map { it.first.id }.toSet() }
            .scan(emptySet<UserId>() to emptySet<UserId>()) { (_, currentUsers), nextUsers -> currentUsers to nextUsers }
            .collectLatest { (previousUsers, users) ->
                (previousUsers - users).forEach { cancel(it) }
                coroutineScope {
                    users.forEach { userId ->
                        launch {
                            coreLogic.getSessionScope(userId).meetings.meetingReminders.changes().collect {
                                scheduleNext(userId)
                            }
                        }
                    }
                }
            }
    }

    suspend fun restore() {
        schedulingMutex.withLock { scheduledReminderTimes.clearAll() }
        coreLogic.getGlobalScope().observeValidAccounts().first().forEach { (user, _) -> scheduleNext(user.id) }
    }

    suspend fun onAlarm(userId: UserId, startTime: Instant) {
        val session = coreLogic.getGlobalScope().doesValidSessionExist(userId)
        if (session !is DoesValidSessionExistResult.Success || !session.doesValidSessionExist) {
            cancel(userId)
            return
        }
        val now = currentTimeProvider()
        val rangeEndExclusive = maxOf(startTime + 1.minutes, nextEligibleReminderTime(now))
        val isCurrentAlarm = schedulingMutex.withLock {
            if (scheduledReminderTimes.get(userId) != startTime || now < startTime - REMINDER_NOTIFICATION_LEAD_TIME) return@withLock false
            scheduledReminderTimes.clear(userId)
            scheduleNextLocked(userId, rangeEndExclusive - 1.nanoseconds, now, null)
            true
        }
        if (isCurrentAlarm) {
            val sessionScope = coreLogic.getSessionScope(userId)
            val ongoingCallConversationIds = sessionScope.calls.establishedCall().first().map { it.conversationId }.toSet()
            sessionScope.meetings.meetingReminders
                .within(startTime, rangeEndExclusive)
                .filter { it.startTime > now && it.conversationId !in ongoingCallConversationIds }
                .forEach { meetingNotificationManager.showReminder(it, userId) }
        }
    }

    private suspend fun scheduleNext(userId: UserId) {
        schedulingMutex.withLock {
            val now = currentTimeProvider()
            val scheduledReminderTime = scheduledReminderTimes.get(userId)
            if (scheduledReminderTime != null && scheduledReminderTime - REMINDER_NOTIFICATION_LEAD_TIME <= now) {
                val reminderWindowEndExclusive = maxOf(scheduledReminderTime + 1.minutes, nextEligibleReminderTime(now))
                val hasUpcomingMeeting = coreLogic.getSessionScope(userId).meetings.meetingReminders
                    .within(scheduledReminderTime, reminderWindowEndExclusive)
                    .any { it.startTime > now }
                if (hasUpcomingMeeting) return@withLock

                existingAlarmIntent(userId)?.let(alarmManager::cancel)
                scheduledReminderTimes.clear(userId)
                scheduleNextLocked(userId, reminderWindowEndExclusive - 1.nanoseconds, now, null)
                return@withLock
            }

            if (scheduledReminderTime != null && existingAlarmIntent(userId) == null) {
                scheduledReminderTimes.clear(userId)
                scheduleNextLocked(userId, nextEligibleReminderTime(now) - 1.nanoseconds, now, null)
            } else {
                scheduleNextLocked(userId, nextEligibleReminderTime(now) - 1.nanoseconds, now, scheduledReminderTime)
            }
        }
    }

    private suspend fun scheduleNextLocked(userId: UserId, nextReminderTime: Instant, now: Instant, scheduledReminderTime: Instant?) {
        val eligibleFrom = maxOf(nextReminderTime, nextEligibleReminderTime(now) - 1.nanoseconds)
        val next = coreLogic.getSessionScope(userId).meetings.meetingReminders.next(eligibleFrom)
        if (next == null) {
            if (scheduledReminderTime != null) {
                existingAlarmIntent(userId)?.let(alarmManager::cancel)
                scheduledReminderTimes.clear(userId)
            }
            return
        }
        val startTime = startOfMinute(next.startTime)
        if (scheduledReminderTime != startTime) {
            scheduleAlarm(userId, startTime)
            scheduledReminderTimes.save(userId, startTime)
        }
    }

    private fun scheduleAlarm(userId: UserId, rangeFrom: Instant) {
        val trigger = rangeFrom - REMINDER_NOTIFICATION_LEAD_TIME
        val pendingIntent = alarmIntent(userId, rangeFrom)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger.toEpochMilliseconds(), pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger.toEpochMilliseconds(), pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger.toEpochMilliseconds(), pendingIntent)
        }
    }

    private fun alarmBroadcastIntent(userId: UserId): Intent = Intent(context, MeetingReminderReceiver::class.java).apply {
        action = ACTION
        data = android.net.Uri.parse("$ALARM_INTENT_URI_PREFIX:$userId")
        putExtra(EXTRA_USER_ID, userId.toString())
    }

    private fun alarmIntent(userId: UserId, startTime: Instant): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        alarmBroadcastIntent(userId).putExtra(EXTRA_START_TIME, startTime.toEpochMilliseconds()),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun existingAlarmIntent(userId: UserId): PendingIntent? =
        PendingIntent.getBroadcast(context, 0, alarmBroadcastIntent(userId), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)

    private suspend fun cancel(userId: UserId) {
        schedulingMutex.withLock {
            existingAlarmIntent(userId)?.let(alarmManager::cancel)
            scheduledReminderTimes.clear(userId)
        }
    }

    private fun nextEligibleReminderTime(now: Instant): Instant = startOfMinute(now + REMINDER_NOTIFICATION_LEAD_TIME) + 1.minutes

    private fun startOfMinute(instant: Instant): Instant =
        Instant.fromEpochSeconds(instant.epochSeconds.floorDiv(minuteInSeconds) * minuteInSeconds)

    companion object {
        val minuteInSeconds = 1.minutes.inWholeSeconds
        const val ACTION = "com.wire.android.MEETING_REMINDER"
        const val EXTRA_USER_ID = "user_id"
        const val EXTRA_START_TIME = "start_time"
        const val ALARM_INTENT_URI_PREFIX = "wire-meeting-reminder:"
    }
}

private class ScheduledReminderTimeStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun get(userId: UserId): Instant? = preferences.getLong(userId.toString(), -1L)
        .takeIf { it > 0L }
        ?.let(Instant::fromEpochMilliseconds)
    fun save(userId: UserId, time: Instant) = preferences.edit().putLong(userId.toString(), time.toEpochMilliseconds()).apply()
    fun clear(userId: UserId) = preferences.edit().remove(userId.toString()).apply()
    fun clearAll() = preferences.edit().clear().apply()

    private companion object {
        const val PREFERENCES_NAME = "meeting_reminder_scheduled_range"
    }
}
