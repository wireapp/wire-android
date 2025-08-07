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
import com.wire.android.util.logIfEmptyUserName
import com.wire.kalium.common.functional.Either
import com.wire.kalium.common.functional.fold
import com.wire.kalium.common.functional.left
import com.wire.kalium.common.functional.right
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.call.Call
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.DoesValidSessionExistResult
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

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
                is Action.Stop -> flowOf(Either.Left(StopReason.ACTION_STOP_CALLED))
                is Action.Start -> {
                    if (action is Action.Start.AnswerCall) answerCall(action)
                    startedServiceLifecycleFlow(action)
                }
            }
        }
        .distinctUntilChanged()
        .debounce { if (it is Either.Left) ServicesManager.DEBOUNCE_TIME else 0L }

    /** Answers the call for the given action if a valid session and incoming call exist for the userId, logging if it does not. */
    private suspend fun answerCall(action: Action.Start.AnswerCall) = getValidSessionIfExists(action.userId).fold({
        appLogger.i("$TAG: Cannot answer call, no valid session for user ${action.userId.toLogString()}")
    }, { userSessionScope ->
        val incomingCall = userSessionScope.calls.getIncomingCalls().firstOrNull()?.firstOrNull {
            it.conversationId == action.conversationId
        }
        if (incomingCall != null) {
            userSessionScope.calls.answerCall(action.conversationId)
        } else {
            appLogger.i(
                "$TAG: Cannot answer call, no incoming call for user ${action.userId.toLogString()}" +
                        " and conversation ${action.conversationId.toLogString()}"
            )
        }
    })

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
                    val userSessionScope = coreLogic.getSessionScope(userId)
                    val userName = userSessionScope.getUserName()
                    combine(
                        userSessionScope.calls.establishedCall().mapToCallNotificationData(userId, userName),
                        userSessionScope.calls.observeOutgoingCall().mapToCallNotificationData(userId, userName),
                        observeCallCurrentlyBeingAnswered(action),
                    ) { establishedCalls, outgoingCalls, answeringCall ->
                        (establishedCalls + outgoingCalls + answeringCall).firstOrNull()?.right() ?: StopReason.NO_CALLS.left()
                    }
                } else {
                    flowOf(Either.Left(StopReason.NO_VALID_CURRENT_SESSION))
                }
            }

    /**
     * Observes the call that is currently being answered, if any. If the action is of type AnswerCall, it checks if a valid session exists
     * for the userId and returns the call data if it does, otherwise returns an empty list.
     */
    private suspend fun observeCallCurrentlyBeingAnswered(action: Action.Start) =
        if (action is Action.Start.AnswerCall) {
            getValidSessionIfExists(action.userId).fold({
                flowOf(emptyList())
            }, { userSessionScope ->
                val userName = userSessionScope.getUserName()
                userSessionScope.calls.getIncomingCalls()
                    .map { it.filter { it.conversationId == action.conversationId } }
                    .mapToCallNotificationData(action.userId, userName)
            })
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

    /** Checks if a valid session exists for the given userId and returns the UserSessionScope if it does. */
    private suspend fun getValidSessionIfExists(userId: UserId): Either<Unit, UserSessionScope> =
        coreLogic.getGlobalScope().doesValidSessionExist(userId).let { result ->
            if (result is DoesValidSessionExistResult.Success && result.doesValidSessionExist) {
                coreLogic.getSessionScope(userId).right()
            } else {
                Unit.left()
            }
        }

    enum class StopReason(val message: String) {
        NO_CALLS("no calls"),
        NO_VALID_CURRENT_SESSION("no valid current session"),
        ACTION_STOP_CALLED("stopService was called")
    }

    companion object {
        private const val TAG = "CallServiceManager"
    }
}
