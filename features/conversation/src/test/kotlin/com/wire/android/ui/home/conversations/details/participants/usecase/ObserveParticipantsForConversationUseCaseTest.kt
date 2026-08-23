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
import com.wire.kalium.logic.data.conversation.Conversation.Member
import com.wire.kalium.logic.data.conversation.MemberDetails
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.data.user.type.UserTypeInfo
import com.wire.kalium.logic.feature.conversation.ObserveConversationMembersUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.GetMembersE2EICertificateStatusesUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveParticipantsForConversationUseCaseTest {

    @Test
    fun givenGroupMembers_whenSolvingTheParticipantsListWithLimit_thenLimitedSizesArePassed() = runTest {
        // Given
        val limit = 4
        val members = buildList {
            for (i in 1..(limit + 1)) {
                add(MemberDetails(testOtherUser(i).copy(userType = UserTypeInfo.Regular(UserType.INTERNAL)), Member.Role.Member))
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
                add(MemberDetails(testOtherUser(i).copy(userType = UserTypeInfo.Regular(UserType.INTERNAL)), Member.Role.Member))
            }
        }
        val userId1 = UserId(value = "value1", domain = "domain1")
        val userId2 = UserId(value = "value2", domain = "domain2")
        val userId3 = UserId(value = "value3", domain = "domain3")
        val (_, useCase) = ObserveParticipantsForConversationUseCaseArrangement()
            .withConversationParticipantsUpdate(members)
            .withE2EICertificateStatuses(mapOf(userId1 to true, userId2 to false))
            .arrange()
        // When - Then
        useCase(ConversationId("", "")).test {
            val data = awaitItem()
            assert(data.participants.size == members.size)
            assert(data.allParticipantsCount == members.size)
            assertEquals(true, data.participants.firstOrNull { it.id == userId1 }?.isMLSVerified)
            assertEquals(false, data.participants.firstOrNull { it.id == userId2 }?.isMLSVerified)
            assertEquals(false, data.participants.firstOrNull { it.id == userId3 }?.isMLSVerified) // false if null
        }
    }

    @Test
    fun givenGroupMembersUnderLegalHold_whenSolvingTheParticipantsList_thenPassCorrectLegalHoldValues() = runTest {
        // Given
        val memberUnderLegalHold = MemberDetails(
            user = testOtherUser(0).copy(userType = UserTypeInfo.Regular(UserType.INTERNAL), isUnderLegalHold = true),
            role = Member.Role.Member
        )
        val memberNotUnderLegalHold = MemberDetails(
            user = testOtherUser(1).copy(userType = UserTypeInfo.Regular(UserType.INTERNAL), isUnderLegalHold = false),
            role = Member.Role.Member
        )
        val (_, useCase) = ObserveParticipantsForConversationUseCaseArrangement()
            .withConversationParticipantsUpdate(listOf(memberUnderLegalHold, memberNotUnderLegalHold))
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

    @Test
    fun givenGroup_whenVisibleMembersListChanges_thenGetE2EICertificateOnlyForNewOnes() = runTest {
        // Given
        val members: List<MemberDetails> = buildList {
            for (i in 1..6) {
                add(MemberDetails(testOtherUser(i).copy(userType = UserTypeInfo.Regular(UserType.INTERNAL)), Member.Role.Member))
            }
        }
        val members1 = members.subList(0, 4)
        val members2 = members.subList(4, 6)
        val (arrangement, useCase) = ObserveParticipantsForConversationUseCaseArrangement()
            .withConversationParticipantsUpdate(members1)
            .arrange()
        // When - Then
        useCase(ConversationId("", "")).test {
            val result1 = awaitItem()
            assertEquals(result1.participants.size, members1.size)
            assertEquals(result1.allParticipantsCount, members1.size)
            // emit updated members list with new members that should also be visible because there is no limit set
            arrangement.withConversationParticipantsUpdate(members1 + members2)
            advanceUntilIdle()
            val result2 = awaitItem()
            assertEquals(result2.participants.size, members1.size + members2.size)
            assertEquals(result2.allParticipantsCount, members1.size + members2.size)

            // certificate statuses are fetched once for the initial visible members list
            coVerify(exactly = 1) { arrangement.getMembersE2EICertificateStatuses(any(), eq(members1.map { it.user.id })) }
            // and then second time only for newly emitted visible members
            coVerify(exactly = 1) { arrangement.getMembersE2EICertificateStatuses(any(), eq(members2.map { it.user.id })) }
        }
    }

    @Test
    fun givenGroup_whenVisibleMembersDoesNotChange_thenDoNotGetE2EICertificateForNonVisibleMembers() = runTest {
        // Given
        val members: List<MemberDetails> = buildList {
            for (i in 1..6) {
                add(MemberDetails(testOtherUser(i).copy(userType = UserTypeInfo.Regular(UserType.INTERNAL)), Member.Role.Member))
            }
        }
        val limit = 4
        val members1 = members.subList(0, limit)
        val members2 = members.subList(limit, limit + 2)
        val (arrangement, useCase) = ObserveParticipantsForConversationUseCaseArrangement()
            .withConversationParticipantsUpdate(members1)
            .arrange()
        // When - Then
        useCase(ConversationId("", ""), limit = limit).test {
            val result1 = awaitItem()
            assertEquals(result1.participants.size, members1.size)
            assertEquals(result1.allParticipantsCount, members1.size)
            // emit updated members list with new members that should not be visible because there is no limit set
            arrangement.withConversationParticipantsUpdate(members1 + members2)
            advanceUntilIdle()
            val result2 = awaitItem()
            assertEquals(result2.participants.size, members1.size)
            assertEquals(result2.allParticipantsCount, members1.size + members2.size)

            // certificate statuses are fetched once for the initial visible members list
            coVerify(exactly = 1) { arrangement.getMembersE2EICertificateStatuses(any(), eq(members1.map { it.user.id })) }
            // and then newly emitted members are non-visible (because of the limit) and should not be fetched
            coVerify(exactly = 0) { arrangement.getMembersE2EICertificateStatuses(any(), eq(members2.map { it.user.id })) }
        }
    }

    @Test
    fun givenGroup_whenVisibleMembersDoesNotChange_thenDoNotGetE2EICertificateAgainForTheSameList() = runTest {
        // Given
        val members: List<MemberDetails> = buildList {
            for (i in 1..6) {
                add(MemberDetails(testOtherUser(i).copy(userType = UserTypeInfo.Regular(UserType.INTERNAL)), Member.Role.Member))
            }
        }
        val (arrangement, useCase) = ObserveParticipantsForConversationUseCaseArrangement()
            .withConversationParticipantsUpdate(members)
            .arrange()
        // When - Then
        useCase(ConversationId("", "")).test {
            val result1 = awaitItem()
            assertEquals(result1.participants.size, members.size)
            assertEquals(result1.allParticipantsCount, members.size)
            // emit the same members list again
            arrangement.withConversationParticipantsUpdate(members)
            advanceUntilIdle()
            val result2 = awaitItem()
            assertEquals(result2.participants.size, members.size)
            assertEquals(result2.allParticipantsCount, members.size)

            // executed only once for visible members list even if the same list was emitted twice
            coVerify(exactly = 1) { arrangement.getMembersE2EICertificateStatuses(any(), eq(members.map { it.user.id })) }
        }
    }
}

internal class ObserveParticipantsForConversationUseCaseArrangement {

    @MockK
    lateinit var observeConversationMembersUseCase: ObserveConversationMembersUseCase

    @MockK
    lateinit var getMembersE2EICertificateStatuses: GetMembersE2EICertificateStatusesUseCase

    private val uIParticipantMapper by lazy { UIParticipantMapper(UserTypeMapper()) }
    private val conversationMembersChannel = Channel<List<MemberDetails>>(capacity = Channel.UNLIMITED)
    private val useCase by lazy {
        ObserveParticipantsForConversationUseCase(
            observeConversationMembersUseCase,
            getMembersE2EICertificateStatuses,
            uIParticipantMapper,
            dispatchers = TestDispatcherProvider()
        )
    }

    init {
        // Tests setup
        MockKAnnotations.init(this, relaxUnitFun = true)
        // Default empty values
        coEvery { observeConversationMembersUseCase(any()) } returns flowOf()
        coEvery {
            getMembersE2EICertificateStatuses(any(), any())
        } answers {
            secondArg<List<UserId>>().associateWith { false }
        }
    }

    suspend fun withConversationParticipantsUpdate(members: List<MemberDetails>): ObserveParticipantsForConversationUseCaseArrangement {
        coEvery { observeConversationMembersUseCase(any()) } returns conversationMembersChannel.consumeAsFlow()
        conversationMembersChannel.send(members)
        return this
    }

    suspend fun withE2EICertificateStatuses(result: Map<UserId, Boolean>) = apply {
        coEvery { getMembersE2EICertificateStatuses(any(), any()) } answers { result }
    }

    fun arrange() = this to useCase
}
