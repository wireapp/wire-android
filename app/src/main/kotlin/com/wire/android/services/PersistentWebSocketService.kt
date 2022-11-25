package com.wire.android.services

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.IBinder
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.di.CurrentSessionFlowService
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.notification.NotificationConstants.PERSISTENT_NOTIFICATION_ID
import com.wire.android.notification.NotificationConstants.WEB_SOCKET_CHANNEL_ID
import com.wire.android.notification.NotificationConstants.WEB_SOCKET_CHANNEL_NAME
import com.wire.android.notification.WireNotificationManager
import com.wire.android.notification.openAppPendingIntent
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.session.CurrentSessionFlowUseCase
import com.wire.kalium.logic.feature.user.webSocketStatus.ObservePersistentWebSocketConnectionStatusUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PersistentWebSocketService : Service() {

    @Inject
    @KaliumCoreLogic
    lateinit var coreLogic: CoreLogic

    @Inject
    lateinit var dispatcherProvider: DispatcherProvider

    private val scope by lazy {
        CoroutineScope(SupervisorJob() + dispatcherProvider.io())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Inject
    lateinit var notificationManager: WireNotificationManager

    @Inject
    @CurrentSessionFlowService
    lateinit var currentSessionFlow: CurrentSessionFlowUseCase

    @Inject
    lateinit var navigationManager: NavigationManager

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        isServiceStarted = true
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch {
            coreLogic.getGlobalScope().observePersistentWebSocketConnectionStatus().let {
                when (it) {
                    is ObservePersistentWebSocketConnectionStatusUseCase.Result.Failure -> {
                        appLogger.e("Failure while fetching persistent web socket status flow from service")
                    }
                    is ObservePersistentWebSocketConnectionStatusUseCase.Result.Success -> {
                        it.persistentWebSocketStatusListFlow.collect {
                            it.map { persistentWebSocketStatus ->
                                if (persistentWebSocketStatus.isPersistentWebSocketEnabled) {
                                    kotlinx.coroutines.runBlocking {
                                        coreLogic.getSessionScope(persistentWebSocketStatus.userId)
                                            .setConnectionPolicy(com.wire.kalium.logic.data.sync.ConnectionPolicy.KEEP_ALIVE)
                                    }
                                    notificationManager.observeNotificationsAndCalls(
                                        kotlinx.coroutines.flow.flowOf(
                                            persistentWebSocketStatus.userId
                                        ), scope
                                    ) {
                                        openIncomingCall(it.conversationId)
                                    }

                                } else {
                                    kotlinx.coroutines.runBlocking {
                                        coreLogic.getSessionScope(persistentWebSocketStatus.userId)
                                            .setConnectionPolicy(com.wire.kalium.logic.data.sync.ConnectionPolicy.DISCONNECT_AFTER_PENDING_EVENTS)
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }
        generateForegroundNotification()
        return START_STICKY

    }

    private fun openIncomingCall(conversationId: ConversationId) {
        scope.launch {
            navigationManager.navigate(NavigationCommand(NavigationItem.IncomingCall.getRouteWithArgs(listOf(conversationId))))
        }
    }

    private fun generateForegroundNotification() {
        val notificationManager = NotificationManagerCompat.from(this)
        val notificationChannel = NotificationChannelCompat
            .Builder(WEB_SOCKET_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName(WEB_SOCKET_CHANNEL_NAME)
            .build()

        notificationManager.createNotificationChannel(notificationChannel)

        val notification: Notification = Notification.Builder(this, WEB_SOCKET_CHANNEL_ID)
            .setContentTitle("${resources.getString(R.string.app_name)} ${resources.getString(R.string.settings_service_is_running)}")
            .setSmallIcon(R.drawable.websocket_notification_icon_small)
            .setContentIntent(openAppPendingIntent(this))
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()

        startForeground(PERSISTENT_NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel("PersistentWebSocketService was destroyed")
        isServiceStarted = false

    }

    companion object {
        fun newIntent(context: Context?): Intent =
            Intent(context, PersistentWebSocketService::class.java)
        var isServiceStarted = false
    }
}
