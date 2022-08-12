package com.wire.android.util.lifecycle

import com.wire.android.appLogger
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logger.KaliumLogger.Companion.ApplicationFlow.SYNC
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.sync.ConnectionPolicy
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.functional.flatMap
import com.wire.kalium.logic.functional.fold
import com.wire.kalium.logic.functional.map
import com.wire.kalium.logic.functional.onSuccess
import kotlinx.coroutines.CoroutineScope
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
    private val dispatcherProvider: DispatcherProvider
) {

    private val logger = appLogger.withFeatureId(SYNC)

    /**
     * Starts observing the app state and take action.
     */
    fun startObservingAppLifecycle() {
        CoroutineScope(dispatcherProvider.default()).launch {
            currentScreenManager.appWasVisibleAtLeastOnceFlow().collect { wasUIInitialized ->
                getAllSessionsAndCurrentSession().onSuccess { (userIdList, currentSessionUserId) ->
                    setPolicyForSessions(userIdList, currentSessionUserId, wasUIInitialized)
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
    suspend fun handleConnectionOnPushNotification(userId: UserId) {
        logger.d("Handling connection policy for push notification of user=$userId")
        coreLogic.getSessionScope(userId).run {
            logger.d("Forcing KEEP_ALIVE policy")
            // Force KEEP_ALIVE policy, so we gather pending events and become online
            setConnectionPolicy(ConnectionPolicy.KEEP_ALIVE)
            // Wait until the client is live and pending events are processed
            logger.d("Waiting until live")
            syncManager.waitUntilLive()
            logger.d("Downgrading policy back if needed")
            downgradePolicyIfNeeded(userId)
        }
    }

    /**
     * If the app has initialised the UI (was in the foreground at least once),
     * and the session at hands is the currently active session, then we can keep
     * the [ConnectionPolicy.KEEP_ALIVE] policy.
     * Otherwise, we downgrade to [ConnectionPolicy.DISCONNECT_AFTER_PENDING_EVENTS].
     */
    private fun UserSessionScope.downgradePolicyIfNeeded(
        userId: UserId
    ) {
        val isCurrentSession = isCurrentSession(userId)
        val hasInitialisedUI = currentScreenManager.appWasVisibleAtLeastOnceFlow().value
        logger.d("isCurrentSession = $isCurrentSession; hasInitialisedUI = $isCurrentSession")
        val shouldKeepLivePolicy = isCurrentSession && hasInitialisedUI
        if (!shouldKeepLivePolicy) {
            logger.d("Downgrading policy as conditions to KEEP_ALIVE are not met")
            setConnectionPolicy(ConnectionPolicy.DISCONNECT_AFTER_PENDING_EVENTS)
        }
    }

    private fun isCurrentSession(userId: UserId): Boolean {
        val isCurrentSession = coreLogic.sessionRepository.currentSession().fold({
            // Assume so in case of failure
            true
        }, {
            it.session.userId == userId
        })
        return isCurrentSession
    }

    private fun setPolicyForSessions(
        userIdList: List<QualifiedID>,
        currentSessionUserId: QualifiedID,
        wasUIInitialized: Boolean
    ) = userIdList.forEach { userId ->
        val isCurrentSession = userId == currentSessionUserId
        val connectionPolicy = if (isCurrentSession && wasUIInitialized) {
            ConnectionPolicy.KEEP_ALIVE
        } else {
            ConnectionPolicy.DISCONNECT_AFTER_PENDING_EVENTS
        }
        coreLogic.getSessionScope(userId).setConnectionPolicy(connectionPolicy)
    }

    private fun getAllSessionsAndCurrentSession() = coreLogic.sessionRepository.currentSession().flatMap { authSession ->
        coreLogic.sessionRepository.allValidSessions().map { sessions ->
            sessions.map {
                it.session.userId
            } to authSession.session.userId
        }
    }
}
