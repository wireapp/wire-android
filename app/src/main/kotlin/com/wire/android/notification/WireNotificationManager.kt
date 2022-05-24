package com.wire.android.notification

import com.wire.android.appLogger
import com.wire.android.di.GetIncomingCallsUseCaseProvider
import com.wire.android.di.GetNotificationsUseCaseProvider
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.util.extension.intervalFlow
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.notification.LocalNotificationConversation
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.call.Call
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.take
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WireNotificationManager @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val getNotificationProvider: GetNotificationsUseCaseProvider.Factory,
    private val getIncomingCallsProvider: GetIncomingCallsUseCaseProvider.Factory,
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
            println("cyka fetchAndShowNotificationsOnce $userId")
            fetchAndShowMessageNotificationsOnce(userId)
            fetchAndShowCallNotificationsOnce(userId)
        }
    }

    private suspend fun fetchAndShowCallNotificationsOnce(userId: QualifiedID) {
        //TODO for now GetIncomingCallsUseCase() returns valid data not from the first try.
        // so it's possible to have scenario, when FCM comes informing us that there is a Call,
        // but we don't get it from the first GetIncomingCallsUseCase() call.
        // To cover that case we have this `intervalFlow().take(10)`
        // to try get incoming calls 10 times, if it returns nothing we assume there is no incoming call
        intervalFlow(1000L, 0L)
            .map {
                getIncomingCallsProvider.create(userId)
                    .getCalls()
                    .first()
            }
            .take(10)
            .map {
                println("cyka once: ${it.size}")
                it
            }
            .distinctUntilChanged()
            .collect { callsList ->
                println("cyka once: collecting $callsList")
                callsManager.handleNotifications(callsList, userId)
            }
    }

    private suspend fun fetchAndShowMessageNotificationsOnce(userId: QualifiedID) {
        val notificationsList = getNotificationProvider.create(userId)
            .getNotifications()
            .first()

        messagesManager.handleNotification(listOf(), notificationsList, userId)
    }

    // TODO : to be changed as soon as we get the qualifiedID from the notification payload
    /**
     * return the userId if the user is authenticated and null otherwise
     */
    @Suppress("NestedBlockDepth")
    private fun checkIfUserIsAuthenticated(userId: String): QualifiedID? =
        coreLogic.getAuthenticationScope().getSessions().let {
            when (it) {
                is GetAllSessionsResult.Success -> {
                    for (sessions in it.sessions) {
                        if (sessions.userId.value == userId)
                            return@let sessions.userId
                    }
                    null
                }
                is GetAllSessionsResult.Failure.Generic -> {
                    appLogger.e("get sessions failed ${it.genericFailure} ")
                    null
                }
                GetAllSessionsResult.Failure.NoSessionFound -> null
            }
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
                println("cyka observing $userId")
                if (userId == null) {
                    flowOf(listOf())
                } else {
                    getIncomingCallsProvider.create(userId).getCalls()
                }
                    .map { list -> list to userId }
            }
            .collect { (calls, userId) ->
//                println("cyka observing calls $calls")
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
                    getNotificationProvider.create(userId).getNotifications()
                } else {
                    // if userId == null means there is no current user (e.g., logged out)
                    // so we need to unsubscribe from the notification changes (it's done by `flatMapLatest`)
                    // and remove the notifications that were displayed previously
                    // (empty list in here makes all the pre. notifications be removed)
                    flowOf(listOf())
                }
                    // we need to remember prev. displayed Notifications,
                    // so we can remove notifications that were displayed previously but are not in the new list
                    .scan((listOf<LocalNotificationConversation>() to listOf<LocalNotificationConversation>()))
                    { old, newList -> old.second to newList }
                    // combining all the data that is necessary for Notifications into small data class,
                    // just to make it more readable than
                    // Triple<List<LocalNotificationConversation>, List<LocalNotificationConversation>, QualifiedID?>
                    .map { (oldNotifications, newNotifications) ->
                        MessagesNotificationsData(oldNotifications, newNotifications, userId)
                    }
            }
            .collect { (oldNotifications, newNotifications, userId) ->
                messagesManager.handleNotification(oldNotifications, newNotifications, userId)
            }
    }

    private data class MessagesNotificationsData(
        val oldNotifications: List<LocalNotificationConversation>,
        val newNotifications: List<LocalNotificationConversation>,
        val userId: QualifiedID?
    )
}
