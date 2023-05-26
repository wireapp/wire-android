/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.notification

import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.media.PingRinger
import com.wire.android.services.ServicesManager
import com.wire.android.util.CurrentScreen
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.lifecycle.ConnectionPolicyManager
import com.wire.kalium.logger.obfuscateId
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.notification.LocalNotificationConversation
import com.wire.kalium.logic.data.notification.LocalNotificationMessage
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.call.Call
import com.wire.kalium.logic.feature.message.MarkMessagesAsNotifiedUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("TooManyFunctions", "LongParameterList")
@Singleton
class WireNotificationManager @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val currentScreenManager: CurrentScreenManager,
    private val messagesNotificationManager: MessageNotificationManager,
    private val callNotificationManager: CallNotificationManager,
    private val connectionPolicyManager: ConnectionPolicyManager,
    private val servicesManager: ServicesManager,
    private val dispatcherProvider: DispatcherProvider,
    private val pingRinger: PingRinger
) {

    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.default())
    private val fetchOnceMutex = Mutex()
    private val fetchOnceJobs = hashMapOf<UserId, Job>()
    private val observingWhileRunningJobs = hashMapOf<UserId, ObservingJobs>()
    private val observingPersistentlyJobs = hashMapOf<UserId, ObservingJobs>()

    /**
     * Stops all the ObservingNotifications jobs that are currently running, for a specific User.
     */
    fun stopObservingOnLogout(userId: UserId) {
        stopObservingForUser(userId, observingWhileRunningJobs)
        stopObservingForUser(userId, observingPersistentlyJobs)
    }

    /**
     * Observes all the Message and Call notifications while app is running
     */
    suspend fun observeNotificationsAndCallsWhileRunning(
        userIds: List<UserId>,
        scope: CoroutineScope,
        doIfCallCameAndAppVisible: (Call) -> Unit
    ) = observeNotificationsAndCalls(userIds, scope, doIfCallCameAndAppVisible, observingWhileRunningJobs)

    /**
     * Observes all the Message and Call notifications persistently.
     * Use for Persistent WebSocket connection only.
     */
    suspend fun observeNotificationsAndCallsPersistently(
        userIds: List<UserId>,
        scope: CoroutineScope,
        doIfCallCameAndAppVisible: (Call) -> Unit
    ) = observeNotificationsAndCalls(userIds, scope, doIfCallCameAndAppVisible, observingPersistentlyJobs)

    /**
     * Become online, process all the Pending events,
     * and display notifications for new events.
     * Can be used in Services (e.g., after receiving FCM push).
     *
     * This function is synchronized and won't allow parallel
     * executions for the same [userIdValue].
     * Successive calls with the same [userIdValue]
     * will join the first execution and return together.
     * @param userIdValue String value param of QualifiedID of the User that need to check Notifications for
     */
    suspend fun fetchAndShowNotificationsOnce(userIdValue: String) {
        val userId = checkIfUserIsAuthenticated(userIdValue) ?: return

        val syncAndNotificationJobForUser = fetchAndShowMessageNotificationsJob(userId)

        // Join the jobs for the user, waiting for its completion
        syncAndNotificationJobForUser?.start()
        syncAndNotificationJobForUser?.join()
    }

    private suspend fun fetchAndShowMessageNotificationsJob(userId: UserId): Job? =
        fetchOnceMutex.withLock {
            // Use the lock to create a new coroutine if needed
            val currentJobForUser = fetchOnceJobs[userId]
            val isJobRunningForUser = currentJobForUser?.run {
                // Coroutine started, or didn't start yet, and it's waiting to be started
                isActive || !isCompleted
            } ?: false
            if (isJobRunningForUser) {
                // Return the currently existing job if it's active
                appLogger.d(
                    "$TAG Already processing notifications for user=${userId.value.obfuscateId()}, and joining original execution"
                )
                currentJobForUser
            } else {
                // Create a new job for this user
                appLogger.d("$TAG Starting to processing notifications for user=${userId.value.obfuscateId()}")
                val newJob = scope.launch(start = CoroutineStart.LAZY) {
                    triggerSyncForUserIfAuthenticated(userId)
                }
                fetchOnceJobs[userId] = newJob
                newJob
            }
        }

    private suspend fun triggerSyncForUserIfAuthenticated(userId: UserId) {
        appLogger.d("$TAG checking the notifications once")

        val observeMessagesJob = observeMessageNotificationsOnceJob(userId)
        val observeCallsJob = observeCallNotificationsOnceJob(userId)

        appLogger.d("$TAG start syncing")
        connectionPolicyManager.handleConnectionOnPushNotification(userId, STAY_ALIVE_TIME_ON_PUSH_MS)

        observeMessagesJob?.cancel("$TAG checked the notifications once, canceling observing.")
        observeCallsJob?.cancel("$TAG checked the calls once, canceling observing.")
    }

    private suspend fun observeMessageNotificationsOnceJob(userId: UserId): Job? {
        val isMessagesAlreadyObserving =
            observingWhileRunningJobs[userId]?.run { messagesJob.isActive }
                ?: observingPersistentlyJobs[userId]?.run { messagesJob.isActive }
                ?: false

        return if (isMessagesAlreadyObserving) {
            // notifications are already observed, just need to connect to websocket.
            appLogger.d("$TAG checking the notifications once, but notifications are already observed, no need to start a new job")
            null
        } else {
            scope.launch { observeMessageNotifications(userId, MutableStateFlow(CurrentScreen.InBackground)) }
        }
    }

    private suspend fun observeCallNotificationsOnceJob(userId: UserId): Job? {
        val isCallsAlreadyObserving =
            observingWhileRunningJobs[userId]?.run { incomingCallsJob.isActive }
                ?: observingPersistentlyJobs[userId]?.run { incomingCallsJob.isActive }
                ?: false

        return if (isCallsAlreadyObserving) {
            // calls are already observed, just need to connect to websocket.
            appLogger.d("$TAG checking the calls once, but calls are already observed, no need to start a new job")
            null
        } else {
            scope.launch { observeIncomingCalls(userId, MutableStateFlow(CurrentScreen.InBackground)) {} }
        }
    }

    /**
     * @return the userId if the user is authenticated on that device and null otherwise
     */
    @Suppress("NestedBlockDepth")
    private suspend fun checkIfUserIsAuthenticated(userId: String): QualifiedID? =
        coreLogic.globalScope { getSessions() }.let { sessionsResult ->
            when (sessionsResult) {
                is GetAllSessionsResult.Success ->
                    sessionsResult.sessions.firstOrNull { it.userId.value == userId }?.userId

                is GetAllSessionsResult.Failure.Generic -> {
                    appLogger.e("get sessions failed ${sessionsResult.genericFailure} ")
                    null
                }

                GetAllSessionsResult.Failure.NoSessionFound -> null
            }
        }

    /**
     * Infinitely listen for the new IncomingCalls, CurrentScreen changes and Message Notifications
     * Notify user and mark Conversations as notified when it's needed.
     * @param userIds List<QualifiedID> of Users that we need to observe notifications for
     * @param scope CoroutineScope used for observing CurrentScreen
     * @param doIfCallCameAndAppVisible action that should be done when incoming call comes and app is in foreground
     */
    private suspend fun observeNotificationsAndCalls(
        userIds: List<UserId>,
        scope: CoroutineScope,
        doIfCallCameAndAppVisible: (Call) -> Unit,
        observingJobs: HashMap<UserId, ObservingJobs>
    ) {
        val currentScreenState = currentScreenManager.observeCurrentScreen(scope)

        // removing notifications and stop observing it for the users that are not logged in anymore
        observingJobs.keys.filter { !userIds.contains(it) }
            .forEach { userId -> stopObservingForUser(userId, observingJobs) }

        if (userIds.isEmpty()) {
            // userIds.isEmpty() means there is no current user (logged out e.g.)
            // so we need to unsubscribe from the notification changes (done by canceling all the jobs above)
            // and remove the notifications that were displayed previously
            appLogger.i("$TAG no Users -> hide all the notifications")
            messagesNotificationManager.hideAllNotifications()
            callNotificationManager.hideAllNotifications()
            servicesManager.stopOngoingCallService()

            return
        }

        // start observing notifications only for new users
        userIds
            .filter { observingJobs[it]?.isAllActive() != true }
            .forEach { userId ->
                val jobs = ObservingJobs(
                    currentScreenJob = scope.launch(dispatcherProvider.default()) {
                        observeCurrentScreenAndHideNotifications(currentScreenState, userId)
                    },
                    incomingCallsJob = scope.launch(dispatcherProvider.default()) {
                        observeIncomingCalls(userId, currentScreenState, doIfCallCameAndAppVisible)
                    },
                    messagesJob = scope.launch(dispatcherProvider.default()) {
                        observeMessageNotifications(userId, currentScreenState)
                    },
                    ongoingCallJob = scope.launch(dispatcherProvider.default()) {
                        observeOngoingCalls(currentScreenState, userId)
                    }
                )
                observingJobs[userId] = jobs
            }
    }

    private fun stopObservingForUser(userId: UserId, observingJobs: HashMap<UserId, ObservingJobs>) {
        messagesNotificationManager.hideAllNotificationsForUser(userId)
        observingJobs[userId]?.cancelAll()
        observingJobs.remove(userId)
    }

    /**
     * Infinitely listen for the CurrentScreen changes and update Notifications if needed
     */
    private suspend fun observeCurrentScreenAndHideNotifications(
        currentScreenState: StateFlow<CurrentScreen>,
        userId: UserId
    ) {
        currentScreenState
            .collect { screens ->
                when (screens) {
                    is CurrentScreen.Conversation -> messagesNotificationManager.hideNotification(screens.id, userId)
                    is CurrentScreen.OtherUserProfile -> messagesNotificationManager.hideNotification(screens.id, userId)
                    is CurrentScreen.IncomingCallScreen -> callNotificationManager.hideIncomingCallNotification()
                    else -> {}
                }
            }
    }

    /**
     * Infinitely listen for the new IncomingCalls, notify about it and do additional actions if needed.
     * Can be used for listening for the Notifications when the app is running.
     * @param userId QualifiedID of User that we want to observe for
     * @param currentScreenState StateFlow that informs which screen is currently visible,
     * so we can decide: should we show notification, or run a @param[doIfCallCameAndAppVisible]
     */
    private suspend fun observeIncomingCalls(
        userId: UserId,
        currentScreenState: StateFlow<CurrentScreen>,
        doIfCallCameAndAppVisible: (Call) -> Unit
    ) {
        appLogger.d("$TAG observe incoming calls")

        coreLogic.getSessionScope(userId)
            .calls
            .getIncomingCalls()
            .collect { calls ->
                val isCurrentUserReceiver = coreLogic.getGlobalScope().session.currentSession().let {
                    (it is CurrentSessionResult.Success && it.accountInfo.userId == userId)
                }
                if (currentScreenState.value != CurrentScreen.InBackground && isCurrentUserReceiver) {
                    calls.firstOrNull()?.run {
                        appLogger.d("$TAG got some call while app is visible")
                        doIfCallCameAndAppVisible(this)
                    }
                    callNotificationManager.hideIncomingCallNotification()
                } else {
                    callNotificationManager.handleIncomingCallNotifications(calls, userId)
                }
            }
    }

    /**
     * Infinitely listen for the new Message notifications and show it.
     * Can be used for listening for the Notifications when the app is running.
     * @param userId QualifiedID of User that we want to observe for
     * @param currentScreenState StateFlow that informs which screen is currently visible,
     * so we can filter the notifications if user is in the Conversation that receives a new messages
     */
    private suspend fun observeMessageNotifications(
        userId: UserId,
        currentScreenState: StateFlow<CurrentScreen>
    ) {
        val selfUserNameState = coreLogic.getSessionScope(userId)
            .users
            .getSelfUser()
            .map { it.handle ?: it.name ?: "" }
            .distinctUntilChanged()
            .stateIn(scope)

        coreLogic.getSessionScope(userId)
            .messages
            .getNotifications()
            .cancellable()
            .map { newNotifications ->
                // we don't want to display notifications for the Conversation that user currently in.
                val notificationsList = filterAccordingToScreenAndUpdateNotifyDate(
                    currentScreenState.value,
                    userId,
                    newNotifications
                )

                playPingSoundIfNeeded(
                    currentScreen = currentScreenState.value,
                    notifications = newNotifications
                )
                val userName = selfUserNameState.value

                // combining all the data that is necessary for Notifications into small data class,
                // just to make it more readable than
                // Triple<List<LocalNotificationConversation>, QualifiedID, String>
                MessagesNotificationsData(notificationsList, userId, userName)
            }
            .cancellable()
            .collect { (newNotifications, userId, userName) ->
                appLogger.d("$TAG got ${newNotifications.size} notifications")
                messagesNotificationManager.handleNotification(newNotifications, userId, userName)
                markMessagesAsNotified(userId)
                markConnectionAsNotified(userId)
            }
    }

    /**
     * Infinitely listen for the established calls and run OngoingCall foreground Service
     * to show corresponding notification and do not lose a call.
     * @param userId QualifiedID of User that we want to observe for
     * @param currentScreenState StateFlow that informs which screen is currently visible,
     * so we can listen established calls only when the app is in background.
     */
    private suspend fun observeOngoingCalls(
        currentScreenState: StateFlow<CurrentScreen>,
        userId: UserId
    ) {
        currentScreenState
            .flatMapLatest { currentScreen ->
                if (currentScreen !is CurrentScreen.InBackground) {
                    flowOf(null)
                } else {
                    try {
                        coreLogic.getSessionScope(userId).calls
                            .establishedCall()
                            .map {
                                it.firstOrNull()?.let { call ->
                                    OngoingCallData(callNotificationManager.getNotificationTitle(call), call.conversationId, userId)
                                }
                            }
                    } catch (e: IllegalStateException) {
                        flowOf(null)
                    }
                }
            }
            .distinctUntilChanged()
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
        userId: UserId,
        newNotifications: List<LocalNotificationConversation>
    ) =
        if (currentScreen is CurrentScreen.Conversation) {
            markMessagesAsNotified(userId, currentScreen.id)
            newNotifications.filter { it.id != currentScreen.id }
        } else {
            newNotifications
        }

    private suspend fun markMessagesAsNotified(userId: QualifiedID, conversationId: ConversationId? = null) {
        val markNotified = conversationId?.let {
            MarkMessagesAsNotifiedUseCase.UpdateTarget.SingleConversation(conversationId)
        } ?: MarkMessagesAsNotifiedUseCase.UpdateTarget.AllConversations
        coreLogic.getSessionScope(userId)
            .messages
            .markMessagesAsNotified(markNotified)
    }

    private suspend fun markConnectionAsNotified(userId: QualifiedID?, connectionRequestUserId: QualifiedID? = null) {
        appLogger.d("$TAG markConnectionAsNotified")
        userId?.let {
            coreLogic.getSessionScope(it)
                .conversations
                .markConnectionRequestAsNotified(connectionRequestUserId)
        }
    }

    private fun playPingSoundIfNeeded(
        currentScreen: CurrentScreen,
        notifications: List<LocalNotificationConversation>
    ) {
        if (currentScreen is CurrentScreen.Conversation) {
            val conversationId = currentScreen.id
            val containsPingMessage = notifications
                .any {
                    it.id == conversationId && it.messages.any { message -> message is LocalNotificationMessage.Knock }
                }

            if (containsPingMessage) {
                pingRinger.ping(
                    resource = R.raw.ping_from_them,
                    isReceivingPing = true
                )
            }
        }
    }

    data class MessagesNotificationsData(
        val newNotifications: List<LocalNotificationConversation>,
        val userId: QualifiedID,
        val userName: String
    )

    private data class OngoingCallData(val notificationTitle: String, val conversationId: ConversationId, val userId: UserId)

    private data class ObservingJobs(
        val currentScreenJob: Job,
        val incomingCallsJob: Job,
        val messagesJob: Job,
        val ongoingCallJob: Job,
    ) {
        fun cancelAll() {
            currentScreenJob.cancel()
            incomingCallsJob.cancel()
            messagesJob.cancel()
            ongoingCallJob.cancel()
        }

        fun isAllActive(): Boolean =
            currentScreenJob.isActive && incomingCallsJob.isActive && messagesJob.isActive && ongoingCallJob.isActive
    }

    companion object {
        private const val TAG = "WireNotificationManager"
        private const val STAY_ALIVE_TIME_ON_PUSH_MS = 1000L
    }
}
