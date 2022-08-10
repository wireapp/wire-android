package com.wire.android.ui.debugscreen

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.di.CurrentSessionFlowService
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.notification.WireNotificationManager
import com.wire.android.ui.WireActivity
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.sync.ConnectionPolicy
import com.wire.kalium.logic.feature.session.CurrentSessionFlowUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.functional.fold
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PersistentWebSocketService : Service() {

    @Inject
    @KaliumCoreLogic
    lateinit var coreLogic: CoreLogic

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    @OptIn(ExperimentalCoroutinesApi::class)
    @Inject
    lateinit var notificationManager: WireNotificationManager

    @Inject
    @CurrentSessionFlowService
    lateinit var currentSessionFlow: CurrentSessionFlowUseCase


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != null && intent.action.equals(
                ACTION_STOP_FOREGROUND, ignoreCase = true
            )
        ) {
            stopForeground(true)
            stopSelf()
        }

        coreLogic.sessionRepository.currentSession().fold({

        }, { authSession ->
            coreLogic.getSessionScope(authSession.tokens.userId).setConnectionPolicy(ConnectionPolicy.KEEP_ALIVE)

            val observeUserId = currentSessionFlow()
                .map { result ->
                    if (result is CurrentSessionResult.Success) result.authSession.tokens.userId
                    else null
                }
                .distinctUntilChanged()
                .flowOn(Dispatchers.IO)
                .shareIn(scope, SharingStarted.WhileSubscribed(), 1)


            scope.launch {
                notificationManager.observeNotificationsAndCalls(observeUserId, scope) {
//                    openIncomingCall(it.conversationId)
                }
            }
        })



        generateForegroundNotification()
        return START_STICKY

    }

    //Notififcation for ON-going
    private var iconNotification: Bitmap? = null
    private var notification: Notification? = null
    var mNotificationManager: NotificationManager? = null
    private val mNotificationId = 123

    private fun generateForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intentMainLanding = Intent(this, WireActivity::class.java)
            val pendingIntent =
                PendingIntent.getActivity(this, 0, intentMainLanding, 0)
            iconNotification = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
            if (mNotificationManager == null) {
                mNotificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                assert(mNotificationManager != null)
                mNotificationManager?.createNotificationChannelGroup(
                    NotificationChannelGroup("chats_group", "Chats")
                )
                val notificationChannel =
                    NotificationChannel(
                        "service_channel", "Service Notifications",
                        NotificationManager.IMPORTANCE_MIN
                    )
                notificationChannel.enableLights(false)
                notificationChannel.lockscreenVisibility = Notification.VISIBILITY_SECRET
                mNotificationManager?.createNotificationChannel(notificationChannel)
            }
            val builder = NotificationCompat.Builder(this, "service_channel")

            builder.setContentTitle(StringBuilder(resources.getString(R.string.app_name)).append(" service is running").toString())
                .setTicker(StringBuilder(resources.getString(R.string.app_name)).append("service is running").toString())
                .setContentText("Touch to open") //                    , swipe down for more options.
                .setSmallIcon(R.drawable.ic_dropdown_icon)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setWhen(0)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
            if (iconNotification != null) {
                builder.setLargeIcon(Bitmap.createScaledBitmap(iconNotification!!, 128, 128, false))
            }
            notification = builder.build()
            startForeground(mNotificationId, notification)
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    companion object {
        const val ACTION_STOP_FOREGROUND = "${BuildConfig.APPLICATION_ID}.stopforeground"
    }
}
