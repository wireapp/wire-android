package com.wire.android.util.lifecycle

import com.wire.android.di.KaliumCoreLogic
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.sync.ConnectionPolicy
import com.wire.kalium.logic.functional.flatMap
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
