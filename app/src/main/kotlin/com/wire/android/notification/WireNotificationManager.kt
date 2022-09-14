package com.wire.android.notification

import com.wire.android.appLogger
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.services.ServicesManager
import com.wire.android.util.CurrentScreen
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.extension.intervalFlow
import com.wire.android.util.lifecycle.ConnectionPolicyManager
import com.wire.kalium.logger.obfuscateId
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.notification.LocalNotificationConversation
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.call.Call
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.logic.util.toStringDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@ExperimentalCoroutinesApi
@Suppress("TooManyFunctions", "LongParameterList")
@Singleton
class WireNotificationManager @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val currentScreenManager: CurrentScreenManager,
    private val messagesNotificationManager: MessageNotificationManager,
    private val callNotificationManager: CallNotificationManager,
    private val connectionPolicyManager: ConnectionPolicyManager,
    private val servicesManager: ServicesManager,
    dispatcherProvider: DispatcherProvider
) {

    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.default())
    private val fetchOnceMutex = Mutex()
    private val fetchOnceJobs = hashMapOf<String, Job>()

    /**
     * Become online, process all the Pending events,
     * and display notifications for new events.
     * Can be used in Services (e.g., after receiving FCM push).
     *
     * This function is asynchronous and thread-safe.
     * It ignores successive calls with the same [userIdValue]
     * until the first call is finished processing.
     * @param userIdValue String value param of QualifiedID of the User that need to check Notifications for
     */
    suspend fun fetchAndShowNotificationsOnce(userIdValue: String) = fetchOnceMutex.withLock {
        if (isNotCurrentUser(userIdValue)) {
            appLogger.d("$TAG Ignoring notification for user=${userIdValue.obfuscateId()}, because not current user")
            return@withLock
        }
        val isJobRunningForUser = fetchOnceJobs[userIdValue]?.isActive ?: false
        if (isJobRunningForUser) {
            appLogger.d("$TAG Already processing notifications for user=${userIdValue.obfuscateId()}, ignoring request")
        } else {
            appLogger.d("$TAG Starting to processing notifications for user=${userIdValue.obfuscateId()}")
            fetchOnceJobs[userIdValue] = scope.launch {
                triggerSyncForUserIfAuthenticated(userIdValue)
            }
        }
    }

    private fun isNotCurrentUser(userId: String): Boolean {
        return when (val result = coreLogic.getGlobalScope().session.currentSession()) {
            is CurrentSessionResult.Success -> result.accountInfo.userId.value != userId
            else -> true // Fallback to display notifications anyway in case of unexpected error
        }
    }

    private suspend fun triggerSyncForUserIfAuthenticated(userIdValue: String) {
        checkIfUserIsAuthenticated(userId = userIdValue)?.let { userId ->
            appLogger.d("$TAG checking the notifications once")

            val observeMessagesJob = scope.launch {
                observeMessageNotifications(flowOf(userId), MutableStateFlow(CurrentScreen.InBackground))
            }
            appLogger.d("$TAG start syncing")
            connectionPolicyManager.handleConnectionOnPushNotification(userId)

            appLogger.d("$TAG checking calls once")
            fetchAndShowCallNotificationsOnce(userId)

            appLogger.d("$TAG checked the notifications once, canceling observing.")
            observeMessagesJob.cancel("$TAG checked the notifications once, canceling observing.")
        }
    }

    private suspend fun fetchAndShowCallNotificationsOnce(userId: QualifiedID) {
        // TODO: for now GetIncomingCallsUseCase() doesn't return valid data on the first try.
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
            .cancellable()
            .collect { callsList ->
                callNotificationManager.handleIncomingCallNotifications(callsList, userId)
            }
    }

    /**
     * Showing notifications for some other user then Current one requires to change user on opening Notification,
     * which is not implemented yet.
     * So for now we show notifications only for Current User
     *
     * return the userId if the user is authenticated and null otherwise
     */
    @Suppress("NestedBlockDepth")
    private suspend fun checkIfUserIsAuthenticated(userId: String): QualifiedID? =
        coreLogic.globalScope { getSessions() }.let {
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

        scope.launch { observeCurrentScreenAndHideNotifications(currentScreenState, userIdFlow) }
        scope.launch { observeIncomingCalls(currentScreenState, userIdFlow, doIfCallCameAndAppVisible) }
        scope.launch { observeMessageNotifications(userIdFlow, currentScreenState) }
        scope.launch { observeOngoingCalls(currentScreenState, userIdFlow) }
    }

    /**
     * Infinitely listen for the CurrentScreen changes and update Notifications if needed
     */
    private suspend fun observeCurrentScreenAndHideNotifications(
        currentScreenState: StateFlow<CurrentScreen>,
        userIdFlow: Flow<UserId?>
    ) {
        currentScreenState
            .combine(userIdFlow, ::Pair)
            .collect { (screens, userId) ->
                if (userId == null) {
                    // if userId == null means there is no current user (logged out e.g.)
                    // so we need to unsubscribe from the notification changes (it's done by flatMapLatest)
                    // and remove the notifications that were displayed previously
                    appLogger.i("$TAG no UserId -> hide all the notifications")
                    messagesNotificationManager.hideAllNotifications()
                    callNotificationManager.hideAllNotifications()
                    return@collect
                }

                when (screens) {
                    is CurrentScreen.Conversation -> messagesNotificationManager.hideNotification(screens.id)
                    is CurrentScreen.OtherUserProfile -> hideConnectionRequestNotification(userId, screens.id)
                    is CurrentScreen.IncomingCallScreen -> callNotificationManager.hideIncomingCallNotification()
                    else -> {}
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
                    calls.firstOrNull()?.run {
                        appLogger.d("$TAG got some call while app is visible")
                        doIfCallCameAndAppVisible(this)
                    }
                    callNotificationManager.hideIncomingCallNotification()
                } else {
                    appLogger.d("$TAG got ${calls.size} calls while app is in background")
                    callNotificationManager.handleIncomingCallNotifications(calls, userId)
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
                userId?.let {
                    coreLogic.getSessionScope(userId)
                        .messages
                        .getNotifications()
                        .cancellable()
                        // no need to do the whole work if there is no notifications
                        .filter {
                            appLogger.i("$TAG filtering notifications ${it.size}")
                            it.isNotEmpty()
                        }
                        .map { newNotifications ->
                            // we don't want to display notifications for the Conversation that user currently in.
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
                } ?: flowOf(null)
            }
            .cancellable()
            .filterNotNull()
            .collect { (newNotifications, userId) ->
                appLogger.d("$TAG got ${newNotifications.size} notifications")
                messagesNotificationManager.handleNotification(newNotifications, userId)
                markMessagesAsNotified(userId, null)
                markConnectionAsNotified(userId, null)
            }
    }

    /**
     * Infinitely listen for the established calls and run OngoingCall foreground Service
     * to show corresponding notification and do not lose a call.
     * @param userIdFlow Flow of QualifiedID of User
     * @param currentScreenState StateFlow that informs which screen is currently visible,
     * so we can listen established calls only when the app is in background.
     */
    private suspend fun observeOngoingCalls(
        currentScreenState: StateFlow<CurrentScreen>,
        userIdFlow: Flow<UserId?>
    ) {
        currentScreenState
            .combine(userIdFlow, ::Pair)
            .flatMapLatest { (currentScreen, userId) ->
                if (userId == null || currentScreen !is CurrentScreen.InBackground) {
                    flowOf(null)
                } else {
                    coreLogic.getSessionScope(userId).calls
                        .establishedCall()
                        .map {
                            it.firstOrNull()?.let { call ->
                                OngoingCallData(callNotificationManager.getNotificationTitle(call), call.conversationId, userId)
                            }
                        }
                }
            }
            .collect { ongoingCallData ->
                if (ongoingCallData == null) {
                    servicesManager.stopOngoingCallService()
                } else {
                    servicesManager.startOngoingCallService(
                        ongoingCallData.notificationTitle,
                        ongoingCallData.conversationId,
                        ongoingCallData.userId
                    )
                }
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
        appLogger.d("$TAG markMessagesAsNotified")
        userId?.let {
            coreLogic.getSessionScope(it)
                .messages
                .markMessagesAsNotified(conversationId, System.currentTimeMillis().toStringDate())
        }
    }

    private suspend fun markConnectionAsNotified(userId: QualifiedID?, connectionRequestUserId: QualifiedID?) {
        appLogger.d("$TAG markConnectionAsNotified")
        userId?.let {
            coreLogic.getSessionScope(it)
                .conversations
                .markConnectionRequestAsNotified(connectionRequestUserId)
        }
    }

    private suspend fun hideConnectionRequestNotification(userId: QualifiedID?, connectionRequestUserId: QualifiedID?) {
        // to hide ConnectionRequestNotification we need to get conversationId for it
        // cause it's used as Notification ID
        userId?.let {
            coreLogic.getSessionScope(it)
                .conversations
                .observeConnectionList()
                .first()
                .firstOrNull { conversationDetails ->
                    conversationDetails is ConversationDetails.Connection
                            && conversationDetails.otherUser?.id == connectionRequestUserId
                }
                ?.conversation
                ?.id
                ?.let { conversationId ->
                    messagesNotificationManager.hideNotification(conversationId)
                }
        }
    }

    private data class MessagesNotificationsData(
        val newNotifications: List<LocalNotificationConversation>,
        val userId: QualifiedID?
    )

    private data class OngoingCallData(val notificationTitle: String, val conversationId: ConversationId, val userId: UserId)

    companion object {
        private const val CHECK_INCOMING_CALLS_PERIOD_MS = 1000L
        private const val CHECK_INCOMING_CALLS_TRIES = 6
        private const val TAG = "WireNotificationManager"
    }
}
