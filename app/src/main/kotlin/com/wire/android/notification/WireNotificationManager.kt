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

package com.wire.android.notification

import androidx.annotation.VisibleForTesting
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.media.PingRinger
import com.wire.android.services.ServicesManager
import com.wire.android.util.CurrentScreen
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.lifecycle.SyncLifecycleManager
import com.wire.android.util.logging.logIfEmptyUserName
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.notification.LocalNotification
import com.wire.kalium.logic.data.notification.LocalNotificationMessage
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.message.MarkMessagesAsNotifiedUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.DoesValidSessionExistResult
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.logic.feature.user.E2EIRequiredResult
import com.wire.kalium.logic.sync.SyncRequestResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import java.net.SocketException
import java.net.UnknownHostException
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.SingleIn
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("TooManyFunctions", "LongParameterList")
@SingleIn(AppScope::class)
class WireNotificationManager @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val currentScreenManager: CurrentScreenManager,
    private val messagesNotificationManager: MessageNotificationManager,
    private val callNotificationManager: CallNotificationManager,
    private val syncLifecycleManager: SyncLifecycleManager,
    private val servicesManager: ServicesManager,
    private val dispatcherProvider: DispatcherProvider,
    private val pingRinger: PingRinger
) {

    private val fetchOnceMutex = Mutex()
    private val fetchOnceJobs = hashMapOf<UserId, Deferred<NotificationFetchResult>>()
    private val observerLock = Any()
    private val observerScope = CoroutineScope(SupervisorJob() + dispatcherProvider.default())
    private val observerOwners = hashMapOf<ObserverOwner, Set<UserId>>()
    private val observerOwnerCleanupRegistered = hashSetOf<ObserverOwner>()
    private val userObservers = hashMapOf<UserId, UserObservingJobs>()
    private var outgoingOngoingCallJob: Job? = null
    private val currentScreenState = observerScope.async(start = CoroutineStart.LAZY) {
        currentScreenManager.observeCurrentScreen(observerScope)
    }

    /**
     * Stops all the ObservingNotifications jobs that are currently running, for a specific User.
     */
    fun stopObservingOnLogout(userId: UserId) {
        synchronized(observerLock) {
            observerOwners.entries.forEach { entry -> entry.setValue(entry.value - userId) }
            observerOwners.entries.removeAll { it.value.isEmpty() }
            userObservers.remove(userId)?.cancelAll()
            stopOutgoingOngoingCallObserverIfUnused()
        }
        messagesNotificationManager.hideAllNotificationsForUser(userId)
        callNotificationManager.hideAllIncomingCallNotificationsForUser(userId)
    }

    /**
     * Observes all the Message and Call notifications while app is running
     */
    suspend fun observeNotificationsAndCallsWhileRunning(
        userIds: List<UserId>,
        scope: CoroutineScope
    ) = observeNotificationsAndCalls(userIds, scope, ObserverOwnerType.WHILE_RUNNING)

    /**
     * Observes all the Message and Call notifications persistently.
     * Use for Persistent WebSocket connection only.
     */
    suspend fun observeNotificationsAndCallsPersistently(
        userIds: List<UserId>,
        scope: CoroutineScope
    ) = observeNotificationsAndCalls(userIds, scope, ObserverOwnerType.PERSISTENT)

    /**
     * When there are no valid users with active sessions, we want to stop all the notifications and calls observing jobs,
     * hide all the notifications and stop the call service, because they are not relevant anymore.
     */
    fun clearWhenNoUsers() {
        synchronized(observerLock) {
            observerOwners.clear()
            observerOwnerCleanupRegistered.clear()
            userObservers.values.forEach { it.cancelAll() }
            userObservers.clear()
            outgoingOngoingCallJob?.cancel()
            outgoingOngoingCallJob = null
        }
        messagesNotificationManager.hideAllNotifications()
        callNotificationManager.hideAllCallNotifications()
        servicesManager.stopCallService()
    }

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
    internal suspend fun fetchAndShowNotificationsOnce(userIdValue: String): NotificationFetchResult = coroutineScope {
        val userId = checkIfUserIsAuthenticated(userIdValue) ?: return@coroutineScope NotificationFetchResult.Success

        val syncAndNotificationJobForUser = fetchOnceMutex.withLock {
            fetchOnceJobs[userId]?.takeUnless { it.isCompleted }?.also {
                appLogger.d(
                    "$TAG Already processing notifications for user=${userId.toLogString()}, and joining original execution"
                )
            } ?: async(start = CoroutineStart.LAZY) {
                appLogger.d("$TAG Starting to process notifications for user=${userId.toLogString()}")
                triggerSyncForUserIfAuthenticated(userId)
            }.also { fetchOnceJobs[userId] = it }
        }

        try {
            syncAndNotificationJobForUser.start()
            syncAndNotificationJobForUser.await()
        } finally {
            fetchOnceMutex.withLock {
                if (fetchOnceJobs[userId] === syncAndNotificationJobForUser && syncAndNotificationJobForUser.isCompleted) {
                    fetchOnceJobs.remove(userId)
                }
            }
        }
    }

    private suspend fun triggerSyncForUserIfAuthenticated(userId: UserId): NotificationFetchResult = coroutineScope {
        appLogger.d("$TAG checking the notifications once")
        val owner = ObserverOwner(ObserverOwnerType.TEMPORARY, coroutineContext[Job])
        updateObserverOwner(owner, setOf(userId), registerScopeCleanup = false)

        val stayAliveDuration = BuildConfig.BACKGROUND_NOTIFICATION_STAY_ALIVE_SECONDS.seconds

        try {
            if (BuildConfig.BACKGROUND_NOTIFICATION_RETRY_ENABLED) {
                appLogger.d("$TAG start syncing with retry logic and extended duration (${stayAliveDuration.inWholeSeconds}s)")
                retrySync(userId, stayAliveDuration)
            } else {
                appLogger.d("$TAG start syncing without retry logic, default duration (${stayAliveDuration.inWholeSeconds}s)")
                syncLifecycleManager.syncTemporarily(userId, stayAliveDuration).toNotificationFetchResult()
            }
        } finally {
            withContext(NonCancellable) {
                updateObserverOwner(owner, emptySet(), registerScopeCleanup = false)
            }
        }
    }

    /**
     * Retries sync operation with exponential backoff for transient network failures.
     * This is critical for background notifications during Doze mode where network
     * may not be immediately available despite WorkManager constraints.
     */
    private suspend fun retrySync(userId: UserId, stayAliveDuration: Duration): NotificationFetchResult {
        var attempt = 1
        var result: NotificationFetchResult
        do {
            appLogger.d("$TAG Sync attempt $attempt/$MAX_SYNC_RETRY")
            result = syncOnce(userId, stayAliveDuration)
            if (result == NotificationFetchResult.Retry && attempt < MAX_SYNC_RETRY) {
                val delaySeconds = 1L shl (attempt - 1)
                appLogger.w("$TAG Retrying notification sync in ${delaySeconds}s")
                delay(delaySeconds.seconds)
            }
            attempt++
        } while (result == NotificationFetchResult.Retry && attempt <= MAX_SYNC_RETRY)

        when (result) {
            NotificationFetchResult.Success -> appLogger.i("$TAG Sync succeeded on attempt ${attempt - 1}")
            NotificationFetchResult.Retry ->
                appLogger.e("$TAG All $MAX_SYNC_RETRY sync attempts failed; requesting WorkManager retry")
            NotificationFetchResult.Failure -> appLogger.w("$TAG Non-retryable notification sync failure")
        }
        return result
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun syncOnce(userId: UserId, stayAliveDuration: Duration): NotificationFetchResult = try {
        syncLifecycleManager.syncTemporarily(userId, stayAliveDuration).toNotificationFetchResult()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        if (e.isRetryableNetworkException()) {
            appLogger.w("$TAG Retryable network error during notification sync: ${e.message}")
            NotificationFetchResult.Retry
        } else {
            appLogger.w("$TAG Non-retryable error during notification sync: ${e.message}")
            throw e
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
     */
    private suspend fun observeNotificationsAndCalls(
        userIds: List<UserId>,
        scope: CoroutineScope,
        ownerType: ObserverOwnerType
    ) {
        val owner = ObserverOwner(ownerType, scope.coroutineContext[Job])
        val currentlyOwnedUsers = synchronized(observerLock) { observerOwners[owner].orEmpty() }
        val validNewUsers = newUsersWithValidSessionAndWithoutActiveJobs(userIds) { it in currentlyOwnedUsers }
        val retainedUsers = userIds.filter { it in currentlyOwnedUsers }
        updateObserverOwner(owner, (retainedUsers + validNewUsers).toSet())
    }

    private fun updateObserverOwner(
        owner: ObserverOwner,
        userIds: Set<UserId>,
        registerScopeCleanup: Boolean = true
    ) {
        var shouldRegisterScopeCleanup = false
        synchronized(observerLock) {
            val previousUserIds = observerOwners[owner].orEmpty()
            (previousUserIds - userIds).forEach { userId -> removeOwnerFromUserObserver(owner, userId) }
            (userIds - previousUserIds).forEach { userId -> addOwnerToUserObserver(owner, userId) }
            (userIds intersect previousUserIds).forEach { userId -> ensureUserObserverActive(userId) }

            if (userIds.isEmpty()) {
                observerOwners.remove(owner)
                observerOwnerCleanupRegistered.remove(owner)
            } else {
                observerOwners[owner] = userIds
                shouldRegisterScopeCleanup = registerScopeCleanup && observerOwnerCleanupRegistered.add(owner)
            }
            if (userObservers.isEmpty()) {
                stopOutgoingOngoingCallObserverIfUnused()
            } else {
                startOutgoingOngoingCallObserverIfNeeded()
            }
        }

        if (shouldRegisterScopeCleanup) {
            owner.lifecycleJob?.invokeOnCompletion {
                updateObserverOwner(owner, emptySet(), registerScopeCleanup = false)
            }
        }
    }

    private fun addOwnerToUserObserver(owner: ObserverOwner, userId: UserId) {
        ensureUserObserverActive(userId).owners.add(owner)
    }

    private fun ensureUserObserverActive(userId: UserId): UserObservingJobs {
        val existingObserver = userObservers[userId]
        return if (existingObserver == null || !existingObserver.isAllActive()) {
            val existingOwners = existingObserver?.owners.orEmpty()
            existingObserver?.cancelAll()
            startUserObserver(userId).also {
                it.owners.addAll(existingOwners)
                userObservers[userId] = it
            }
        } else {
            existingObserver
        }
    }

    private fun removeOwnerFromUserObserver(owner: ObserverOwner, userId: UserId) {
        userObservers[userId]?.let { observer ->
            observer.owners.remove(owner)
            if (observer.owners.isEmpty()) {
                observer.cancelAll()
                userObservers.remove(userId)
            }
        }
    }

    private fun startUserObserver(userId: UserId): UserObservingJobs = UserObservingJobs(
        currentScreenJob = observerScope.launch {
            observeCurrentScreenAndHideNotifications(currentScreenState.await(), userId)
        },
        incomingCallsJob = observerScope.launch {
            observeIncomingCalls(userId)
        },
        messagesJob = observerScope.launch {
            observeMessageNotifications(userId, currentScreenState.await())
        }
    )

    private fun startOutgoingOngoingCallObserverIfNeeded() {
        if (outgoingOngoingCallJob?.isActive != true) {
            outgoingOngoingCallJob = observerScope.launch { observeOutgoingOngoingCalls() }
        }
    }

    private fun stopOutgoingOngoingCallObserverIfUnused() {
        if (userObservers.isEmpty()) {
            outgoingOngoingCallJob?.cancel()
            outgoingOngoingCallJob = null
        }
    }

    @VisibleForTesting
    internal suspend fun newUsersWithValidSessionAndWithoutActiveJobs(
        userIds: List<UserId>,
        hasActiveJobs: (UserId) -> Boolean
    ): List<UserId> = userIds
        .filter { !hasActiveJobs(it) }
        .filter {
            // double check if the valid session for the given user still exists
            when (val result = coreLogic.getGlobalScope().doesValidSessionExist(it)) {
                is DoesValidSessionExistResult.Success -> result.doesValidSessionExist
                else -> false
            }
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
                    is CurrentScreen.Conversation -> messagesNotificationManager.hideNotification(
                        conversationsId = screens.id,
                        userId = userId
                    )

                    is CurrentScreen.OtherUserProfile -> screens.groupConversationId?.let {
                        messagesNotificationManager.hideNotification(
                            conversationsId = screens.groupConversationId,
                            userId = userId
                        )
                    }

                    else -> {}
                }
            }
    }

    /**
     * Infinitely listen for the new IncomingCalls, notify about it and do additional actions if needed.
     * Can be used for listening for the Notifications when the app is running.
     * @param userId QualifiedID of User that we want to observe for
     */
    private suspend fun observeIncomingCalls(
        userId: UserId
    ) {
        appLogger.d("$TAG observe incoming calls")

        coreLogic.getSessionScope(userId).let { userSessionScope ->
            userSessionScope.observeE2EIRequired()
                .map { it is E2EIRequiredResult.NoGracePeriod }
                .distinctUntilChanged()
                .flatMapLatest { isBlockedByE2EIRequired ->
                    if (isBlockedByE2EIRequired) {
                        appLogger.d("$TAG calls were blocked as E2EI is required")
                        flowOf(listOf())
                    } else {
                        userSessionScope.calls.getIncomingCalls()
                    }.map { calls ->
                        userSessionScope.users.observeSelfUser().first()
                            .also { it.logIfEmptyUserName() }
                            .let { it.handle ?: it.name ?: "" } to calls
                    }
                }
                .collect { (userName, calls) ->
                    callNotificationManager.handleIncomingCalls(calls, userId, userName)
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
    ) = coroutineScope {
        val selfUserNameState = coreLogic.getSessionScope(userId)
            .users
            .observeSelfUser()
            .onEach { it.logIfEmptyUserName() }
            .map { it.handle ?: it.name ?: "" }
            .distinctUntilChanged()
            .stateIn(this)

        val isBlockedByE2EIRequiredState = coreLogic.getSessionScope(userId).observeE2EIRequired()
            .map { it is E2EIRequiredResult.NoGracePeriod }
            .distinctUntilChanged()
            .stateIn(this)

        coreLogic.getSessionScope(userId)
            .messages
            .getNotifications()
            .cancellable()
            .onEach { newNotifications ->
                playPingSoundIfNeeded(
                    currentScreen = currentScreenState.value,
                    notifications = newNotifications
                )
            }
            .map { newNotifications ->
                // we don't want to display notifications for the Conversation that user currently in.
                filterAccordingToScreenAndUpdateNotifyDate(
                    currentScreenState.value,
                    userId,
                    newNotifications
                )
            }
            .cancellable()
            .collect { newNotifications ->
                appLogger.d("$TAG got ${newNotifications.size} notifications")
                if (isBlockedByE2EIRequiredState.value) {
                    appLogger.d("$TAG notifications were skipped as E2EI is required")
                } else {
                    messagesNotificationManager.handleNotification(
                        newNotifications,
                        userId,
                        selfUserNameState.value
                    )
                }

                newNotifications
                    .filterIsInstance<LocalNotification.Conversation>()
                    .filter { it.messages.isNotEmpty() }
                    .forEach { conversation ->
                        val lastNotified = conversation.messages.maxOf { it.time }
                        markMessagesAsNotified(userId, conversation.id, lastNotified)
                    }

                markConnectionAsNotified(userId)
            }
    }

    /**
     * Infinitely listen for outgoing and established calls of a current user and run the call on foreground Service to show corresponding
     * notification and do not lose a call. The call service handles stopping itself when there are no calls so here only care about
     * starting the service when there are calls.
     */
    private suspend fun observeOutgoingOngoingCalls() {
        coreLogic.getGlobalScope().session.currentSessionFlow()
            .flatMapLatest {
                if (it is CurrentSessionResult.Success && it.accountInfo.isValid()) {
                    val sessionScope = coreLogic.getSessionScope(it.accountInfo.userId)
                    // wait for the initial cleanup of stale open calls to be completed before starting to observe the calls,
                    // to avoid starting the service by mistake for the calls that are already stale
                    sessionScope.calls.observeStaleOpenCallsCleanup()
                        .dropWhile { completed -> !completed }
                        .flatMapLatest {
                            combine(
                                sessionScope.calls.establishedCall(),
                                sessionScope.calls.observeOutgoingCall()
                            ) { establishedCalls, outgoingCalls -> (establishedCalls + outgoingCalls).isNotEmpty() }
                        }
                } else {
                    flowOf(null)
                }
            }
            .distinctUntilChanged()
            .filter { it == true }
            .collect {
                servicesManager.startCallService()
            }
    }

    private suspend fun filterAccordingToScreenAndUpdateNotifyDate(
        currentScreen: CurrentScreen,
        userId: UserId,
        newNotifications: List<LocalNotification>
    ) =
        if (currentScreen is CurrentScreen.Conversation) {
            markMessagesAsNotified(userId, currentScreen.id)
            newNotifications.filter { it.conversationId != currentScreen.id }
        } else {
            newNotifications
        }

    private suspend fun markMessagesAsNotified(
        userId: QualifiedID,
        conversationId: ConversationId? = null,
        lastNotified: Instant? = null,
    ) {
        val markNotified = conversationId?.let {
            MarkMessagesAsNotifiedUseCase.UpdateTarget.SingleConversation(conversationId, lastNotified)
        } ?: MarkMessagesAsNotifiedUseCase.UpdateTarget.AllConversations
        coreLogic.getSessionScope(userId)
            .messages
            .markMessagesAsNotified(markNotified)
    }

    private suspend fun markConnectionAsNotified(
        userId: QualifiedID?,
        connectionRequestUserId: QualifiedID? = null
    ) {
        appLogger.d("$TAG markConnectionAsNotified")
        userId?.let {
            coreLogic.getSessionScope(it)
                .conversations
                .markConnectionRequestAsNotified(connectionRequestUserId)
        }
    }

    private fun playPingSoundIfNeeded(
        currentScreen: CurrentScreen,
        notifications: List<LocalNotification>
    ) {
        if (currentScreen is CurrentScreen.Conversation) {
            val conversationId = currentScreen.id
            val containsPingMessage = notifications
                .any {
                    it.conversationId == conversationId &&
                            it is LocalNotification.Conversation &&
                            it.messages.any { message ->
                                message is LocalNotificationMessage.Knock ||
                                        message is LocalNotificationMessage.SelfDeleteKnock
                            }
                }

            if (containsPingMessage) {
                pingRinger.ping(
                    resource = R.raw.ping_from_them,
                    isReceivingPing = true
                )
            }
        }
    }

    private data class UserObservingJobs(
        val currentScreenJob: Job,
        val incomingCallsJob: Job,
        val messagesJob: Job,
        val owners: MutableSet<ObserverOwner> = mutableSetOf()
    ) {
        fun cancelAll() {
            currentScreenJob.cancel()
            incomingCallsJob.cancel()
            messagesJob.cancel()
        }

        fun isAllActive(): Boolean =
            currentScreenJob.isActive && incomingCallsJob.isActive && messagesJob.isActive
    }

    private data class ObserverOwner(
        val type: ObserverOwnerType,
        val lifecycleJob: Job?
    )

    private enum class ObserverOwnerType {
        WHILE_RUNNING,
        PERSISTENT,
        TEMPORARY
    }

    companion object {
        private const val TAG = "WireNotificationManager"
        private const val MAX_SYNC_RETRY = 3
    }
}

internal sealed interface NotificationFetchResult {
    data object Success : NotificationFetchResult
    data object Retry : NotificationFetchResult
    data object Failure : NotificationFetchResult
}

private fun SyncRequestResult.toNotificationFetchResult(): NotificationFetchResult = when (this) {
    SyncRequestResult.Success -> NotificationFetchResult.Success
    is SyncRequestResult.Failure -> if (error.isRetryableNotificationSyncFailure()) {
        NotificationFetchResult.Retry
    } else {
        NotificationFetchResult.Failure
    }
}

private fun CoreFailure.isRetryableNotificationSyncFailure(): Boolean = when (this) {
    is NetworkFailure.NoNetworkConnection -> true
    is CoreFailure.Unknown -> rootCause.isRetryableNetworkException()
    else -> false
}

private fun Throwable?.isRetryableNetworkException(): Boolean {
    var current = this
    while (current != null) {
        if (current is UnknownHostException || current is SocketException) return true
        current = current.cause
    }
    return false
}
