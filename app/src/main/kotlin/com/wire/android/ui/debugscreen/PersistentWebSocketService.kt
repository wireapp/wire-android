package com.wire.android.ui.debugscreen

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.di.CurrentSessionFlowService
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.notification.NotificationConstants.WEB_SOCKET_CHANNEL_ID
import com.wire.android.notification.NotificationConstants.WEB_SOCKET_CHANNEL_NAME
import com.wire.android.notification.WireNotificationManager
import com.wire.android.ui.WireActivity
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.ConversationId
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

    private val navigationManager: NavigationManager = NavigationManager()

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
            coreLogic.getSessionScope(authSession.session.userId).setConnectionPolicy(ConnectionPolicy.KEEP_ALIVE)

            val observeUserId = currentSessionFlow()
                .map { result ->
                    if (result is CurrentSessionResult.Success) result.authSession.session.userId
                    else null
                }
                .distinctUntilChanged()
                .flowOn(Dispatchers.IO)
                .shareIn(scope, SharingStarted.WhileSubscribed(), 1)


            scope.launch {
                notificationManager.observeNotificationsAndCalls(observeUserId, scope) {
                    openIncomingCall(it.conversationId)
                }
            }
        })



        generateForegroundNotification()
        return START_STICKY

    }

    private fun openIncomingCall(conversationId: ConversationId) {
        scope.launch {
            navigationManager.navigate(NavigationCommand(NavigationItem.IncomingCall.getRouteWithArgs(listOf(conversationId))))
        }
    }

    private val notification_ID = 123

    private fun generateForegroundNotification() {
        val pendingIntent: PendingIntent =
            Intent(this, WireActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(
                    this, 0, notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            }

        val channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel()
            } else {
                // If earlier version channel ID is not used
                ""
            }

        val notification: Notification = Notification.Builder(this, channelId)
            .setContentTitle(
                StringBuilder(resources.getString(R.string.app_name)).append(getString(R.string.service_is_running)).toString()
            )
            .setSmallIcon(R.drawable.notification_icon_small)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(notification_ID, notification)

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): String {
        val chan = NotificationChannel(
            WEB_SOCKET_CHANNEL_ID,
            WEB_SOCKET_CHANNEL_NAME, NotificationManager.IMPORTANCE_NONE
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return WEB_SOCKET_CHANNEL_ID
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    companion object {
        const val ACTION_STOP_FOREGROUND = "${BuildConfig.APPLICATION_ID}.stopforeground"
    }
}
