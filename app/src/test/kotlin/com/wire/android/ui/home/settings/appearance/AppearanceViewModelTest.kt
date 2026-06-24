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
package com.wire.android.ui.home.settings.appearance

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.ui.home.conversations.messages.item.MessageSwipeAction
import com.wire.android.ui.theme.ThemeOption
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
class AppearanceViewModelTest {

    @Test
    fun `given theme option, when changing it, then should update global data store`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withEnterToSend(flowOf(false))
            .arrange()

        viewModel.selectThemeOption(ThemeOption.DARK)

        coVerify(exactly = 1) { arrangement.globalDataStore.setThemeOption(ThemeOption.DARK) }
    }

    @Test
    fun `given enter to send option, when changing it, then should update global data store`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withEnterToSend(flowOf(true))
            .arrange()

        viewModel.selectPressEnterToSendOption(false)

        coVerify(exactly = 1) { arrangement.globalDataStore.setEnterToSend(false) }
    }

    @Test
    fun `given stored swipe actions, when observing settings state, then should update state`() = runTest {
        val (_, viewModel) = Arrangement()
            .withEnterToSend(flowOf(true))
            .withMessageSwipeRightAction(MessageSwipeAction.DETAILS)
            .withMessageSwipeLeftAction(MessageSwipeAction.REPLY)
            .arrange()

        advanceUntilIdle()

        assertEquals(MessageSwipeAction.DETAILS, viewModel.state.messageSwipeRightAction)
        assertEquals(MessageSwipeAction.REPLY, viewModel.state.messageSwipeLeftAction)
    }

    @Test
    fun `given swipe right action, when changing it, then should update global data store`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withEnterToSend(flowOf(true))
            .arrange()

        viewModel.selectMessageSwipeRightAction(MessageSwipeAction.DETAILS)

        coVerify(exactly = 1) { arrangement.globalDataStore.setMessageSwipeRightAction(MessageSwipeAction.DETAILS) }
    }

    @Test
    fun `given swipe left action, when changing it, then should update global data store`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withEnterToSend(flowOf(true))
            .arrange()

        viewModel.selectMessageSwipeLeftAction(MessageSwipeAction.REPLY)

        coVerify(exactly = 1) { arrangement.globalDataStore.setMessageSwipeLeftAction(MessageSwipeAction.REPLY) }
    }

    @Test
    fun `given swipe right action already used on left, when changing right, then should swap actions`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withEnterToSend(flowOf(true))
            .arrange()

        viewModel.selectMessageSwipeRightAction(MessageSwipeAction.DEFAULT_LEFT)

        coVerify(exactly = 1) { arrangement.globalDataStore.setMessageSwipeRightAction(MessageSwipeAction.DEFAULT_LEFT) }
        coVerify(exactly = 1) { arrangement.globalDataStore.setMessageSwipeLeftAction(MessageSwipeAction.DEFAULT_RIGHT) }
    }

    @Test
    fun `given swipe left action already used on right, when changing left, then should swap actions`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withEnterToSend(flowOf(true))
            .arrange()

        viewModel.selectMessageSwipeLeftAction(MessageSwipeAction.DEFAULT_RIGHT)

        coVerify(exactly = 1) { arrangement.globalDataStore.setMessageSwipeLeftAction(MessageSwipeAction.DEFAULT_RIGHT) }
        coVerify(exactly = 1) { arrangement.globalDataStore.setMessageSwipeRightAction(MessageSwipeAction.DEFAULT_LEFT) }
    }

    private class Arrangement {
        @MockK
        lateinit var globalDataStore: GlobalDataStore

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            coEvery { globalDataStore.setThemeOption(any()) } returns Unit
            coEvery { globalDataStore.setMessageSwipeRightAction(any()) } returns Unit
            coEvery { globalDataStore.setMessageSwipeLeftAction(any()) } returns Unit
            every { globalDataStore.selectedThemeOptionFlow() } returns flowOf(ThemeOption.DARK)
            every { globalDataStore.messageSwipeRightActionFlow() } returns flowOf(MessageSwipeAction.DEFAULT_RIGHT)
            every { globalDataStore.messageSwipeLeftActionFlow() } returns flowOf(MessageSwipeAction.DEFAULT_LEFT)
        }

        fun withEnterToSend(result: Flow<Boolean>) = apply {
            every { globalDataStore.enterToSendFlow() } returns result
        }

        fun withMessageSwipeRightAction(action: MessageSwipeAction) = apply {
            every { globalDataStore.messageSwipeRightActionFlow() } returns flowOf(action)
        }

        fun withMessageSwipeLeftAction(action: MessageSwipeAction) = apply {
            every { globalDataStore.messageSwipeLeftActionFlow() } returns flowOf(action)
        }

        fun arrange() = this to CustomizationViewModel(globalDataStore)
    }
}
