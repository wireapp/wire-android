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

package com.wire.android.ui.home.conversations.details.metadata

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.framework.TestConversation
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.RenameConversationUseCase
import com.wire.kalium.logic.feature.conversation.RenamingResult
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class EditConversationMetadataViewModelTest {

    @Test
    fun `given conversation details, when loaded, then name and channel projection are exposed`() = runTest {
        val channel = TestConversation.GROUP().copy(
            name = "Channel name",
            type = Conversation.Type.Group.Channel,
        )
        val (_, viewModel) = Arrangement().withConversation(channel).arrange()
        advanceUntilIdle()

        assertEquals("Channel name", viewModel.editConversationNameTextState.text.toString())
        assertEquals("Channel name", viewModel.editConversationState.originalGroupName)
        assertTrue(viewModel.editConversationState.isChannel)
    }

    @Test
    fun `given conversation name has leading and trailing spaces, when saving, then rename uses trimmed name`() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange()
        advanceUntilIdle()

        viewModel.editConversationNameTextState.setTextAndPlaceCursorAtEnd(" New group name ")
        viewModel.saveNewGroupName()
        advanceUntilIdle()

        coVerify {
            arrangement.renameConversation(TestConversation.ID, "New group name")
        }
    }

    @Test
    fun `given rename succeeds, when saving, then completion is success`() = runTest {
        val (_, viewModel) = Arrangement().arrange()
        advanceUntilIdle()

        viewModel.saveNewGroupName()
        advanceUntilIdle()

        assertEquals(EditConversationMetadataState.Completed.Success, viewModel.editConversationState.completed)
    }

    @Test
    fun `given rename fails, when saving, then completion is failure`() = runTest {
        val (_, viewModel) = Arrangement()
            .withRenamingResult(RenamingResult.Failure(CoreFailure.Unknown(null)))
            .arrange()
        advanceUntilIdle()

        viewModel.saveNewGroupName()
        advanceUntilIdle()

        assertEquals(EditConversationMetadataState.Completed.Failure, viewModel.editConversationState.completed)
    }

    private class Arrangement {

        @MockK
        lateinit var observeConversationDetails: ObserveConversationDetailsUseCase

        @MockK
        lateinit var renameConversation: RenameConversationUseCase

        private var conversation: Conversation = TestConversation.GROUP()
        private var renamingResult: RenamingResult = RenamingResult.Success

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        fun withConversation(conversation: Conversation) = apply {
            this.conversation = conversation
        }

        fun withRenamingResult(renamingResult: RenamingResult) = apply {
            this.renamingResult = renamingResult
        }

        fun arrange(): Pair<Arrangement, EditConversationMetadataViewModel> {
            coEvery { observeConversationDetails(any()) } returns flowOf(
                ObserveConversationDetailsUseCase.Result.Success(
                    ConversationDetails.Group.Regular(
                        conversation = conversation,
                        isSelfUserMember = true,
                        selfRole = Conversation.Member.Role.Member,
                        wireCell = null,
                    ),
                ),
            )
            coEvery { renameConversation(any(), any()) } returns renamingResult

            return this to EditConversationMetadataViewModel(
                dispatcher = TestDispatcherProvider(),
                observeConversationDetails = observeConversationDetails,
                renameConversation = renameConversation,
                navigationArgs = EditConversationNameNavArgs(TestConversation.ID),
            )
        }
    }
}
