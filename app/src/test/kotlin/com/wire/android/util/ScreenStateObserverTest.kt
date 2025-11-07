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

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import app.cash.turbine.test
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import com.wire.android.assertions.shouldBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ScreenStateObserverTest {

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `given screen is initially on, when observing screen state, then initial value should be emit true`() = runTest {
        val (arrangement, screenStateObserver) =
            Arrangement()
                .withScreenInitiallyOn()
                .arrange()

        screenStateObserver.screenStateFlow.test {
            awaitItem() shouldBeEqualTo true
            val intent = Intent(Intent.ACTION_SCREEN_OFF)
            screenStateObserver.onReceive(arrangement.context, intent)
        }
    }

    @Test
    fun `given screen is initially off, when observing screen state, then initial value should be emit false`() = runTest {
        val (arrangement, screenStateObserver) =
            Arrangement()
                .withScreenInitiallyOn()
                .arrange()

        screenStateObserver.screenStateFlow.test {
            awaitItem() shouldBeEqualTo true
            val intent = Intent(Intent.ACTION_SCREEN_OFF)
            screenStateObserver.onReceive(arrangement.context, intent)
        }
    }

    @Test
    fun `given a intent action screen on, when observing screen state, should emit true`() = runTest {
        val (arrangement, screenStateObserver) =
            Arrangement()
                .withScreenInitiallyOff()
                .arrange()

        screenStateObserver.screenStateFlow.test {
            awaitItem() // Ignore initial state
            val intent = createIntent(Intent.ACTION_SCREEN_ON)
            println("INTENT WITH ACTION ${intent.action}")
            screenStateObserver.onReceive(arrangement.context, intent)
            advanceUntilIdle()
            awaitItem() shouldBeEqualTo true
        }
    }

    @Test
    fun `given a intent action screen off, when observing screen state, should emit false`() = runTest {
        val (arrangement, screenStateObserver) =
            Arrangement()
                .withScreenInitiallyOn()
                .arrange()

        screenStateObserver.screenStateFlow.test {
            awaitItem() // Ignore initial state
            val intent = createIntent(Intent.ACTION_SCREEN_OFF)
            println("INTENT WITH ACTION ${intent.action}")
            screenStateObserver.onReceive(arrangement.context, intent)
            advanceUntilIdle()
            awaitItem() shouldBeEqualTo false
        }
    }

    private fun createIntent(action: String) = mockk<Intent>().also {
        every { it.action } returns action
    }

    private class Arrangement {

        val context: Context = mockk()

        val powerManager: PowerManager = mockk()

        init {
            // Intent result is not used by ScreenStateObserver
            every { context.getSystemService(Context.POWER_SERVICE) } returns powerManager
            every { context.registerReceiver(any(), any()) } returns Intent()
        }

        val screenStateObserver: ScreenStateObserver by lazy {
            ScreenStateObserver(
                context = context
            )
        }

        fun withScreenInitiallyOff() = apply {
            every { powerManager.isInteractive } returns false
        }

        fun withScreenInitiallyOn() = apply {
            every { powerManager.isInteractive } returns true
        }

        fun arrange() = this to screenStateObserver
    }
}
