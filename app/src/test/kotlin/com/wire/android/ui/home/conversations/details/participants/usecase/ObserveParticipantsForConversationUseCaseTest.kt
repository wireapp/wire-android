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

package com.wire.android.ui.home.conversations.details.participants.usecase

import app.cash.turbine.test
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.mapper.UIParticipantMapper
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.mapper.testOtherUser
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.conversation.Conversation.Member
import com.wire.kalium.logic.data.conversation.MemberDetails
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.feature.conversation.ObserveConversationMembersUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.GetMembersE2EICertificateStatusesUseCase
import com.wire.kalium.logic.feature.legalhold.MembersHavingLegalHoldClientUseCase
import com.wire.kalium.logic.functional.Either
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveParticipantsForConversationUseCaseTest {

    @Test
    fun givenGroupMembers_whenSolvingTheParticipantsListWithLimit_thenLimitedSizesArePassed() = runTest {
        // Given
        val limit = 4
        val members = buildList {
            for (i in 1..(limit + 1)) {
                add(MemberDetails(testOtherUser(i).copy(userType = UserType.INTERNAL), Member.Role.Member))
            }
        }
        val (_, useCase) = ObserveParticipantsForConversationUseCaseArrangement()
            .withConversationParticipantsUpdate(members)
            .arrange()
        // When - Then
        useCase(ConversationId("", ""), limit).test {
            val data = awaitItem()
            assertEquals(limit, data.participants.size)
            assertEquals(members.size, data.allParticipantsCount)
        }
    }

    @Test
    fun givenGroupMembers_whenSolvingTheParticipantsListWithoutLimit_thenAllListsArePassed() = runTest {
        // Given
        val members: List<MemberDetails> = buildList {
            for (i in 1..20) {
                add(MemberDetails(testOtherUser(i).copy(userType = UserType.INTERNAL), Member.Role.Member))
            }
        }
        val (_, useCase) = ObserveParticipantsForConversationUseCaseArrangement()
            .withConversationParticipantsUpdate(members)
            .arrange()
        // When - Then
        useCase(ConversationId("", "")).test {
            val data = awaitItem()
            assert(data.participants.size == members.size)
            assert(data.allParticipantsCount == members.size)
        }
    }

    @Test
    fun givenGroupMembersUnderLegalHold_whenSolvingTheParticipantsList_thenPassCorrectLegalHoldValues() = runTest {
        // Given
        val memberUnderLegalHold = MemberDetails(testOtherUser(0).copy(userType = UserType.INTERNAL), Member.Role.Member)
        val memberNotUnderLegalHold = MemberDetails(testOtherUser(1).copy(userType = UserType.INTERNAL), Member.Role.Member)
        val (_, useCase) = ObserveParticipantsForConversationUseCaseArrangement()
            .withConversationParticipantsUpdate(listOf(memberUnderLegalHold, memberNotUnderLegalHold))
            .withMembersHavingLegalHoldClient(listOf(memberUnderLegalHold.user.id))
            .arrange()
        // When - Then
        useCase(ConversationId("", "")).test {
            val data = awaitItem()
            for (participant in data.participants) {
                val expected = participant.id == memberUnderLegalHold.user.id
                assertEquals(expected, participant.isUnderLegalHold)
            }
        }
    }
}

internal class ObserveParticipantsForConversationUseCaseArrangement {

    @MockK
    lateinit var observeConversationMembersUseCase: ObserveConversationMembersUseCase

    @MockK
    lateinit var getMembersE2EICertificateStatuses: GetMembersE2EICertificateStatusesUseCase

    @MockK
    lateinit var membersHavingLegalHoldClientUseCase: MembersHavingLegalHoldClientUseCase

    @MockK
    private lateinit var wireSessionImageLoader: WireSessionImageLoader
    private val uIParticipantMapper by lazy { UIParticipantMapper(UserTypeMapper(), wireSessionImageLoader) }
    private val conversationMembersChannel = Channel<List<MemberDetails>>(capacity = Channel.UNLIMITED)
    private val useCase by lazy {
        ObserveParticipantsForConversationUseCase(
            observeConversationMembersUseCase,
            getMembersE2EICertificateStatuses,
            membersHavingLegalHoldClientUseCase,
            uIParticipantMapper,
            dispatchers = TestDispatcherProvider()
        )
    }

    init {
        // Tests setup
        MockKAnnotations.init(this, relaxUnitFun = true)
        // Default empty values
        coEvery { observeConversationMembersUseCase(any()) } returns flowOf()
        coEvery { getMembersE2EICertificateStatuses(any(), any()) } returns mapOf()
        coEvery { membersHavingLegalHoldClientUseCase(any()) } returns Either.Right(emptyList())
    }

    suspend fun withConversationParticipantsUpdate(members: List<MemberDetails>): ObserveParticipantsForConversationUseCaseArrangement {
        coEvery { observeConversationMembersUseCase(any()) } returns conversationMembersChannel.consumeAsFlow()
        conversationMembersChannel.send(members)
        return this
    }

    suspend fun withMembersHavingLegalHoldClient(members: List<UserId>) = apply {
        coEvery { membersHavingLegalHoldClientUseCase(any()) } returns Either.Right(members)
    }

    fun arrange() = this to useCase
}
