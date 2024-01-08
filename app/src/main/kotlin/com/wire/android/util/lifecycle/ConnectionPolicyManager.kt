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

package com.wire.android.util.lifecycle

import com.wire.android.appLogger
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.migration.MigrationManager
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logger.KaliumLogger.Companion.ApplicationFlow.SYNC
import com.wire.kalium.logger.obfuscateDomain
import com.wire.kalium.logger.obfuscateId
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.sync.ConnectionPolicy
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.functional.fold
import com.wire.kalium.logic.functional.isLeft
import com.wire.kalium.logic.functional.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Observes the CurrentScreen and changes the
 * ConnectionPolicy based on the latest value.
 *
 * If the app was recently in the foreground, only the
 * current session will have [ConnectionPolicy.KEEP_ALIVE].
 * All other sessions will have [ConnectionPolicy.DISCONNECT_AFTER_PENDING_EVENTS].
 *
 * When the app is initialised without displaying any UI all sessions are
 * set to [ConnectionPolicy.DISCONNECT_AFTER_PENDING_EVENTS].
 */
@Singleton
class ConnectionPolicyManager @Inject constructor(
    private val currentScreenManager: CurrentScreenManager,
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val dispatcherProvider: DispatcherProvider,
    private val migrationManager: MigrationManager
) {

    private val logger by lazy { appLogger.withFeatureId(SYNC) }

    /**
     * Starts observing the app state and take action.
     */
    fun startObservingAppLifecycle() {
        CoroutineScope(dispatcherProvider.default()).launch {
            combine(
                currentScreenManager.isAppVisibleFlow(),
                migrationManager.isMigrationCompletedFlow(),
                ::Pair
            ).collect { (isVisible, isMigrationCompleted) ->
                if (isMigrationCompleted) {
                    setPolicyForSessions(allValidSessions(), isVisible)
                }
            }
        }
    }

    /**
     * When a push notification is received for a [userId], this will
     * upgrade the policy to [ConnectionPolicy.KEEP_ALIVE] and suspend
     * until the client is online.
     *
     * Depending on the current screen and the active session,
     * this will downgrade the policy back to [ConnectionPolicy.DISCONNECT_AFTER_PENDING_EVENTS].
     */
    suspend fun handleConnectionOnPushNotification(userId: UserId, stayAliveTimeMs: Long = 0) {
        logger.d(
            "$TAG Handling connection policy for push notification of " +
                    "user=${userId.value.obfuscateId()}@${userId.domain.obfuscateDomain()}"
        )
        coreLogic.getSessionScope(userId).run {
            logger.d("$TAG Forcing KEEP_ALIVE policy")
            // Force KEEP_ALIVE policy, so we gather pending events and become online
            setConnectionPolicy(ConnectionPolicy.KEEP_ALIVE)
            // Wait until the client is live and pending events are processed
            logger.d("$TAG Waiting until live")
            if (syncManager.waitUntilLiveOrFailure().isLeft()) {
                logger.w("$TAG Failed waiting until live")
            }
            logger.d("$TAG Checking if downgrading policy is needed (after small delay)")
            // this delay needed to have some time for getting the messages and calls from DB and displaying the notifications
            delay(stayAliveTimeMs)
            downgradePolicyIfNeeded(userId)
        }
    }

    /**
     * If the app has initialised the UI (was in the foreground at least once),
     * and the session at hands is the currently active session, then we can keep
     * the [ConnectionPolicy.KEEP_ALIVE] policy.
     * Otherwise, we downgrade to [ConnectionPolicy.DISCONNECT_AFTER_PENDING_EVENTS].
     */
    private suspend fun UserSessionScope.downgradePolicyIfNeeded(
        userId: UserId
    ) {
        val isAppVisible = currentScreenManager.isAppVisibleFlow().first()
        logger.d("$TAG isAppVisible = $isAppVisible")
        if (!isAppVisible) {
            logger.d("$TAG ${userId.toString().obfuscateId()} Downgrading policy as conditions to KEEP_ALIVE are not met")
            setConnectionPolicy(ConnectionPolicy.DISCONNECT_AFTER_PENDING_EVENTS)
        }
    }

    private suspend fun setPolicyForSessions(
        userIdList: List<QualifiedID>,
        isAppVisible: Boolean
    ) = userIdList.forEach { userId ->
        val isWebSocketEnabled = coreLogic.getSessionScope(userId).getPersistentWebSocketStatus()
        val connectionPolicy = if (isAppVisible) {
            ConnectionPolicy.KEEP_ALIVE
        } else {
            if (!isWebSocketEnabled) {
                ConnectionPolicy.DISCONNECT_AFTER_PENDING_EVENTS
            } else {
                ConnectionPolicy.KEEP_ALIVE
            }
        }
        coreLogic.getSessionScope(userId).setConnectionPolicy(connectionPolicy)
    }

    private suspend fun allValidSessions() =
        coreLogic.getGlobalScope().sessionRepository.allValidSessions()
            .map { it.map { session -> session.userId } }.fold({ emptyList() }, { it })

    companion object {
        private const val TAG = "ConnectionPolicyManager"
    }
}
