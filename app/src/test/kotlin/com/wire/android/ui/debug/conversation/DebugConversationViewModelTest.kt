/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.wire.android.ui.debug.conversation

import app.cash.turbine.test
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.framework.TestConversation
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.conversation.FetchConversationUseCase
import com.wire.kalium.logic.data.conversation.ResetMLSConversationUseCase
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.debug.DebugFeedConversationUseCase
import com.wire.kalium.logic.feature.debug.GetConversationEpochFromCCResult
import com.wire.kalium.logic.feature.debug.GetConversationEpochFromCCUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
class DebugConversationViewModelTest {

    @Test
    fun givenMLSConversation_whenInitialising_thenLoadCCEpochFromUseCase() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withConversationDetails(mlsConversationDetails())
            .withCCEpochResult(GetConversationEpochFromCCResult.Success(CC_EPOCH))
            .arrange()

        advanceUntilIdle()

        assertEquals(TestConversation.MLS_PROTOCOL_INFO, viewModel.state.value.mlsProtocolInfo)
        assertEquals(CC_EPOCH, viewModel.state.value.ccEpoch)
        coVerify(exactly = 1) {
            arrangement.getConversationEpochFromCCUseCase(arrangement.conversationId)
        }
    }

    @Test
    fun givenCCEpochFetchFailure_whenRefreshing_thenShowErrorMessageAndKeepEpochUnset() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withConversationDetails(mlsConversationDetails())
            .withCCEpochResults(
                GetConversationEpochFromCCResult.Success(CC_EPOCH),
                GetConversationEpochFromCCResult.Failure.Generic(CoreFailure.MissingClientRegistration),
            )
            .arrange()

        advanceUntilIdle()

        viewModel.actions.test {
            viewModel.refreshConversationEpochFromCC()
            assertEquals(ShowMessage("Failed to refresh CC epoch."), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(CC_EPOCH, viewModel.state.value.ccEpoch)
        assertEquals(false, viewModel.state.value.isRefreshingCCEpoch)
        coVerify(exactly = 2) {
            arrangement.getConversationEpochFromCCUseCase(arrangement.conversationId)
        }
    }
}

private class Arrangement {

    @MockK
    lateinit var observeConversationDetailsUseCase: ObserveConversationDetailsUseCase

    @MockK
    lateinit var resetMLSConversationUseCase: ResetMLSConversationUseCase

    @MockK
    lateinit var fetchConversationUseCase: FetchConversationUseCase

    @MockK
    lateinit var debugFeedConversationUseCase: DebugFeedConversationUseCase

    @MockK
    lateinit var getConversationEpochFromCCUseCase: GetConversationEpochFromCCUseCase

    val conversationId = ConversationId("debug-conversation-id", "wire.com")

    init {
        MockKAnnotations.init(this, relaxUnitFun = true)
        coEvery { observeConversationDetailsUseCase(any()) } returns flowOf<ObserveConversationDetailsUseCase.Result>()
        coEvery { getConversationEpochFromCCUseCase(any()) } returns GetConversationEpochFromCCResult.Failure.NotMlsConversation
    }

    suspend fun withConversationDetails(conversationDetails: ConversationDetails) = apply {
        coEvery { observeConversationDetailsUseCase(any()) } returns flowOf(
            ObserveConversationDetailsUseCase.Result.Success(conversationDetails)
        )
    }

    suspend fun withCCEpochResult(result: GetConversationEpochFromCCResult) = apply {
        coEvery { getConversationEpochFromCCUseCase(any()) } returns result
    }

    suspend fun withCCEpochResults(vararg results: GetConversationEpochFromCCResult) = apply {
        coEvery { getConversationEpochFromCCUseCase(any()) } returnsMany results.toList()
    }

    fun arrange() = this to DebugConversationViewModel(
        conversationDetails = observeConversationDetailsUseCase,
        resetMLSConversation = resetMLSConversationUseCase,
        fetchConversation = fetchConversationUseCase,
        feedConversation = debugFeedConversationUseCase,
        getConversationEpochFromCC = getConversationEpochFromCCUseCase,
        args = DebugConversationScreenNavArgs(conversationId),
    )
}

private fun mlsConversationDetails(): ConversationDetails.Group.Regular =
    ConversationDetails.Group.Regular(
        conversation = TestConversation.GROUP(TestConversation.MLS_PROTOCOL_INFO),
        isSelfUserMember = true,
        selfRole = Conversation.Member.Role.Member,
        wireCell = null,
    )

private const val CC_EPOCH = 42UL
