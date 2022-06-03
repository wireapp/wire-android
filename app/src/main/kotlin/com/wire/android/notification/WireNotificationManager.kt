package com.wire.android.notification

import com.wire.android.di.KaliumCoreLogic
import com.wire.android.util.extension.intervalFlow
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.notification.LocalNotificationConversation
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.call.Call
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WireNotificationManager @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val messagesManager: MessageNotificationManager,
    private val callsManager: CallNotificationManager,
) {
    /**
     * Sync all the Pending events, fetch Message notifications from DB once and show it.
     * Can be used in Services (e.g., after receiving FCM)
     * @param userIdValue String value param of QualifiedID of the User that need to check Notifications for
     */
    suspend fun fetchAndShowNotificationsOnce(userIdValue: String) {
        checkIfUserIsAuthenticated(userId = userIdValue)?.let { userId ->
            coreLogic.getSessionScope(userId).syncPendingEvents()
            fetchAndShowMessageNotificationsOnce(userId)
            fetchAndShowCallNotificationsOnce(userId)
        }
    }

    private suspend fun fetchAndShowCallNotificationsOnce(userId: QualifiedID) {
        //TODO for now GetIncomingCallsUseCase() returns valid data not from the first try.
        // so it's possible to have scenario, when FCM comes informing us that there is a Call,
        // but we don't get it from the first GetIncomingCallsUseCase() call.
        // To cover that case we have this `intervalFlow().take(CHECK_INCOMING_CALLS_TRIES)`
        // to try get incoming calls 6 times, if it returns nothing we assume there is no incoming call
        intervalFlow(CHECK_INCOMING_CALLS_PERIOD_MS)
            .map {
                coreLogic.getSessionScope(userId)
                    .calls
                    .getIncomingCalls()
                    .first()
            }
            .take(CHECK_INCOMING_CALLS_TRIES)
            .distinctUntilChanged()
            .collect { callsList ->
                callsManager.handleNotifications(callsList, userId)
            }
    }

    private suspend fun fetchAndShowMessageNotificationsOnce(userId: QualifiedID) {
        val notificationsList = coreLogic.getSessionScope(userId)
            .messages
            .getNotifications()
            .first()

        messagesManager.handleNotification(notificationsList, userId)
    }

    /**
     * Showing notifications for some other user then Current one requires to change user on opening Notification,
     * which is not implemented yet.
     * So for now we show notifications only for Current User
     *
     * return the userId if the user is authenticated and null otherwise
     */
    private fun checkIfUserIsAuthenticated(userId: String): QualifiedID? =
        when (val currentSession = coreLogic.getAuthenticationScope().session.currentSession()) {
            is CurrentSessionResult.Success -> currentSession.authSession.userId
            else -> null
        }

    /**
     * Infinitely listen for the new IncomingCalls, notify about it and do additional actions if needed.
     * Can be used for listening for the Notifications when the app is running.
     * @param userIdFlow Flow of QualifiedID of User
     * @param isAppVisibleFlow StateFlow that informs if app is currently visible,
     * so we can decide: should we show notification, or just open the IncomingCall screen
     */
    suspend fun observeIncomingCalls(
        isAppVisibleFlow: StateFlow<Boolean>,
        userIdFlow: Flow<UserId?>,
        doIfCallCameAndAppVisible: (Call) -> Unit
    ) {
        userIdFlow
            .flatMapLatest { userId ->
                if (userId == null) {
                    flowOf(listOf())
                } else {
                    coreLogic.getSessionScope(userId)
                        .calls
                        .getIncomingCalls()
                }
                    .map { list -> list to userId }
            }
            .collect { (calls, userId) ->
                if (isAppVisibleFlow.value) {
                    calls.firstOrNull()?.run { doIfCallCameAndAppVisible(this) }
                } else {
                    callsManager.handleNotifications(calls, userId)
                }
            }
    }

    /**
     * Infinitely listen for the new Message notifications and show it.
     * Can be used for listening for the Notifications when the app is running.
     * @param userIdFlow Flow of QualifiedID of User
     */
    @ExperimentalCoroutinesApi
    suspend fun observeMessageNotifications(userIdFlow: Flow<UserId?>) {
        userIdFlow
            .flatMapLatest { userId ->
                if (userId != null) {
                    coreLogic.getSessionScope(userId)
                        .messages
                        .getNotifications()
                } else {
                    // if userId == null means there is no current user (e.g., logged out)
                    // so we need to unsubscribe from the notification changes (it's done by `flatMapLatest`)
                    // and remove the notifications that were displayed previously
                    // (empty list in here makes all the pre. notifications be removed)
                    flowOf(listOf())
                }
                    // combining all the data that is necessary for Notifications into small data class,
                    // just to make it more readable than
                    // Pair<List<LocalNotificationConversation>, QualifiedID?>
                    .map { newNotifications ->
                        MessagesNotificationsData(newNotifications, userId)
                    }
            }
            .collect { (newNotifications, userId) ->
                messagesManager.handleNotification(newNotifications, userId)
            }
    }

    private data class MessagesNotificationsData(
        val newNotifications: List<LocalNotificationConversation>,
        val userId: QualifiedID?
    )

    companion object {
        private const val CHECK_INCOMING_CALLS_PERIOD_MS = 1000L
        private const val CHECK_INCOMING_CALLS_TRIES = 6
    }
}
