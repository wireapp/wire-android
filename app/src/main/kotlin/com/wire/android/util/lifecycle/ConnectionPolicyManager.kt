package com.wire.android.util.lifecycle

import com.wire.android.appLogger
import com.wire.android.di.KaliumCoreLogic
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
import com.wire.kalium.logic.functional.map
import com.wire.kalium.logic.functional.nullableFold
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class ConnectionPolicyManager @Inject constructor(
    private val currentScreenManager: CurrentScreenManager,
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val dispatcherProvider: DispatcherProvider
) {

    private val logger = appLogger.withFeatureId(SYNC)

    /**
     * Starts observing the app state and take action.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun startObservingAppLifecycle() {
        CoroutineScope(dispatcherProvider.default()).launch {
            currentScreenManager.isAppOnForegroundFlow()
                .combine(currentSessionFlow()) { isOnForeground, currentSession -> isOnForeground to currentSession }
                .collect { (isOnForeground, currentSession) ->
                    setPolicyForSessions(allValidSessions(), currentSession, isOnForeground)
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
    suspend fun handleConnectionOnPushNotification(userId: UserId) {
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
            syncManager.waitUntilLive()
            logger.d("$TAG Checking if downgrading policy is needed")
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
        val isCurrentSession = isCurrentSession(userId)
        val isAppOnForeground = currentScreenManager.isAppOnForegroundFlow().first()
        logger.d("isCurrentSession = $isCurrentSession; hasInitialisedUI = $isCurrentSession")
        val shouldKeepLivePolicy = isCurrentSession && isAppOnForeground
        if (!shouldKeepLivePolicy) {
            logger.d("$TAG Downgrading policy as conditions to KEEP_ALIVE are not met")
            setConnectionPolicy(ConnectionPolicy.DISCONNECT_AFTER_PENDING_EVENTS)
        }
    }

    private fun isCurrentSession(userId: UserId): Boolean {
        val isCurrentSession = coreLogic.getGlobalScope().sessionRepository.currentSession().fold({
            // Assume so in case of failure
            true
        }, {
            it.userId == userId
        })
        return isCurrentSession
    }

    private fun setPolicyForSessions(
        userIdList: List<QualifiedID>,
        currentSessionUserId: QualifiedID?,
        wasUIInitialized: Boolean
    ) = userIdList.forEach { userId ->
        val isCurrentSession = userId == currentSessionUserId
        val connectionPolicy = if (isCurrentSession && wasUIInitialized) {
            ConnectionPolicy.KEEP_ALIVE
        } else {
            ConnectionPolicy.DISCONNECT_AFTER_PENDING_EVENTS
        }
        CoroutineScope(dispatcherProvider.default()).launch {
            coreLogic.getSessionScope(userId).setConnectionPolicy(connectionPolicy)
        }
    }

    private suspend fun allValidSessions() =
        coreLogic.getGlobalScope().sessionRepository.allValidSessions()
            .map { it.map { session -> session.userId } }.fold({ emptyList() }, { it })

    private fun currentSessionFlow() =
        coreLogic.getGlobalScope().sessionRepository.currentSessionFlow()
            .map { it.nullableFold({ null }, { currentSession -> currentSession.userId }) }

    companion object {
        private const val TAG = "ConnectionPolicyManager"
    }
}
