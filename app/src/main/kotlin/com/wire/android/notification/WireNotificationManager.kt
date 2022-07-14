package com.wire.android.notification

import com.wire.android.appLogger
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.util.CurrentScreen
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.extension.intervalFlow
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.notification.LocalNotificationConversation
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.call.Call
import com.wire.kalium.logic.feature.conversation.ObserveConversationsAndConnectionsUseCase
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.logic.util.toStringDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@ExperimentalCoroutinesApi
@Suppress( "TooManyFunctions")
@Singleton
class WireNotificationManager @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val currentScreenManager: CurrentScreenManager,
    private val messagesNotificationManager: MessageNotificationManager,
    private val callNotificationManager: CallNotificationManager,
) {
    /**
     * Sync all the Pending events, fetch Message notifications from DB once and show it.
     * Can be used in Services (e.g., after receiving FCM)
     * @param userIdValue String value param of QualifiedID of the User that need to check Notifications for
     */
    suspend fun fetchAndShowNotificationsOnce(userIdValue: String) {
        checkIfUserIsAuthenticated(userId = userIdValue)?.let { userId ->
            //TODO: Move logic to Kalium.
            //      All of this could be handled inside Kalium,
            //      and Reloaded shouldn't need to call `waitUntilLive`.
            //      Kalium could be smarter
            coreLogic.getSessionScope(userId).syncManager.waitUntilLive()
            fetchAndShowMessageNotificationsOnce(userId)
            fetchAndShowCallNotificationsOnce(userId)
        }
    }

    private suspend fun fetchAndShowCallNotificationsOnce(userId: QualifiedID) {
        //TODO: for now GetIncomingCallsUseCase() doesn't return valid data on the first try.
        //      so it's possible to have scenario, when FCM comes informing us that there is a Call,
        //      but we don't get it from the first GetIncomingCallsUseCase() call.
        //      To cover that case we have this `intervalFlow().take(CHECK_INCOMING_CALLS_TRIES)`
        //      to try get incoming calls 6 times, if it returns nothing we assume there is no incoming call
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
                callNotificationManager.handleNotifications(callsList, userId)
            }
    }

    private suspend fun fetchAndShowMessageNotificationsOnce(userId: QualifiedID) {
        val notificationsList = coreLogic.getSessionScope(userId)
            .messages
            .getNotifications()
            .first()

        messagesNotificationManager.handleNotification(notificationsList, userId)
    }

    /**
     * Showing notifications for some other user then Current one requires to change user on opening Notification,
     * which is not implemented yet.
     * So for now we show notifications only for Current User
     *
     * return the userId if the user is authenticated and null otherwise
     */
    @Suppress("NestedBlockDepth")
    private fun checkIfUserIsAuthenticated(userId: String): QualifiedID? =
        coreLogic.globalScope { getSessions() }.let {
            when (it) {
                is GetAllSessionsResult.Success -> {
                    for (sessions in it.sessions) {
                        if (sessions.tokens.userId.value == userId)
                            return@let sessions.tokens.userId
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
     * Infinitely listen for the new IncomingCalls, CurrentScreen changes and Message Notifications
     * Notify user and mark Conversations as notified when it's needed.
     * @param userIdFlow Flow of QualifiedID of User
     * @param scope CoroutineScope used for observing CurrentScreen
     * @param doIfCallCameAndAppVisible action that should be done when incoming call comes and app is in foreground
     */
    suspend fun observeNotificationsAndCalls(
        userIdFlow: Flow<UserId?>,
        scope: CoroutineScope,
        doIfCallCameAndAppVisible: (Call) -> Unit
    ) {
        val currentScreenState = currentScreenManager.observeCurrentScreen(scope)

        scope.launch { observeCurrentScreenAndUpdateNotifyDate(currentScreenState, userIdFlow) }
        scope.launch { observeIncomingCalls(currentScreenState, userIdFlow, doIfCallCameAndAppVisible) }
        scope.launch { observeMessageNotifications(userIdFlow, currentScreenState) }

//        coreLogic.getSessionScope().conversations.observeConnectionList
    }

    /**
     * Infinitely listen for the CurrentScreen changes and update LastNotifiedDate for the Conversations when it's needed
     */
    private suspend fun observeCurrentScreenAndUpdateNotifyDate(
        currentScreenState: StateFlow<CurrentScreen>,
        userIdFlow: Flow<UserId?>
    ) {
        currentScreenState
            .combine(userIdFlow, ::Pair)
            .collect { (currentScreen, userId) ->
                if (userId == null) return@collect

                if (currentScreen is CurrentScreen.Conversation) {
                    markMessagesAsNotified(userId, currentScreen.id)
                }
            }
    }

    /**
     * Infinitely listen for the new IncomingCalls, notify about it and do additional actions if needed.
     * Can be used for listening for the Notifications when the app is running.
     * @param observeUserId Flow of QualifiedID of User
     * @param currentScreenState StateFlow that informs which screen is currently visible,
     * so we can decide: should we show notification, or run a @param[doIfCallCameAndAppVisible]
     */
    private suspend fun observeIncomingCalls(
        currentScreenState: StateFlow<CurrentScreen>,
        observeUserId: Flow<UserId?>,
        doIfCallCameAndAppVisible: (Call) -> Unit
    ) {
        observeUserId
            .flatMapLatest { userId ->
                if (userId == null) {
                    flowOf(listOf())
                } else {
                    coreLogic.getSessionScope(userId)
                        .calls
                        .getIncomingCalls()
                }.map { list -> list to userId }
            }
            .collect { (calls, userId) ->
                if (currentScreenState.value != CurrentScreen.InBackground) {
                    calls.firstOrNull()?.run { doIfCallCameAndAppVisible(this) }
                    callNotificationManager.hideCallNotification()
                } else {
                    callNotificationManager.handleNotifications(calls, userId)
                }
            }
    }


    /**
     * Infinitely listen for the new Message notifications and show it.
     * Can be used for listening for the Notifications when the app is running.
     * @param userIdFlow Flow of QualifiedID of User
     * @param currentScreenState StateFlow that informs which screen is currently visible,
     * so we can filter the notifications if user is in the Conversation that receives a new messages
     */
    @ExperimentalCoroutinesApi
    private suspend fun observeMessageNotifications(
        userIdFlow: Flow<UserId?>,
        currentScreenState: StateFlow<CurrentScreen>
    ) {
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
                    .map { newNotifications ->
                        // we don't want to display notifications for the Conversation that user currently in,
                        // or any notification when user is in ConversationsList.
                        val notificationsList = filterAccordingToScreenAndUpdateNotifyDate(
                            currentScreenState.value,
                            userId,
                            newNotifications
                        )
                        // combining all the data that is necessary for Notifications into small data class,
                        // just to make it more readable than
                        // Pair<List<LocalNotificationConversation>, QualifiedID?>
                        MessagesNotificationsData(notificationsList, userId)
                    }
            }
            .collect { (newNotifications, userId) ->
                messagesNotificationManager.handleNotification(newNotifications, userId)
            }
    }

    private suspend fun filterAccordingToScreenAndUpdateNotifyDate(
        currentScreen: CurrentScreen,
        userId: UserId?,
        newNotifications: List<LocalNotificationConversation>
    ) =
        if (currentScreen is CurrentScreen.Conversation) {
            markMessagesAsNotified(userId, currentScreen.id)
            newNotifications.filter { it.id != currentScreen.id }
        } else {
            newNotifications
        }

    private suspend fun markMessagesAsNotified(userId: QualifiedID?, conversationId: ConversationId?) {
        userId?.let {
            coreLogic.getSessionScope(it)
                .messages
                .markMessagesAsNotified(conversationId, System.currentTimeMillis().toStringDate())
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
