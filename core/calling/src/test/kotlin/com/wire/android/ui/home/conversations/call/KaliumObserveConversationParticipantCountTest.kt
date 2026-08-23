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

package com.wire.android.ui.home.conversations.call

import com.wire.android.mapper.testOtherUser
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.MemberDetails
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.conversation.ObserveConversationMembersUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KaliumObserveConversationParticipantCountTest {

    @Test
    fun givenNoMembers_whenObservingParticipantCount_thenEmitsZero() = runTest {
        val (_, observeParticipantCount) = Arrangement()
            .withMemberEmissions(emptyList())
            .arrange()

        assertEquals(listOf(0), observeParticipantCount(conversationId).toList())
    }

    @Test
    fun givenMembers_whenObservingParticipantCount_thenEmitsMemberCount() = runTest {
        val (_, observeParticipantCount) = Arrangement()
            .withMemberEmissions(members(3))
            .arrange()

        assertEquals(listOf(3), observeParticipantCount(conversationId).toList())
    }

    @Test
    fun givenSuccessiveMemberEmissions_whenObservingParticipantCount_thenEmitsEachCount() = runTest {
        val (_, observeParticipantCount) = Arrangement()
            .withMemberEmissions(emptyList(), members(2), members(1))
            .arrange()

        assertEquals(listOf(0, 2, 1), observeParticipantCount(conversationId).toList())
    }

    private class Arrangement {
        @MockK
        lateinit var observeConversationMembers: ObserveConversationMembersUseCase

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        suspend fun withMemberEmissions(vararg memberEmissions: List<MemberDetails>) = apply {
            coEvery { observeConversationMembers(any()) } returns flowOf(*memberEmissions)
        }

        fun arrange() = this to KaliumObserveConversationParticipantCount(observeConversationMembers)
    }

    private companion object {
        val conversationId = ConversationId("conversation-id", "domain.com")

        fun members(count: Int): List<MemberDetails> = List(count) { index ->
            MemberDetails(testOtherUser(index), Conversation.Member.Role.Member)
        }
    }
}
