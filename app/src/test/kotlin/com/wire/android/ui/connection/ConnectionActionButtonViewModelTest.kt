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
 */
@file:OptIn(ExperimentalCoroutinesApi::class)

package com.wire.android.ui.connection

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.wire.android.R
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.config.mockUri
import com.wire.android.framework.TestConversation
import com.wire.android.framework.TestUser
import com.wire.android.navigation.EXTRA_CONNECTION_STATE
import com.wire.android.navigation.EXTRA_USER_ID
import com.wire.android.navigation.EXTRA_USER_NAME
import com.wire.android.ui.userprofile.other.OtherUserProfileScreenViewModelTest
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.feature.connection.AcceptConnectionRequestUseCase
import com.wire.kalium.logic.feature.connection.AcceptConnectionRequestUseCaseResult
import com.wire.kalium.logic.feature.connection.CancelConnectionRequestUseCase
import com.wire.kalium.logic.feature.connection.CancelConnectionRequestUseCaseResult
import com.wire.kalium.logic.feature.connection.IgnoreConnectionRequestUseCase
import com.wire.kalium.logic.feature.connection.IgnoreConnectionRequestUseCaseResult
import com.wire.kalium.logic.feature.connection.SendConnectionRequestResult
import com.wire.kalium.logic.feature.connection.SendConnectionRequestUseCase
import com.wire.kalium.logic.feature.connection.UnblockUserUseCase
import com.wire.kalium.logic.feature.conversation.CreateConversationResult
import com.wire.kalium.logic.feature.conversation.GetOrCreateOneToOneConversationUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import io.mockk.Called
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test

class ConnectionActionButtonViewModelTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `given a userId, when sending a connection request, then returns a Success result and update view state`() = runTest {
        // given
        val (arrangement, viewModel) = ConnectionActionButtonHiltArrangement()
            .withSendConnectionRequest(SendConnectionRequestResult.Success)
            .arrange()
        assertEquals(ConnectionState.NOT_CONNECTED, viewModel.actionableState().state)

        viewModel.infoMessage.test {
            // when
            viewModel.onSendConnectionRequest()

            // then
            val result = awaitItem()
            assertEquals(UIText.StringResource(R.string.connection_request_sent), result)
            coVerify { arrangement.sendConnectionRequest.invoke(eq(TestUser.USER_ID)) }
            assertEquals(ConnectionState.SENT, viewModel.actionableState().state)
        }
    }

    @Test
    fun `given a userId, when sending a connection request a fails, then returns a Failure result and show error message`() =
        runTest {
            // given
            val (arrangement, viewModel) = ConnectionActionButtonHiltArrangement()
                .withSendConnectionRequest(SendConnectionRequestResult.Failure(CoreFailure.Unknown(RuntimeException("some error"))))
                .arrange()
            assertEquals(ConnectionState.NOT_CONNECTED, viewModel.actionableState().state)

            viewModel.infoMessage.test {
                // when
                viewModel.onSendConnectionRequest()

                // then
                val result = awaitItem()
                assertEquals(UIText.StringResource(R.string.connection_request_sent_error), result)
                coVerify { arrangement.sendConnectionRequest.invoke(eq(TestUser.USER_ID)) }
                assertEquals(ConnectionState.NOT_CONNECTED, viewModel.actionableState().state)
            }
        }

    @Test
    fun `given a userId, when ignoring a connection request, then returns a Success result and update view state`() =
        runTest {
            // given
            val (arrangement, viewModel) = ConnectionActionButtonHiltArrangement()
                .withIgnoreConnectionRequest(IgnoreConnectionRequestUseCaseResult.Success)
                .arrange()
            assertEquals(ConnectionState.PENDING, viewModel.actionableState().state)

            // when
            viewModel.onIgnoreConnectionRequest(arrangement.onIgnoreSuccess)

            // then
            coVerify { arrangement.ignoreConnectionRequest.invoke(eq(TestUser.USER_ID)) }
            assertEquals(ConnectionState.IGNORED, viewModel.actionableState().state)
            verify { arrangement.onIgnoreSuccess(any()) }
        }

    @Test
    fun `given a userId, when canceling a connection request, then returns a Success result and update view state`() =
        runTest {
            // given
            val (arrangement, viewModel) = ConnectionActionButtonHiltArrangement()
                .withCancelConnectionRequest(CancelConnectionRequestUseCaseResult.Success)
                .arrange()
            assertEquals(ConnectionState.SENT, viewModel.actionableState().state)

            viewModel.infoMessage.test {
                // when
                viewModel.onCancelConnectionRequest()

                // then
                val result = awaitItem()
                assertEquals(UIText.StringResource(R.string.connection_request_canceled), result)
                coVerify { arrangement.cancelConnectionRequest.invoke(eq(TestUser.USER_ID)) }
                assertEquals(ConnectionState.NOT_CONNECTED, viewModel.actionableState().state)
            }
        }

    @Test
    fun `given a userId, when accepting a connection request, then returns a Success result and update view state`() =
        runTest {
            // given
            val (arrangement, viewModel) = ConnectionActionButtonHiltArrangement()
                .withAcceptConnectionRequest(AcceptConnectionRequestUseCaseResult.Success)
                .arrange()
            assertEquals(ConnectionState.PENDING, viewModel.actionableState().state)

            viewModel.infoMessage.test {
                // when
                viewModel.onAcceptConnectionRequest()

                // then
                awaitItem()
                coVerify { arrangement.acceptConnectionRequest.invoke(eq(TestUser.USER_ID)) }
                assertEquals(ConnectionState.ACCEPTED, viewModel.actionableState().state)
            }
        }

    @Test
    fun `given a conversationId, when trying to open the conversation, then returns a Success result with the conversation`() =
        runTest {
            // given
            val (arrangement, viewModel) = ConnectionActionButtonHiltArrangement()
                .withGetOneToOneConversation(CreateConversationResult.Success(TestConversation.CONVERSATION))
                .arrange()

            // when
            viewModel.onOpenConversation(arrangement.onOpenConversation)

            // then
            coVerify {
                arrangement.getOrCreateOneToOneConversation(TestUser.USER_ID)
            }
            verify { arrangement.onOpenConversation(any()) }
        }

    @Test
    fun `given a conversationId, when trying to open the conversation and fails, then returns a Failure result and update error state`() =
        runTest {
            // given
            val (arrangement, viewModel) = ConnectionActionButtonHiltArrangement()
                .withGetOneToOneConversation(CreateConversationResult.Failure(CoreFailure.Unknown(RuntimeException("some error"))))
                .arrange()

            // when
            viewModel.onOpenConversation(arrangement.onOpenConversation)

            // then
            coVerify {
                arrangement.getOrCreateOneToOneConversation(TestUser.USER_ID)
            }
            verify { arrangement.onOpenConversation wasNot Called }
        }
}

internal class ConnectionActionButtonHiltArrangement {

    @MockK
    lateinit var savedStateHandle: SavedStateHandle

    @MockK
    lateinit var getOrCreateOneToOneConversation: GetOrCreateOneToOneConversationUseCase

    @MockK
    lateinit var sendConnectionRequest: SendConnectionRequestUseCase

    @MockK
    lateinit var cancelConnectionRequest: CancelConnectionRequestUseCase

    @MockK
    lateinit var acceptConnectionRequest: AcceptConnectionRequestUseCase

    @MockK
    lateinit var ignoreConnectionRequest: IgnoreConnectionRequestUseCase

    @MockK
    lateinit var wireSessionImageLoader: WireSessionImageLoader

    @MockK
    lateinit var unblockUser: UnblockUserUseCase

    @MockK
    lateinit var observeSelfUser: GetSelfUserUseCase

    @MockK
    lateinit var qualifiedIdMapper: QualifiedIdMapper

    @MockK(relaxed = true)
    lateinit var onIgnoreSuccess: (userName: String) -> Unit

    @MockK(relaxed = true)
    lateinit var onOpenConversation: (conversationId: ConversationId) -> Unit

    private val viewModel by lazy {
        ConnectionActionButtonViewModelImpl(
            TestDispatcherProvider(),
            sendConnectionRequest,
            cancelConnectionRequest,
            acceptConnectionRequest,
            ignoreConnectionRequest,
            unblockUser,
            getOrCreateOneToOneConversation,
            savedStateHandle,
            qualifiedIdMapper
        )
    }

    init {
        MockKAnnotations.init(this, relaxUnitFun = true)
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mockUri()
        every { savedStateHandle.get<String>(EXTRA_USER_ID) } returns "some_value@some_domain"
        every { savedStateHandle.get<String>(EXTRA_USER_NAME) } returns TestUser.OTHER_USER.name
        every { savedStateHandle.get<String>(EXTRA_CONNECTION_STATE) } returns
                ConnectionState.NOT_CONNECTED.toString()

        coEvery { observeSelfUser() } returns flowOf(TestUser.SELF_USER)
        coEvery { getOrCreateOneToOneConversation(TestConversation.ID) } returns CreateConversationResult.Success(
            OtherUserProfileScreenViewModelTest.CONVERSATION
        )

        coEvery {
            qualifiedIdMapper.fromStringToQualifiedID("some_value@some_domain")
        } returns TestUser.USER_ID
    }

    fun withGetOneToOneConversation(result: CreateConversationResult) = apply {
        coEvery { getOrCreateOneToOneConversation(TestUser.USER_ID) } returns result
    }

    fun withAcceptConnectionRequest(result: AcceptConnectionRequestUseCaseResult) = apply {
        every { savedStateHandle.get<String>(EXTRA_CONNECTION_STATE) } returns
                ConnectionState.PENDING.toString()
        coEvery { acceptConnectionRequest(any()) } returns result
    }

    fun withCancelConnectionRequest(result: CancelConnectionRequestUseCaseResult) = apply {
        every { savedStateHandle.get<String>(EXTRA_CONNECTION_STATE) } returns
                ConnectionState.SENT.toString()
        coEvery { cancelConnectionRequest(any()) } returns result
    }

    fun withIgnoreConnectionRequest(result: IgnoreConnectionRequestUseCaseResult) = apply {
        every { savedStateHandle.get<String>(EXTRA_CONNECTION_STATE) } returns
                ConnectionState.PENDING.toString()
        coEvery { ignoreConnectionRequest(any()) } returns result
    }

    fun withSendConnectionRequest(result: SendConnectionRequestResult) = apply {
        coEvery { sendConnectionRequest.invoke(any()) } returns result
    }

    fun arrange() = this to viewModel
}
