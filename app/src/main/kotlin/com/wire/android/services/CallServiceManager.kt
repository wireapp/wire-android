/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.services

import com.wire.android.appLogger
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.notification.CallNotificationData
import com.wire.android.services.CallService.Action
import com.wire.android.session.runWithValidSession
import com.wire.android.util.logging.logIfEmptyUserName
import com.wire.kalium.common.functional.Either
import com.wire.kalium.common.functional.left
import com.wire.kalium.common.functional.right
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.call.Call
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import dev.zacsweers.metro.Inject

class CallServiceManager @Inject constructor(@KaliumCoreLogic val coreLogic: CoreLogic) {
    private val actions = Channel<Action>(capacity = Channel.BUFFERED, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** Handles the action by adding it to the channel for processing. */
    internal suspend fun handleAction(action: Action) = actions.send(action)

    /**
     * Observes actions sent to the service and reacts accordingly.
     * If the action is [Action.Stop], it returns a reason for stopping the service.
     * If the action is [Action.Start], it collects the current session and its active calls and emits [CallNotificationData] to update
     * the foreground notification. If there are no calls or no current session, it returns a reason for stopping the service.
     */
    internal fun handleActionsFlow(): Flow<Either<StopReason, CallNotificationData>> = actions.receiveAsFlow()
        .flatMapLatest { action ->
            when (action) {
                is Action.Stop -> {
                    closeActiveCall()
                    flowOf(Either.Left(StopReason.ACTION_STOP_CALLED))
                }

                is Action.Start -> {
                    if (action is Action.Start.AnswerCall) answerCall(action)
                    startedServiceLifecycleFlow(action)
                }
            }
        }
        .distinctUntilChanged()
        .debounce { if (it is Either.Left) ServicesManager.DEBOUNCE_TIME else 0L }

    /** Answers the call for the given action if a valid session and incoming call exist for the userId, logging if it does not. */
    private suspend fun answerCall(action: Action.Start.AnswerCall) = coreLogic.runWithValidSession(action.userId) {
        val incomingCall = calls.getIncomingCalls().firstOrNull()?.firstOrNull {
            it.conversationId == action.conversationId
        }
        if (incomingCall != null) {
            calls.answerCall(action.conversationId)
        } else {
            appLogger.i(
                "$TAG: Cannot answer call, no incoming call for user ${action.userId.toLogString()}" +
                        " and conversation ${action.conversationId.toLogString()}"
            )
        }
    }

    /** Closes at most one snapshot of active calls, cancelling when the current account becomes inactive. */
    private suspend fun closeActiveCall() {
        val sessions = coreLogic.getGlobalScope().session.currentSessionFlow()
        val account = (sessions.firstOrNull() as? CurrentSessionResult.Success)?.accountInfo
        if (account?.isValid() != true) return
        var started = false
        sessions.flatMapLatest { result ->
            flow {
                if (!started && result is CurrentSessionResult.Success && result.accountInfo.isValid() &&
                    result.accountInfo.userId == account.userId
                ) {
                    coreLogic.runWithValidSession(account.userId) {
                        started = true
                        combine(calls.observeOutgoingCall(), calls.establishedCall()) { outgoing, established ->
                            outgoing + established
                        }.firstOrNull()?.forEach {
                            appLogger.i(
                                "$TAG: Ending call for user ${account.userId.toLogString()}" +
                                        " and conversation ${it.conversationId.toLogString()}"
                            )
                            calls.endCall(it.conversationId)
                        }
                    }
                }
                emit(Unit)
            }
        }.firstOrNull()
    }

    /**
     * Returns a flow representing the lifecycle of the service for the given start action.
     * Collects the current session and its active calls and emits [CallNotificationData] to update the foreground notification.
     * If there are no calls or no current session, it returns a reason for stopping the service.
     */
    private fun startedServiceLifecycleFlow(action: Action.Start): Flow<Either<StopReason, CallNotificationData>> =
        coreLogic.getGlobalScope().session.currentSessionFlow()
            .flatMapLatest {
                if (it is CurrentSessionResult.Success && it.accountInfo.isValid()) {
                    val userId = it.accountInfo.userId
                    val currentCalls = coreLogic.observeValidSession(userId) {
                        val userName = getUserName()
                        combine(
                            calls.establishedCall().mapToCallNotificationData(userId, userName),
                            calls.observeOutgoingCall().mapToCallNotificationData(userId, userName),
                        ) { established, outgoing -> established + outgoing }
                    }
                    combine(currentCalls, observeCallCurrentlyBeingAnswered(action)) { current, answering ->
                        val call = (current.orEmpty() + answering.orEmpty()).firstOrNull()
                        when {
                            call != null -> call.right()
                            current == null || answering == null -> null // Pending admission is not evidence of no calls.
                            else -> StopReason.NO_CALLS.left()
                        }
                    }.filterNotNull()
                } else {
                    flowOf(Either.Left(StopReason.NO_VALID_CURRENT_SESSION))
                }
            }

    /**
     * Observes the call that is currently being answered, if any. If the action is of type AnswerCall, it checks if a valid session exists
     * for the userId and returns the call data if it does, otherwise returns an empty list.
     */
    private suspend fun observeCallCurrentlyBeingAnswered(action: Action.Start): Flow<List<CallNotificationData>?> =
        if (action is Action.Start.AnswerCall) {
            coreLogic.getGlobalScope().observeAllValidSessionsFlow().map { result ->
                (result as? GetAllSessionsResult.Success)?.sessions.orEmpty().any { it.userId == action.userId && it.isValid() }
            }.distinctUntilChanged().flatMapLatest { valid ->
                if (valid) {
                    coreLogic.observeValidSession(action.userId) {
                        val userName = getUserName()
                        calls.getIncomingCalls()
                            .map { it.filter { it.conversationId == action.conversationId } }
                            .mapToCallNotificationData(action.userId, userName)
                    }
                } else {
                    flowOf(emptyList())
                }
            }
        } else {
            flowOf(emptyList())
        }

    /** Maps a flow of lists of calls to a flow of lists of CallNotificationData with userId and userName. */
    private fun Flow<List<Call>>.mapToCallNotificationData(userId: UserId, userName: String) = this.map { listOfCalls ->
        listOfCalls.map { call -> CallNotificationData(userId, call, userName) }
    }

    /** Retrieves the user name from the session scope, logging if the user name is empty. */
    private suspend fun UserSessionScope.getUserName() = users.observeSelfUser().first()
        .also { it.logIfEmptyUserName() }
        .let { it.handle ?: it.name ?: "" }

    enum class StopReason(val message: String) {
        NO_CALLS("no calls"),
        NO_VALID_CURRENT_SESSION("no valid current session"),
        ACTION_STOP_CALLED("stopService was called")
    }

    companion object {
        private const val TAG = "CallServiceManager"
    }
}
