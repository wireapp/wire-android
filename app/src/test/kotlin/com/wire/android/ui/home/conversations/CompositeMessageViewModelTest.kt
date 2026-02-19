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
package com.wire.android.ui.home.conversations

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.ScopedArgsTestExtension
import com.wire.android.di.scopedArgs
import com.wire.android.ui.home.conversations.model.CompositeMessageArgs
import com.wire.android.config.NavigationTestExtension
import com.ramcosta.composedestinations.generated.app.navArgs
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.message.composite.SendButtonActionMessageUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
@ExtendWith(ScopedArgsTestExtension::class)
@ExtendWith(NavigationTestExtension::class)
class CompositeMessageViewModelTest {

    @Test
    fun `given pending button, when button is clicked, then do Nothing`() = runTest {
        // Arrange
        val (arrangement, viewModel) = Arrangement().arrange()
        val buttonId = "buttonId"
        viewModel.pendingButtonId = buttonId

        // Act
        viewModel.sendButtonActionMessage(buttonId)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) {
            arrangement.sendButtonActionMessage(any(), any(), any())
        }
    }

    @Test
    fun `given button nto pending, when button is clicked, then mark pending and then remove it once done`() = runTest {
        // Arrange
        val (arrangement, viewModel) = Arrangement()
            .withButtonActionMessage(SendButtonActionMessageUseCase.Result.Success)
            .arrange()

        val buttonId = "buttonId"

        // Act
        viewModel.sendButtonActionMessage(buttonId)
        advanceUntilIdle()
        assertNull(viewModel.pendingButtonId)

        // Assert
        coVerify(exactly = 1) {
            arrangement.sendButtonActionMessage(any(), any(), any())
        }
    }

    private companion object {
        val CONVERSATION_ID = ConversationId("some-dummy-value", "some.dummy.domain")
        const val MESSAGE_ID = "message-id"
    }

    private class Arrangement {

        @MockK
        lateinit var sendButtonActionMessage: SendButtonActionMessageUseCase

        @MockK
        lateinit var savedStateHandle: SavedStateHandle

        init {
            MockKAnnotations.init(this)
            every { savedStateHandle.navArgs<ConversationNavArgs>() } returns ConversationNavArgs(CONVERSATION_ID)
            every { savedStateHandle.scopedArgs<CompositeMessageArgs>() } returns CompositeMessageArgs(MESSAGE_ID)
        }

        private val viewModel = CompositeMessageViewModelImpl(sendButtonActionMessage, savedStateHandle)

        fun withButtonActionMessage(
            result: SendButtonActionMessageUseCase.Result
        ) = apply {
            coEvery { sendButtonActionMessage(any(), any(), any()) } returns result
        }

        fun arrange() = this to viewModel
    }
}
