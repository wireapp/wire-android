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
package com.wire.android.ui.home.conversations.usecase

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.framework.TestMessage
import com.wire.android.framework.TestUser
import com.wire.android.mapper.MessageMapper
import com.wire.kalium.logic.data.user.User
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.conversation.ObserveUserListByIdUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class GetUsersForMessageUseCaseTest {

    @Test
    fun givenMessageWithoutAdditionalUserIds_whenInvoke_thenObserveMemberDetailsByIdsIsNotTriggered() = runTest {
        val sender = TestUser.OTHER_USER
        val userWithoutOtherUsers = TestMessage.TEXT_MESSAGE.copy(sender = sender, senderUserId = sender.id)

        val (arrangement, useCase) = Arrangement()
            .withMemberDetails(listOf())
            .withMemberList(listOf())
            .arrange()

        val result = useCase(userWithoutOtherUsers)

        assertTrue(result.first() == sender)
        coVerify(exactly = 0) { arrangement.observeMemberDetailsByIds(any()) }
    }

    @Test
    fun givenMessageWithAdditionalUserIds_whenInvoke_thenObserveMemberDetailsByIdsIsTriggered() = runTest {
        val otherUser = TestUser.OTHER_USER
        val userWithoutOtherUsers = TestMessage.MEMBER_REMOVED_MESSAGE
        val user1 = TestUser.OTHER_USER.copy(
            id = UserId("user-id1", "domain")
        )
        val user2 = TestUser.OTHER_USER.copy(
            id = UserId("user-id2", "domain")
        )

        val (arrangement, useCase) = Arrangement()
            .withMemberDetails(listOf(user1, user2))
            .withMemberList(listOf(otherUser.id))
            .arrange()


        val result = useCase(userWithoutOtherUsers)

        assertTrue(result.first().equals(user1))
        coVerify(exactly = 1) { arrangement.observeMemberDetailsByIds(any()) }
    }

    private class Arrangement {

        @MockK
        lateinit var observeMemberDetailsByIds: ObserveUserListByIdUseCase

        @MockK
        lateinit var messageMapper: MessageMapper

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        suspend fun withMemberDetails(userList: List<User>) = apply {
            coEvery { observeMemberDetailsByIds(any()) } returns flowOf(userList)
        }

        fun withMemberList(userIdList: List<UserId>) = apply {
            every { messageMapper.memberIdList(any()) } returns userIdList
        }

        fun arrange() = this to GetUsersForMessageUseCase(
            observeMemberDetailsByIds, messageMapper
        )
    }
}
