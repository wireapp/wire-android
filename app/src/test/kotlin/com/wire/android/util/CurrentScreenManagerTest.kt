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
package com.wire.android.util

import app.cash.turbine.test
import com.wire.android.assertions.shouldBeEqualTo
import com.wire.android.config.CoroutineTestExtension
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
class CurrentScreenManagerTest {

    @Test
    fun givenInitialState_whenThereIsNoStartEvent_thenAppShouldNotBeOnForeground() = runTest {
        val (_, currentScreenManager) = Arrangement()
            .withScreenStateFlow(MutableStateFlow(true))
            .arrange()

        currentScreenManager.isAppVisibleFlow().test {
            awaitItem() shouldBeEqualTo false
            expectNoEvents()
            cancel()
        }
    }

    @Test
    fun givenInitialState_whenThereIsAStartEvent_thenAppShouldBeOnForeground() = runTest {
        val (_, currentScreenManager) = Arrangement()
            .withScreenStateFlow(MutableStateFlow(true))
            .arrange()

        currentScreenManager.isAppVisibleFlow().test {
            awaitItem() shouldBeEqualTo false
            currentScreenManager.onStart(StubLifecycleOwner())
            awaitItem() shouldBeEqualTo true
            expectNoEvents()
            cancel()
        }
    }

    @Test
    fun givenTwoStarts_whenTheresASingleStop_shouldStillMarkAsAppOnForeground() = runTest {
        val (_, currentScreenManager) = Arrangement()
            .withScreenStateFlow(MutableStateFlow(true))
            .arrange()

        currentScreenManager.onStart(StubLifecycleOwner())
        currentScreenManager.onStart(StubLifecycleOwner())

        currentScreenManager.onStop(StubLifecycleOwner())

        currentScreenManager.isAppVisibleFlow().test {
            awaitItem() shouldBeEqualTo true
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun givenTwoStarts_whenTheresAreTwoStops_shouldNotBeOnTheForeground() = runTest {
        val (_, currentScreenManager) = Arrangement()
            .withScreenStateFlow(MutableStateFlow(true))
            .arrange()

        currentScreenManager.onStart(StubLifecycleOwner())
        currentScreenManager.onStart(StubLifecycleOwner())

        currentScreenManager.onStop(StubLifecycleOwner())
        currentScreenManager.onStop(StubLifecycleOwner())

        currentScreenManager.isAppVisibleFlow().test {
            awaitItem() shouldBeEqualTo false
            cancelAndIgnoreRemainingEvents()
        }
    }

    private class Arrangement {

        @MockK
        lateinit var screenStateObserver: ScreenStateObserver

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        fun withScreenStateFlow(flow: StateFlow<Boolean>) = apply {
            every { screenStateObserver.screenStateFlow } returns flow
        }

        fun arrange() = this to CurrentScreenManager(
            screenStateObserver
        )
    }
}
