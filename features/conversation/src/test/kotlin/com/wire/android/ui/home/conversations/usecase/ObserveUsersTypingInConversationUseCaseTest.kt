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
import com.wire.android.mapper.UIParticipantMapper
import com.wire.android.ui.home.conversations.details.participants.model.UIParticipant
import com.wire.android.ui.home.conversations.usecase.ObserveUsersTypingInConversationUseCaseTest.Arrangement.Companion.expectedUIParticipant
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.UserSummary
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.data.user.type.UserTypeInfo
import com.wire.kalium.logic.feature.conversation.ObserveUsersTypingUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class ObserveUsersTypingInConversationUseCaseTest {

    @Test
    fun given_UsersAreTyping_ThenObserveFlowAndReturnUIParticipant() = runTest {
        val userSummaryArg = UserSummary(
            expectedUIParticipant.id,
            expectedUIParticipant.name,
            expectedUIParticipant.handle,
            null,
            UserTypeInfo.Regular(UserType.NONE),
            false,
            ConnectionState.ACCEPTED,
            UserAvailabilityStatus.AVAILABLE,
            0
        )
        val (arrangement, useCase) = Arrangement()
            .withMapperFrom(userSummaryArg)
            .withObserveUsersTypingResult(setOf(userSummaryArg))
            .arrange()

        val result = useCase(ConversationId("id", "domain"))

        assertTrue(result.first().isNotEmpty())
        assertEquals(expectedUIParticipant, result.first().first())
        coVerify(exactly = 1) { arrangement.observeUsersTyping(any()) }
    }

    private class Arrangement {

        @MockK
        lateinit var observeUsersTyping: ObserveUsersTypingUseCase

        @MockK
        lateinit var uiParticipantMapper: UIParticipantMapper

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        fun withObserveUsersTypingResult(usersTyping: Set<UserSummary>) = apply {
            coEvery { observeUsersTyping(any()) } returns flowOf(usersTyping)
        }

        fun withMapperFrom(userSummary: UserSummary) = apply {
            every { uiParticipantMapper.toUIParticipant(eq(userSummary)) } returns expectedUIParticipant
        }

        fun arrange() = this to ObserveUsersTypingInConversationUseCase(
            observeUsersTyping, uiParticipantMapper
        )

        companion object {
            val expectedUIParticipant = UIParticipant(
                id = UserId("id", "domain"),
                name = "name",
                handle = "handle",
                isSelf = false,
                isService = false
            )
        }
    }
}
