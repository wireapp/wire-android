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

package com.wire.android.ui.home.conversations.details.participants

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.config.mockUri
import com.wire.android.mapper.testUIParticipant
import com.wire.android.ui.home.conversations.details.participants.model.ConversationParticipantsData
import com.wire.android.ui.home.conversations.details.participants.model.UIParticipant
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase
import com.ramcosta.composedestinations.generated.app.navArgs
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.publicuser.RefreshUsersWithoutMetadataUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
@ExtendWith(NavigationTestExtension::class)
class GroupParticipantsViewModelTest {

    private fun testSize(givenParticipantsSize: Int, expectedParticipantsSize: Int) = runTest {
        // Given
        val members = buildList {
            for (i in 1..givenParticipantsSize) {
                add(testUIParticipant(i))
            }
        }
        val (arrangement, viewModel) = Arrangement()
            .withConversationParticipantsUpdate(members)
            .arrange(expectedParticipantsSize)
        advanceUntilIdle()
        // When - Then
        coVerify { arrangement.observeParticipantsForConversationUseCase.invoke(any(), eq(expectedParticipantsSize)) }
        assertEquals(expectedParticipantsSize, viewModel.groupParticipantsState.data.participants.size)
        assertEquals(members.size, viewModel.groupParticipantsState.data.allParticipantsCount)
    }

    @Test
    fun `given a group members, when solving the participants list, then right sizes are passed`() {
        val maxNumber = 4
        testSize(givenParticipantsSize = maxNumber + 1, expectedParticipantsSize = maxNumber)
        testSize(givenParticipantsSize = maxNumber - 1, expectedParticipantsSize = maxNumber - 1)
    }
}

internal class Arrangement {

    @MockK
    private lateinit var savedStateHandle: SavedStateHandle

    @MockK
    private lateinit var refreshUsersWithoutMetadata: RefreshUsersWithoutMetadataUseCase

    @MockK
    lateinit var observeParticipantsForConversationUseCase: ObserveParticipantsForConversationUseCase

    val conversationId = ConversationId("some-dummy-value", "some.dummy.domain")

    init {
        // Tests setup
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockUri()
        every {
            savedStateHandle.navArgs<GroupConversationAllParticipantsNavArgs>()
        } returns GroupConversationAllParticipantsNavArgs(conversationId = conversationId)
        // Default empty values
        coEvery { observeParticipantsForConversationUseCase(any(), any()) } returns flowOf()
    }

    suspend fun withConversationParticipantsUpdate(participants: List<UIParticipant>): Arrangement {
        coEvery { observeParticipantsForConversationUseCase(any(), any()) } answers {
            flowOf(
                ConversationParticipantsData(
                    participants = participants.take(this.secondArg() as Int),
                    allParticipantsCount = participants.size
                )
            )
        }
        return this
    }

    fun arrange(maxNumberOfItems: Int = -1): Pair<Arrangement, GroupConversationParticipantsViewModel> =
            this to object : GroupConversationParticipantsViewModel(
                savedStateHandle,
                observeParticipantsForConversationUseCase,
                refreshUsersWithoutMetadata,
            ) {
                override val maxNumberOfItems: Int get() = maxNumberOfItems
            }
}
