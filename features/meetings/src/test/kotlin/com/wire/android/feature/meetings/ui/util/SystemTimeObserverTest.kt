/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.android.feature.meetings.ui.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SystemTimeObserverTest {

    @AfterEach
    fun tearDown() {
        unmockkStatic(::supportsTimeZoneOffsetChanges)
    }

    @Test
    fun givenSystemTimeObserver_whenCollected_thenRegistersWithoutEmittingAndUnregistersOnCancellation() = runTest {
        val (arrangement, observer) = Arrangement().arrange()

        observer().test {
            runCurrent()
            verify(exactly = 1) { arrangement.context.registerReceiver(any(), any()) }
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        verify(exactly = 1) { arrangement.context.unregisterReceiver(arrangement.receiver.captured) }
    }

    @Test
    fun givenSystemTimeObserver_whenSupportedBroadcastsArrive_thenEmitsImmediately() = runTest {
        val (arrangement, observer) = Arrangement().arrange()

        observer().test {
            runCurrent()
            listOf(
                Intent.ACTION_TIME_TICK,
                Intent.ACTION_DATE_CHANGED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,
            ).forEach { action ->
                assertTrue(action in observer.actions)
                arrangement.broadcast(action)
                assertEquals(Unit, awaitItem())
            }
            assertEquals(0L, testScheduler.currentTime)
        }
    }

    @Test
    fun givenSystemTimeObserver_whenUnrelatedOrNullBroadcastArrives_thenDoesNotEmit() = runTest {
        val (arrangement, observer) = Arrangement().arrange()

        observer().test {
            runCurrent()
            arrangement.broadcast(Intent.ACTION_SCREEN_ON)
            arrangement.receiver.captured.onReceive(arrangement.context, null)
            runCurrent()
            expectNoEvents()
        }
    }

    @Test
    fun givenAndroid14_whenCollected_thenOffsetActionIsNotRegisteredOrEmitted() = runTest {
        val (arrangement, observer) = Arrangement().arrange()

        observer().test {
            runCurrent()
            assertFalse(Intent.ACTION_TIMEZONE_OFFSET_CHANGED in observer.actions)
            arrangement.broadcast(Intent.ACTION_TIMEZONE_OFFSET_CHANGED)
            runCurrent()
            expectNoEvents()
        }
    }

    @Test
    fun givenAndroid17_whenCollected_thenOffsetActionIsRegisteredAndEmitted() = runTest {
        val (arrangement, observer) = Arrangement()
            .withTimeZoneOffsetChangesSupported(true)
            .arrange()

        observer().test {
            runCurrent()
            assertTrue(Intent.ACTION_TIMEZONE_OFFSET_CHANGED in observer.actions)
            arrangement.broadcast(Intent.ACTION_TIMEZONE_OFFSET_CHANGED)
            assertEquals(Unit, awaitItem())
        }
    }

    private class Arrangement {
        val context = mockk<Context>(relaxed = true)
        val receiver = slot<BroadcastReceiver>()

        init {
            mockkStatic(::supportsTimeZoneOffsetChanges)
            withTimeZoneOffsetChangesSupported(false)
            every { context.registerReceiver(capture(receiver), any()) } returns null
        }

        fun withTimeZoneOffsetChangesSupported(supported: Boolean) = apply {
            every { supportsTimeZoneOffsetChanges() } returns supported
        }

        fun broadcast(action: String) {
            val intent = mockk<Intent>()
            every { intent.action } returns action
            receiver.captured.onReceive(context, intent)
        }

        fun arrange() = this to SystemTimeObserver(context)
    }
}
