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
package com.wire.android.ui.home

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.kalium.logic.sync.ForegroundActionsUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
class AppSyncViewModelTest {
    private val testDispatcher = TestDispatcherProvider()

    @Test
    fun `when startSyncingAppConfig is called then it should call the use case`() = runTest(testDispatcher.io()) {
        val (arrangement, viewModel) = Arrangement().arrange(testDispatcher) {
            withForegroundActionsUseCase()
        }

        viewModel.startSyncingAppConfig()
        advanceUntilIdle()

        coVerify { arrangement.foregroundActionsUseCase.invoke() }
    }

    @Test
    fun `when startSyncingAppConfig is called multiple times then it should call the use case with delay`() = runTest(testDispatcher.io()) {
        val (arrangement, viewModel) = Arrangement().arrange(testDispatcher) {
            withForegroundActionsUseCase(1000)
        }

        viewModel.startSyncingAppConfig()
        viewModel.startSyncingAppConfig()
        viewModel.startSyncingAppConfig()
        advanceUntilIdle()

        coVerify(exactly = 1) { arrangement.foregroundActionsUseCase.invoke() }
    }

    private class Arrangement {

        @MockK
        lateinit var foregroundActionsUseCase: ForegroundActionsUseCase

        init {
            MockKAnnotations.init(this)
        }

        fun withForegroundActionsUseCase(delayMs: Long = 0) {
            coEvery { foregroundActionsUseCase.invoke() } coAnswers {
                delay(delayMs)
            }
        }

        fun arrange(testDispatcher: TestDispatcherProvider, block: Arrangement.() -> Unit) = apply(block).let {
            this to AppSyncViewModel(
                foregroundActionsUseCase = foregroundActionsUseCase,
                dispatcher = testDispatcher
            )
        }
    }
}
