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
@file:OptIn(ExperimentalCoroutinesApi::class)

package com.wire.android.ui.connection

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.wire.android.R
import com.wire.android.config.ScopedArgsTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.config.mockUri
import com.wire.android.di.scopedArgs
import com.wire.android.framework.TestConversation
import com.wire.android.framework.TestUser
import com.wire.android.ui.userprofile.other.OtherUserProfileScreenViewModelTest
import com.wire.android.util.ui.UIText
import com.wire.kalium.common.error.CoreFailure
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
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ScopedArgsTestExtension::class)
class ConnectionActionButtonViewModelTest {

    @Test
    fun `given success, when sending a connection request, then emit success message`() = runTest {
        // given
        val (arrangement, viewModel) = ConnectionActionButtonHiltArrangement()
            .withSendConnectionRequest(SendConnectionRequestResult.Success)
            .arrange()

        viewModel.infoMessage.test {
            // when
            viewModel.onSendConnectionRequest()

            // then
            val result = awaitItem()
            assertEquals(UIText.StringResource(R.string.connection_request_sent), result)
            coVerify(exactly = 1) { arrangement.sendConnectionRequest.invoke(eq(TestUser.USER_ID)) }
            assertEquals(false, viewModel.actionableState().isPerformingAction)
        }
    }

    @Test
    fun `given a failure, when sending a connection request, then emit failure message`() = runTest {
        // given
        val (arrangement, viewModel) = ConnectionActionButtonHiltArrangement()
            .withSendConnectionRequest(SendConnectionRequestResult.Failure.GenericFailure(failure))
            .arrange()

        viewModel.infoMessage.test {
            // when
            viewModel.onSendConnectionRequest()

            // then
            val result = awaitItem()
            assertEquals(UIText.StringResource(R.string.connection_request_sent_error), result)
            coVerify(exactly = 1) { arrangement.sendConnectionRequest.invoke(eq(TestUser.USER_ID)) }
            assertEquals(false, viewModel.actionableState().isPerformingAction)
        }
    }

    @Test
    fun `given a federation denied failure, when sending a connection request, then emit proper failure message`() = runTest {
        // given
        val (arrangement, viewModel) = ConnectionActionButtonHiltArrangement()
            .withSendConnectionRequest(SendConnectionRequestResult.Failure.FederationDenied)
            .arrange()

        viewModel.infoMessage.test {
            // when
            viewModel.onSendConnectionRequest()

            // then
            val result = awaitItem() as UIText.StringResource
            assertEquals(UIText.StringResource(R.string.connection_request_sent_federation_denied_error, "").resId, result.resId)
            coVerify(exactly = 1) { arrangement.sendConnectionRequest.invoke(eq(TestUser.USER_ID)) }
            assertEquals(false, viewModel.actionableState().isPerformingAction)
        }
    }

    @Test
    fun `given a legal hold failure, when sending a connection request, then edit the state properly`() = runTest {
        // given
        val (arrangement, viewModel) = ConnectionActionButtonHiltArrangement()
            .withSendConnectionRequest(SendConnectionRequestResult.Failure.MissingLegalHoldConsent)
            .arrange()

        viewModel.infoMessage.test {
            // when
            viewModel.onSendConnectionRequest()

            // then
            expectNoEvents() // we don't want  to show any info message like snackbar
            assertEquals(viewModel.state.missingLegalHoldConsentDialogState, MissingLegalHoldConsentDialogState.Visible(TestUser.USER_ID))
            coVerify(exactly = 1) { arrangement.sendConnectionRequest.invoke(eq(TestUser.USER_ID)) }
            assertEquals(false, viewModel.actionableState().isPerformingAction)
        }
    }

    @Test
    fun `given success, when ignoring a connection request, then calls onIgnoreSuccess`() =
        runTest {
            // given
            val (arrangement, viewModel) = ConnectionActionButtonHiltArrangement()
                .withIgnoreConnectionRequest(IgnoreConnectionRequestUseCaseResult.Success)
                .arrange()

            viewModel.actions.test {

                // when
                viewModel.onIgnoreConnectionRequest()

                val action = expectMostRecentItem()

                // then
                coVerify(exactly = 1) { arrangement.ignoreConnectionRequest.invoke(eq(TestUser.USER_ID)) }
                assertTrue(action is ConnectionRequestIgnored)
                assertEquals(false, viewModel.actionableState().isPerformingAction)
            }
        }

    @Test
    fun `given failure, when ignoring a connection request, then emit error message`() =
        runTest {
            // given
            val (arrangement, viewModel) = ConnectionActionButtonHiltArrangement()
                .withIgnoreConnectionRequest(IgnoreConnectionRequestUseCaseResult.Failure(failure))
                .arrange()

            viewModel.infoMessage.test {
                // when
                viewModel.onIgnoreConnectionRequest()

                // then
                val result = awaitItem()
                coVerify(exactly = 1) { arrangement.ignoreConnectionRequest.invoke(eq(TestUser.USER_ID)) }
                assertEquals(UIText.StringResource(R.string.connection_request_ignore_error), result)
                assertEquals(false, viewModel.actionableState().isPerformingAction)
            }
        }

    @Test
    fun `given success, when canceling a connection request, then emit success message`() =
        runTest {
            // given
            val (arrangement, viewModel) = ConnectionActionButtonHiltArrangement()
                .withCancelConnectionRequest(CancelConnectionRequestUseCaseResult.Success)
                .arrange()

            viewModel.infoMessage.test {
                // when
                viewModel.onCancelConnectionRequest()

                // then
                val result = awaitItem()
                assertEquals(UIText.StringResource(R.string.connection_request_canceled), result)
                coVerify(exactly = 1) { arrangement.cancelConnectionRequest.invoke(eq(TestUser.USER_ID)) }
                assertEquals(false, viewModel.actionableState().isPerformingAction)
            }
        }

    @Test
    fun `given failure, when canceling a connection request, then emit failure message`() =
        runTest {
            // given
            val (arrangement, viewModel) = ConnectionActionButtonHiltArrangement()
                .withCancelConnectionRequest(CancelConnectionRequestUseCaseResult.Failure(failure))
                .arrange()

            viewModel.infoMessage.test {
                // when
                viewModel.onCancelConnectionRequest()

                // then
                val result = awaitItem()
                assertEquals(UIText.StringResource(R.string.connection_request_cancel_error), result)
                coVerify(exactly = 1) { arrangement.cancelConnectionRequest.invoke(eq(TestUser.USER_ID)) }
                assertEquals(false, viewModel.actionableState().isPerformingAction)
            }
        }

    @Test
    fun `given success, when accepting a connection request, then emit success message`() =
        runTest {
            // given
            val (arrangement, viewModel) = ConnectionActionButtonHiltArrangement()
                .withAcceptConnectionRequest(AcceptConnectionRequestUseCaseResult.Success)
                .arrange()

            viewModel.infoMessage.test {
                // when
                viewModel.onAcceptConnectionRequest()

                // then
                val result = awaitItem()
                assertEquals(UIText.StringResource(R.string.connection_request_accepted), result)
                coVerify(exactly = 1) { arrangement.acceptConnectionRequest.invoke(eq(TestUser.USER_ID)) }
                assertEquals(false, viewModel.actionableState().isPerformingAction)
            }
        }

    @Test
    fun `given failure, when accepting a connection request, then emit failure message`() =
        runTest {
            // given
            val (arrangement, viewModel) = ConnectionActionButtonHiltArrangement()
                .withAcceptConnectionRequest(AcceptConnectionRequestUseCaseResult.Failure(failure))
                .arrange()

            viewModel.infoMessage.test {
                // when
                viewModel.onAcceptConnectionRequest()

                // then
                val result = awaitItem()
                assertEquals(UIText.StringResource(R.string.connection_request_accept_error), result)
                coVerify(exactly = 1) { arrangement.acceptConnectionRequest.invoke(eq(TestUser.USER_ID)) }
                assertEquals(false, viewModel.actionableState().isPerformingAction)
            }
        }

    @Test
    fun `given a conversationId, when trying to open the conversation, then returns a Success result with the conversation`() =
        runTest {
            // given
            val (arrangement, viewModel) = ConnectionActionButtonHiltArrangement()
                .withGetOneToOneConversation(CreateConversationResult.Success(TestConversation.CONVERSATION))
                .arrange()

            viewModel.actions.test {

                // when
                viewModel.onOpenConversation()

                val action = expectMostRecentItem()

                // then
                coVerify {
                    arrangement.getOrCreateOneToOneConversation(TestUser.USER_ID)
                }

                assertTrue(action is OpenConversation)
                assertEquals(false, viewModel.actionableState().isPerformingAction)
            }
        }

    @Test
    fun `given a conversationId, when trying to open the conversation and fails, then returns a Failure result and update error state`() =
        runTest {
            // given
            val (arrangement, viewModel) = ConnectionActionButtonHiltArrangement()
                .withGetOneToOneConversation(CreateConversationResult.Failure(failure))
                .arrange()

            viewModel.actions.test {

                // when
                viewModel.onOpenConversation()

                // then
                coVerify {
                    arrangement.getOrCreateOneToOneConversation(TestUser.USER_ID)
                }
                assertEquals(false, viewModel.actionableState().isPerformingAction)

                expectNoEvents()
            }
        }

    @Test
    fun `given a conversationId, when trying to open the conversation and fails with MissingKeyPackages, then call MissingKeyPackage()`() =
        runTest {
            // given
            val (arrangement, viewModel) = ConnectionActionButtonHiltArrangement()
                .withGetOneToOneConversation(CreateConversationResult.Failure(CoreFailure.MissingKeyPackages(setOf())))
                .arrange()

            viewModel.actions.test {

                // when
                viewModel.onOpenConversation()

                val action = expectMostRecentItem()

                // then
                coVerify {
                    arrangement.getOrCreateOneToOneConversation(TestUser.USER_ID)
                }

                assertTrue(action is MissingKeyPackages)

                assertEquals(false, viewModel.actionableState().isPerformingAction)
            }
        }

    companion object {
        val failure = CoreFailure.Unknown(RuntimeException("some error"))
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
    lateinit var unblockUser: UnblockUserUseCase

    @MockK
    lateinit var observeSelfUser: ObserveSelfUserUseCase

    private val viewModel by lazy {
        ConnectionActionButtonViewModelImpl(
            TestDispatcherProvider(),
            sendConnectionRequest,
            cancelConnectionRequest,
            acceptConnectionRequest,
            ignoreConnectionRequest,
            unblockUser,
            getOrCreateOneToOneConversation,
            savedStateHandle
        )
    }

    init {
        MockKAnnotations.init(this, relaxUnitFun = true)
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mockUri()
        every { savedStateHandle.scopedArgs<ConnectionActionButtonArgs>() } returns ConnectionActionButtonArgs(
            TestUser.USER_ID, TestUser.SELF_USER.name ?: ""
        )

        coEvery { observeSelfUser() } returns flowOf(TestUser.SELF_USER)
        coEvery { getOrCreateOneToOneConversation(TestConversation.ID) } returns CreateConversationResult.Success(
            OtherUserProfileScreenViewModelTest.CONVERSATION
        )
    }

    fun withGetOneToOneConversation(result: CreateConversationResult) = apply {
        coEvery { getOrCreateOneToOneConversation(TestUser.USER_ID) } returns result
    }

    fun withAcceptConnectionRequest(result: AcceptConnectionRequestUseCaseResult) = apply {
        coEvery { acceptConnectionRequest(any()) } returns result
    }

    fun withCancelConnectionRequest(result: CancelConnectionRequestUseCaseResult) = apply {

        coEvery { cancelConnectionRequest(any()) } returns result
    }

    fun withIgnoreConnectionRequest(result: IgnoreConnectionRequestUseCaseResult) = apply {
        coEvery { ignoreConnectionRequest(any()) } returns result
    }

    fun withSendConnectionRequest(result: SendConnectionRequestResult) = apply {
        coEvery { sendConnectionRequest.invoke(any()) } returns result
    }

    fun arrange() = this to viewModel
}
