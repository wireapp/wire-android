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
package com.wire.android.ui.home.conversations.typing

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.ScopedArgsTestExtension
import com.wire.android.di.scopedArgs
import com.wire.android.framework.TestConversation
import com.wire.android.ui.home.conversations.details.participants.model.UIParticipant
import com.wire.android.ui.home.conversations.typing.TypingIndicatorViewModelTest.Arrangement.Companion.expectedUIParticipant
import com.wire.android.ui.home.conversations.usecase.ObserveUsersTypingInConversationUseCase
import com.wire.kalium.logic.data.user.UserId
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
@ExtendWith(ScopedArgsTestExtension::class)
class TypingIndicatorViewModelTest {

    @Test
    fun `given a conversation, when start observing, then call the right use case for observation`() = runTest {
        // Given
        val (arrangement, _) = Arrangement()
            .withParticipantsTyping(listOf(expectedUIParticipant))
            .arrange()

        // Then
        coVerify { arrangement.observeUsersTypingInConversation(TestConversation.ID) }
        arrangement.observeUsersTypingInConversation(TestConversation.ID).test {
            val participants = awaitItem()
            assertEquals(expectedUIParticipant, participants.first())
            awaitComplete()
        }
    }

    private class Arrangement {

        @MockK
        lateinit var observeUsersTypingInConversation: ObserveUsersTypingInConversationUseCase

        @MockK
        lateinit var savedStateHandle: SavedStateHandle

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { savedStateHandle.scopedArgs<TypingIndicatorArgs>() } returns TypingIndicatorArgs(conversationId = TestConversation.ID)
            coEvery { observeUsersTypingInConversation(eq(TestConversation.ID)) } returns flowOf(emptyList())
        }

        fun withParticipantsTyping(usersTyping: List<UIParticipant> = emptyList()) = apply {
            coEvery { observeUsersTypingInConversation(eq(TestConversation.ID)) } returns flowOf(usersTyping)
        }

        fun arrange() = this to TypingIndicatorViewModelImpl(observeUsersTypingInConversation, savedStateHandle)

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
