/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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

import com.wire.android.config.CoroutineTestExtension
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
class CompositeMessagesViewModelTest {

    @Test
    fun `given pending button, when button is clicked, then do Nothing`() = runTest {
        // Arrange
        val (arrangement, viewModel) = Arrangement().arrange()
        val messageId = "messageId"
        val buttonId = "buttonId"
        viewModel.pendingButtons[messageId] = buttonId

        // Act
        viewModel.onButtonClicked(messageId, buttonId)

        // Assert
        // TODO assert that the use case is not called
    }

    @Test
    fun `given button nto pending, when button is clicked, then mark pending and then remove it once done`() = runTest {
        // Arrange
        val (arrangement, viewModel) = Arrangement().arrange()
        val messageId = "messageId"
        val buttonId = "buttonId"

        // Act
        viewModel.onButtonClicked(messageId, buttonId)
        assertTrue(viewModel.pendingButtons.containsKey(messageId))
        advanceUntilIdle()
        assertFalse(viewModel.pendingButtons.containsKey(messageId))

        // Assert
        // TODO assert that the use case is called called
    }

    private class Arrangement {

        private val viewModel = CompositeMessagesViewModel()

        fun arrange() = this to viewModel
    }
}
