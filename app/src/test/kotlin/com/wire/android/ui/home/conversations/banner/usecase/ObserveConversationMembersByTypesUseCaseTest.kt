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

package com.wire.android.ui.home.conversations.banner.usecase

import app.cash.turbine.test
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.mapper.testOtherUser
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.MemberDetails
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.data.user.type.UserTypeInfo
import com.wire.kalium.logic.feature.conversation.ObserveConversationMembersUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveConversationMembersByTypesUseCaseTest {

    @Test
    fun `given a group members, when there are external members, then set should contain external type`() =
        runTest {
            // Given
            val members = listOf(
                MemberDetails(
                    testOtherUser(0).copy(userType = UserTypeInfo.Regular(UserType.EXTERNAL)),
                    Conversation.Member.Role.Member
                )
            )

            val (_, useCase) = Arrangement()
                .withConversationMembersUpdate(members)
                .arrange()
            // When - Then
            useCase(ConversationId("", "")).test {
                val data = awaitItem()
                assert(data.contains(UserTypeInfo.Regular(UserType.EXTERNAL)))
            }
        }

    @Test
    fun `given a group members, when there are federated members, then set should contain federated type`() =
        runTest {
            // Given
            val members = listOf(
                MemberDetails(
                    testOtherUser(0).copy(userType = UserTypeInfo.Regular(UserType.FEDERATED)),
                    Conversation.Member.Role.Member
                )
            )

            val (_, useCase) = Arrangement()
                .withConversationMembersUpdate(members)
                .arrange()
            // When - Then
            useCase(ConversationId("", "")).test {
                val data = awaitItem()
                assert(data.contains(UserTypeInfo.Regular(UserType.FEDERATED)))
            }
        }

    @Test
    fun `given a group members, when there are guest members, then set should contain guest type`() =
        runTest {
            // Given
            val members = listOf(
                MemberDetails(
                    testOtherUser(0).copy(userType = UserTypeInfo.Regular(UserType.GUEST)),
                    Conversation.Member.Role.Member
                )
            )

            val (_, useCase) = Arrangement()
                .withConversationMembersUpdate(members)
                .arrange()
            // When - Then
            useCase(ConversationId("", "")).test {
                val data = awaitItem()
                assert(data.contains(UserTypeInfo.Regular(UserType.GUEST)))
            }
        }

    @Test
    fun `given a group members, when there are service bots, then set should contain service type`() =
        runTest {
            // Given
            val members = listOf(
                MemberDetails(
                    testOtherUser(0).copy(userType = UserTypeInfo.Bot),
                    Conversation.Member.Role.Member
                )
            )

            val (_, useCase) = Arrangement()
                .withConversationMembersUpdate(members)
                .arrange()
            // When - Then
            useCase(ConversationId("", "")).test {
                val data = awaitItem()
                assert(data.contains(UserTypeInfo.Bot))
            }
        }

    private class Arrangement {

        @MockK
        lateinit var observeConversationMembersUseCase: ObserveConversationMembersUseCase
        private val conversationMembersChannel = Channel<List<MemberDetails>>(capacity = Channel.UNLIMITED)
        private val useCase by lazy {
            ObserveConversationMembersByTypesUseCase(
                observeConversationMembersUseCase,
                dispatchers = TestDispatcherProvider()
            )
        }

        init {
            // Tests setup
            MockKAnnotations.init(this, relaxUnitFun = true)
            // Default empty values
            coEvery { observeConversationMembersUseCase(any()) } returns flowOf()
        }

        suspend fun withConversationMembersUpdate(members: List<MemberDetails>) = apply {
            coEvery { observeConversationMembersUseCase(any()) } returns conversationMembersChannel.consumeAsFlow()
            conversationMembersChannel.send(members)
        }

        fun arrange() = this to useCase
    }

}
