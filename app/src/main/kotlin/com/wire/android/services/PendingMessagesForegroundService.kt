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

package com.wire.android.services

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.di.metro.wireApplicationGraph
import com.wire.android.notification.NotificationChannelsManager
import com.wire.android.notification.NotificationConstants.PENDING_MESSAGES_SYNC_CHANNEL_ID
import com.wire.android.notification.NotificationConstants.PENDING_MESSAGES_SYNC_CHANNEL_NAME
import com.wire.android.notification.NotificationIds
import com.wire.android.notification.openAppPendingIntent
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.lifecycle.SyncLifecycleManager
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.LogoutCallback
import com.wire.kalium.logic.feature.session.DoesValidSessionExistResult
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class PendingMessagesForegroundService : Service() {

    @Inject
    @KaliumCoreLogic
    lateinit var coreLogic: CoreLogic

    @Inject
    lateinit var dispatcherProvider: DispatcherProvider

    @Inject
    lateinit var notificationChannelsManager: NotificationChannelsManager

    @Inject
    private lateinit var sendPendingMessagesAfterForegroundSync: SendPendingMessagesAfterForegroundSyncUseCase

    private val scope by lazy {
        CoroutineScope(SupervisorJob() + dispatcherProvider.io())
    }

    private var runJob: Job? = null

    private val logoutCallback = object : LogoutCallback {
        override suspend fun invoke(userId: UserId, reason: LogoutReason) {
            stopIfNoValidSessions()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        wireApplicationGraph.inject(this)
        super.onCreate()
        isServiceStarted = true
        coreLogic.getGlobalScope().logoutCallbackManager.register(logoutCallback)
        startAsForeground(createNotification(waitingForConnection = true))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isServiceStarted) {
            isServiceStarted = true
            startAsForeground(createNotification(waitingForConnection = true))
        }

        if (intent?.action == ACTION_STOP) {
            scope.launch {
                stopIfNoValidSessions(startId)
            }
            return START_NOT_STICKY
        }

        return if (runJob?.isActive == true) {
            appLogger.i("$TAG: pending messages send already running, skipping duplicate start")
            START_NOT_STICKY
        } else {
            runJob = scope.launch {
                try {
                    run()
                } finally {
                    stopSelf()
                }
            }

            START_NOT_STICKY
        }
    }

    private suspend fun run() {
        val connected = withTimeoutOrNull(MAX_WAIT_FOR_NETWORK_MINUTES.minutes) {
            networkAvailability().first { it }
        } ?: false

        if (!connected) {
            appLogger.i("$TAG: network did not become available before timeout")
            return
        }

        startAsForeground(createNotification(waitingForConnection = false))

        PendingMessagesForegroundSyncHandler(coreLogic, sendPendingMessagesAfterForegroundSync).sendPendingMessagesForAllValidSessions()
    }

    private suspend fun stopIfNoValidSessions(startId: Int? = null) {
        when (val result = coreLogic.getGlobalScope().session.allSessions()) {
            is GetAllSessionsResult.Success -> {
                if (result.sessions.any { it.isValid() }) {
                    appLogger.i("$TAG: keeping service alive for valid sessions")
                } else {
                    appLogger.i("$TAG: no valid sessions, stopping pending messages foreground service")
                    startId?.let(::stopSelf) ?: stopSelf()
                }
            }

            is GetAllSessionsResult.Failure -> {
                appLogger.w(
                    "$TAG: unable to get valid sessions after logout, " +
                            "stopping pending messages foreground service: $result"
                )
                startId?.let(::stopSelf) ?: stopSelf()
            }
        }
    }

    private fun networkAvailability(): Flow<Boolean> = callbackFlow {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        fun emitCurrentAvailability() {
            trySend(connectivityManager.hasValidatedInternet())
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = emitCurrentAvailability()
            override fun onLost(network: Network) = emitCurrentAvailability()
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) = emitCurrentAvailability()
        }

        emitCurrentAvailability()
        connectivityManager.registerNetworkCallback(
            NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build(),
            callback
        )
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }

    private fun ConnectivityManager.hasValidatedInternet(): Boolean =
        activeNetwork
            ?.let(::getNetworkCapabilities)
            ?.let {
                it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        it.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            } == true

    private fun startAsForeground(notification: Notification) {
        notificationChannelsManager.createRegularChannel(PENDING_MESSAGES_SYNC_CHANNEL_ID, PENDING_MESSAGES_SYNC_CHANNEL_NAME)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                ServiceCompat.startForeground(
                    this,
                    NotificationIds.PENDING_MESSAGES_SYNC_NOTIFICATION_ID.ordinal,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } catch (e: ForegroundServiceStartNotAllowedException) {
                appLogger.e("$TAG: failure while starting foreground: $e")
                stopSelf()
            }
        } else {
            ServiceCompat.startForeground(
                this,
                NotificationIds.PENDING_MESSAGES_SYNC_NOTIFICATION_ID.ordinal,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        }
    }

    private fun createNotification(waitingForConnection: Boolean): Notification =
        NotificationCompat.Builder(this, PENDING_MESSAGES_SYNC_CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(
                if (waitingForConnection) {
                    resources.getString(R.string.pending_messages_notification_waiting)
                } else {
                    resources.getString(R.string.pending_messages_notification_sending)
                }
            )
            .setSmallIcon(R.drawable.websocket_notification_icon_small)
            .setContentIntent(openAppPendingIntent(this))
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()

    override fun onTimeout(startId: Int, fgsType: Int) {
        appLogger.w("$TAG: foreground service timeout reached")
        stopSelf(startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        coreLogic.getGlobalScope().logoutCallbackManager.unregister(logoutCallback)
        runJob = null
        scope.cancel("PendingMessagesForegroundService was destroyed")
        isServiceStarted = false
    }

    companion object {
        private const val TAG = "PendingMessagesForegroundService"
        private const val MAX_WAIT_FOR_NETWORK_MINUTES = 30
        private const val ACTION_STOP = "com.wire.android.services.action.STOP_PENDING_MESSAGES_FOREGROUND_SERVICE"

        fun newIntent(context: Context): Intent =
            Intent(context, PendingMessagesForegroundService::class.java)

        fun stopIntent(context: Context): Intent =
            Intent(context, PendingMessagesForegroundService::class.java)
                .setAction(ACTION_STOP)

        var isServiceStarted = false
    }
}

internal class PendingMessagesForegroundSyncHandler(
    private val allSessions: suspend () -> GetAllSessionsResult,
    private val doesValidSessionExist: suspend (UserId) -> DoesValidSessionExistResult,
    private val sendPendingMessagesAfterForegroundSync: suspend (UserId) -> Unit,
) {
    constructor(
        coreLogic: CoreLogic,
        sendPendingMessagesAfterForegroundSync: SendPendingMessagesAfterForegroundSyncUseCase,
    ) : this(
        allSessions = { coreLogic.getGlobalScope().session.allSessions() },
        doesValidSessionExist = { coreLogic.getGlobalScope().doesValidSessionExist(it) },
        sendPendingMessagesAfterForegroundSync = sendPendingMessagesAfterForegroundSync::invoke
    )

    suspend fun sendPendingMessagesForAllValidSessions() {
        when (val result = allSessions()) {
            is GetAllSessionsResult.Success ->
                result.sessions
                    .filterIsInstance<AccountInfo.Valid>()
                    .map { it.userId }
                    .distinct()
                    .forEach { userId ->
                        sendPendingMessagesIfSessionIsStillValid(userId)
                    }

            is GetAllSessionsResult.Failure ->
                appLogger.w("$TAG: unable to get valid sessions, skipping pending messages send: $result")
        }
        appLogger.w("$TAG: foreground service finished messages sending")
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun sendPendingMessagesIfSessionIsStillValid(userId: UserId) {
        when (val result = doesValidSessionExist(userId)) {
            is DoesValidSessionExistResult.Success -> {
                if (result.doesValidSessionExist) {
                    try {
                        sendPendingMessagesAfterForegroundSync(userId)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        appLogger.e(
                            "$TAG: failed to send pending messages for ${userId.toLogString()}: $e"
                        )
                    }
                } else {
                    appLogger.w(
                        "$TAG: session for ${userId.toLogString()} is no longer valid, skipping pending messages send"
                    )
                }
            }

            is DoesValidSessionExistResult.Failure ->
                appLogger.w(
                    "$TAG: unable to check valid session for ${userId.toLogString()}, " +
                            "skipping pending messages send: $result"
                )
        }
    }

    private companion object {
        private const val TAG = "PendingMessagesForegroundService"
    }
}

class SendPendingMessagesAfterForegroundSyncUseCase @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val syncLifecycleManager: SyncLifecycleManager,
) {
    suspend operator fun invoke(userId: UserId) {
        syncLifecycleManager.syncTemporarily(
            userId = userId,
            stayAliveExtraDuration = 3.seconds,
            waitForNextSyncState = true,
        ) {
            val userSessionScope = coreLogic.getSessionScope(userId)
            appLogger.i(
                "$TAG: sending pending messages for ${userId.toLogString()}, " +
                        "syncState=${userSessionScope.syncManager.syncState.value}, " +
                        "userSessionScope=$userSessionScope"
            )
            userSessionScope.sendPendingMessages()
        }
    }

    private companion object {
        private const val TAG = "SendPendingMessagesAfterForegroundSyncUseCase"
    }
}
