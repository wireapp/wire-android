package com.wire.android.notification.broadcastreceivers

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.di.NoSession
import com.wire.android.di.metro.wireApplicationGraph
import com.wire.android.notification.MeetingReminderScheduler
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.id.toQualifiedID
import dev.zacsweers.metro.Inject
import kotlinx.datetime.Instant

class MeetingReminderReceiver : CoroutineReceiver() {
    @Inject
    lateinit var scheduler: MeetingReminderScheduler

    @Inject
    @NoSession
    lateinit var qualifiedIdMapper: QualifiedIdMapper

    @Inject
    lateinit var globalDataStore: GlobalDataStore

    override fun onReceive(context: Context, intent: Intent?) {
        context.wireApplicationGraph.inject(this)
        super.onReceive(context, intent)
    }

    override suspend fun receive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> scheduler.restore()
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val alarmManager = context.getSystemService(AlarmManager::class.java)
                    globalDataStore.setExactAlarmAccessDialogSeen(false)
                    if (alarmManager.canScheduleExactAlarms()) scheduler.restore()
                }
            }
            MeetingReminderScheduler.ACTION -> {
                val userId = intent.getStringExtra(MeetingReminderScheduler.EXTRA_USER_ID)?.toQualifiedID(qualifiedIdMapper)
                val startTime = intent.getLongExtra(MeetingReminderScheduler.EXTRA_START_TIME, -1L)
                if (userId != null && startTime > 0L) {
                    scheduler.onAlarm(userId, Instant.fromEpochMilliseconds(startTime))
                }
            }
        }
    }
}
