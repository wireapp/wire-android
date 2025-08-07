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

import app.cash.turbine.test
import com.wire.android.assertIs
import com.wire.android.framework.TestConversation
import com.wire.android.framework.TestUser
import com.wire.android.notification.CallNotificationData
import com.wire.kalium.common.functional.Either
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.call.Call
import com.wire.kalium.logic.data.call.CallStatus
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.call.usecase.AnswerCallUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.DoesValidSessionExistResult
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CallServiceManagerTest {

    @Test
    fun `given action stop, when handling, then emit proper stop`() = runTest {
        val (_, callServiceManager) = Arrangement().arrange()
        callServiceManager.handleAction(CallService.Action.Stop)
        callServiceManager.handleActionsFlow().test {
            assertIsStop(awaitItem()).also {
                assertEquals(CallServiceManager.StopReason.ACTION_STOP_CALLED, it)
            }
        }
    }

    @Test
    fun `given action start & no current session, when handling, then emit proper stop`() = runTest {
        val (_, callServiceManager) = Arrangement()
            .withCurrentSession(flowOf(CurrentSessionResult.Failure.SessionNotFound))
            .arrange()
        callServiceManager.handleAction(CallService.Action.Start.Default)
        callServiceManager.handleActionsFlow().test {
            assertIsStop(awaitItem()).also {
                assertEquals(CallServiceManager.StopReason.NO_VALID_CURRENT_SESSION, it)
            }
        }
    }

    @Test
    fun `given action start & no calls, when handling, then emit proper stop`() = runTest {
        val (_, callServiceManager) = Arrangement()
            .withCurrentSession(flowOf(CurrentSessionResult.Success(AccountInfo.Valid(selfUser.id))))
            .arrange()
        callServiceManager.handleAction(CallService.Action.Start.Default)
        callServiceManager.handleActionsFlow().test {
            assertIsStop(awaitItem()).also {
                assertEquals(CallServiceManager.StopReason.NO_CALLS, it)
            }
        }
    }

    @Test
    fun `given action start & established call, when handling, then emit proper call data`() = runTest {
        val establishedCall = call.copy(status = CallStatus.ESTABLISHED)
        val (_, callServiceManager) = Arrangement()
            .withCurrentSession(flowOf(CurrentSessionResult.Success(AccountInfo.Valid(selfUser.id))))
            .withSpecificUserSession(selfUser = selfUser, established = flowOf(listOf(establishedCall)))
            .arrange()
        callServiceManager.handleAction(CallService.Action.Start.Default)
        callServiceManager.handleActionsFlow().test {
            assertIsUpdate(awaitItem()).also {
                assertEquals(CallNotificationData(selfUser.id, establishedCall, selfUser.name()), it)
            }
        }
    }

    @Test
    fun `given action start & established call, when handling & call becomes closed, then emit proper stop`() = runTest {
        val establishedCall = call.copy(status = CallStatus.ESTABLISHED)
        val establishedCallsFlow = MutableStateFlow(listOf(establishedCall))
        val (_, callServiceManager) = Arrangement()
            .withCurrentSession(flowOf(CurrentSessionResult.Success(AccountInfo.Valid(selfUser.id))))
            .withSpecificUserSession(selfUser = selfUser, established = establishedCallsFlow)
            .arrange()
        callServiceManager.handleAction(CallService.Action.Start.Default)
        callServiceManager.handleActionsFlow().test {
            assertIsUpdate(awaitItem()).also {
                assertEquals(CallNotificationData(selfUser.id, establishedCall, selfUser.name()), it)
            }
            establishedCallsFlow.emit(emptyList())
            assertIsStop(awaitItem()).also {
                assertEquals(CallServiceManager.StopReason.NO_CALLS, it)
            }
        }
    }

    @Test
    fun `given action start & answered call, when handling & call becomes established, then emit changed call data`() = runTest {
        val answeredCall = call.copy(status = CallStatus.ANSWERED)
        val establishedCall = call.copy(status = CallStatus.ESTABLISHED)
        val establishedCallsFlow = MutableStateFlow(listOf(answeredCall))
        val (_, callServiceManager) = Arrangement()
            .withCurrentSession(flowOf(CurrentSessionResult.Success(AccountInfo.Valid(selfUser.id))))
            .withSpecificUserSession(selfUser = selfUser, established = establishedCallsFlow)
            .arrange()
        callServiceManager.handleAction(CallService.Action.Start.Default)
        callServiceManager.handleActionsFlow().test {
            assertIsUpdate(awaitItem()).also {
                assertEquals(CallNotificationData(selfUser.id, answeredCall, selfUser.name()), it)
            }
            establishedCallsFlow.emit(listOf(establishedCall))
            assertIsUpdate(awaitItem()).also {
                assertEquals(CallNotificationData(selfUser.id, establishedCall, selfUser.name()), it)
            }
        }
    }

    @Test
    fun `given action start & active call for current session, when handling & session logs out, then emit proper stop`() = runTest {
        val establishedCall = call.copy(status = CallStatus.ESTABLISHED)
        val currentSessionFlow = MutableStateFlow<CurrentSessionResult>(CurrentSessionResult.Success(AccountInfo.Valid(selfUser.id)))
        val (_, callServiceManager) = Arrangement()
            .withCurrentSession(currentSessionFlow)
            .withSpecificUserSession(selfUser = selfUser, established = flowOf(listOf(establishedCall)))
            .arrange()
        callServiceManager.handleAction(CallService.Action.Start.Default)
        callServiceManager.handleActionsFlow().test {
            assertIsUpdate(awaitItem()).also {
                assertEquals(CallNotificationData(selfUser.id, establishedCall, selfUser.name()), it)
            }
            currentSessionFlow.emit(CurrentSessionResult.Success(AccountInfo.Invalid(selfUser.id, LogoutReason.SELF_SOFT_LOGOUT)))
            assertIsStop(awaitItem()).also {
                assertEquals(CallServiceManager.StopReason.NO_VALID_CURRENT_SESSION, it)
            }
        }
    }

    @Test
    fun `given action start & active call only for current session, when handling & session changes, then emit proper stop`() = runTest {
        val otherSelfUser = selfUser.copy(id = selfUser.id.copy("other"))
        val establishedCall = call.copy(status = CallStatus.ESTABLISHED)
        val currentSessionFlow = MutableStateFlow<CurrentSessionResult>(CurrentSessionResult.Success(AccountInfo.Valid(selfUser.id)))
        val (_, callServiceManager) = Arrangement()
            .withCurrentSession(currentSessionFlow)
            .withSpecificUserSession(selfUser = selfUser, established = flowOf(listOf(establishedCall)))
            .withSpecificUserSession(selfUser = otherSelfUser, established = flowOf(emptyList()))
            .arrange()
        callServiceManager.handleAction(CallService.Action.Start.Default)
        callServiceManager.handleActionsFlow().test {
            assertIsUpdate(awaitItem()).also {
                assertEquals(CallNotificationData(selfUser.id, establishedCall, selfUser.name()), it)
            }
            currentSessionFlow.emit(CurrentSessionResult.Success(AccountInfo.Valid(otherSelfUser.id)))
            assertIsStop(awaitItem()).also {
                assertEquals(CallServiceManager.StopReason.NO_CALLS, it)
            }
        }
    }

    @Test
    fun `given action answer & no such incoming call, when handling, then emit proper stop reason`() = runTest {
        val otherIncomingCall = call.copy(status = CallStatus.INCOMING, conversationId = conversationId.copy(value = "other"))
        val (_, callServiceManager) = Arrangement()
            .withCurrentSession(flowOf(CurrentSessionResult.Success(AccountInfo.Valid(selfUser.id))))
            .withSpecificUserSession(selfUser = selfUser, incoming = flowOf(listOf(otherIncomingCall)))
            .arrange()
        callServiceManager.handleAction(CallService.Action.Start.AnswerCall(selfUser.id, conversationId))
        callServiceManager.handleActionsFlow().test {
            assertIsStop(awaitItem()).also {
                assertEquals(CallServiceManager.StopReason.NO_CALLS, it)
            }
        }
    }

    @Test
    fun `given action answer & incoming call for current session, when handling, then answer & emit proper call data`() = runTest {
        val incomingCall = call.copy(status = CallStatus.INCOMING)
        val (arrangement, callServiceManager) = Arrangement()
            .withCurrentSession(flowOf(CurrentSessionResult.Success(AccountInfo.Valid(selfUser.id))))
            .withSpecificUserSession(selfUser = selfUser, incoming = flowOf(listOf(incomingCall)))
            .arrange()
        callServiceManager.handleAction(CallService.Action.Start.AnswerCall(selfUser.id, conversationId))
        callServiceManager.handleActionsFlow().test {
            assertIsUpdate(awaitItem()).also {
                assertEquals(CallNotificationData(selfUser.id, incomingCall, selfUser.name()), it)
            }
        }
        coVerify(exactly = 1) { arrangement.answerCallForUser(selfUser.id)(conversationId) }
    }

    @Test
    fun `given action answer & incoming call for other valid session, when handling, then answer & emit proper call data`() = runTest {
        val otherSelfUser = selfUser.copy(id = selfUser.id.copy("other"))
        val incomingCall = call.copy(status = CallStatus.INCOMING)
        val (arrangement, callServiceManager) = Arrangement()
            .withCurrentSession(flowOf(CurrentSessionResult.Success(AccountInfo.Valid(selfUser.id))))
            .withSpecificUserSession(selfUser = selfUser, incoming = flowOf(emptyList()))
            .withSpecificUserSession(selfUser = otherSelfUser, incoming = flowOf(listOf(incomingCall)))
            .arrange()
        callServiceManager.handleAction(CallService.Action.Start.AnswerCall(otherSelfUser.id, conversationId))
        callServiceManager.handleActionsFlow().test {
            assertIsUpdate(awaitItem()).also {
                assertEquals(CallNotificationData(otherSelfUser.id, incomingCall, otherSelfUser.name()), it)
            }
        }
        coVerify(exactly = 1) { arrangement.answerCallForUser(otherSelfUser.id)(conversationId) }
    }

    @Test
    fun `given action answer & incoming call for other invalid session, when handling, then do not answer & emit proper stop`() = runTest {
        val otherSelfUser = selfUser.copy(id = selfUser.id.copy("other"))
        val (arrangement, callServiceManager) = Arrangement()
            .withCurrentSession(flowOf(CurrentSessionResult.Success(AccountInfo.Valid(selfUser.id))))
            .withSpecificUserSession(selfUser = selfUser, incoming = flowOf(emptyList()))
            .withValidSessionExists(userId = otherSelfUser.id, result = DoesValidSessionExistResult.Success(false))
            .arrange()
        callServiceManager.handleAction(CallService.Action.Start.AnswerCall(otherSelfUser.id, conversationId))
        callServiceManager.handleActionsFlow().test {
            assertIsStop(awaitItem()).also {
                assertEquals(CallServiceManager.StopReason.NO_CALLS, it)
            }
        }
        coVerify(exactly = 0) { arrangement.answerCallForUser(otherSelfUser.id)(conversationId) }
    }

    @Test
    fun `given action answer & incoming call, when handling & call becomes established, then emit changed call data`() = runTest {
        val incomingCall = call.copy(status = CallStatus.INCOMING)
        val establishedCall = call.copy(status = CallStatus.ESTABLISHED)
        val incomingCallsFlow = MutableStateFlow(listOf(incomingCall))
        val establishedCallsFlow = MutableStateFlow(emptyList<Call>())
        val (_, callServiceManager) = Arrangement()
            .withCurrentSession(flowOf(CurrentSessionResult.Success(AccountInfo.Valid(selfUser.id))))
            .withSpecificUserSession(selfUser = selfUser, incoming = incomingCallsFlow, established = establishedCallsFlow)
            .arrange()
        callServiceManager.handleAction(CallService.Action.Start.AnswerCall(selfUser.id, conversationId))
        callServiceManager.handleActionsFlow().test {
            assertIsUpdate(awaitItem()).also {
                assertEquals(CallNotificationData(selfUser.id, incomingCall, selfUser.name()), it)
            }
            establishedCallsFlow.emit(listOf(establishedCall))
            incomingCallsFlow.emit(emptyList())
            assertIsUpdate(awaitItem()).also {
                assertEquals(CallNotificationData(selfUser.id, establishedCall, selfUser.name()), it)
            }
        }
    }

    private fun assertIsStop(item: Any) = assertIs<Either.Left<CallServiceManager.StopReason>>(item).value
    private fun assertIsUpdate(item: Any) = assertIs<Either.Right<CallNotificationData>>(item).value

    inner class Arrangement {
        @MockK
        lateinit var coreLogic: CoreLogic
        private val answerCallForUser: MutableMap<UserId, AnswerCallUseCase> = mutableMapOf()

        init {
            MockKAnnotations.init(this)
            withSpecificUserSession(TestUser.SELF_USER, flowOf(emptyList()), flowOf(emptyList()), flowOf(emptyList()),)
        }

        fun arrange() = this to CallServiceManager(coreLogic)
        fun answerCallForUser(userId: UserId): AnswerCallUseCase = answerCallForUser.getOrPut(userId) { mockk(relaxed = true) }
        fun withCurrentSession(result: Flow<CurrentSessionResult>) = apply {
            coEvery { coreLogic.getGlobalScope().session.currentSessionFlow() } returns (result)
        }
        fun withValidSessionExists(userId: UserId, result: DoesValidSessionExistResult) = apply {
            coEvery { coreLogic.getGlobalScope().doesValidSessionExist(userId) } returns (result)
        }
        fun withSpecificUserSession(
            selfUser: SelfUser = TestUser.SELF_USER,
            incoming: Flow<List<Call>> = flowOf(emptyList()),
            outgoing: Flow<List<Call>> = flowOf(emptyList()),
            established: Flow<List<Call>> = flowOf(emptyList()),
        ) = apply {
            withValidSessionExists(selfUser.id, DoesValidSessionExistResult.Success(true))
            coEvery { coreLogic.getSessionScope(selfUser.id).users.observeSelfUser() } returns flowOf(selfUser)
            coEvery { coreLogic.getSessionScope(selfUser.id).calls.getIncomingCalls() } returns incoming
            coEvery { coreLogic.getSessionScope(selfUser.id).calls.observeOutgoingCall() } returns outgoing
            coEvery { coreLogic.getSessionScope(selfUser.id).calls.establishedCall() } returns established
            coEvery { coreLogic.getSessionScope(selfUser.id).calls.answerCall } returns answerCallForUser(selfUser.id)
        }
    }

    companion object {
        val selfUser = TestUser.SELF_USER
        val conversationId = TestConversation.ID
        val call = Call(
            conversationId = conversationId,
            status = CallStatus.ESTABLISHED,
            isMuted = true,
            isCameraOn = false,
            isCbrEnabled = false,
            callerId = TestUser.USER_ID,
            conversationName = "Conversation Name",
            conversationType = Conversation.Type.OneOnOne,
            callerName = "Caller Name",
            callerTeamName = "Team Name"
        )
    }
}
private fun SelfUser.name() = this.handle ?: this.name ?: ""
