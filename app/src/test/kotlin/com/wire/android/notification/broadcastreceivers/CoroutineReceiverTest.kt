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
package com.wire.android.notification.broadcastreceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wire.android.config.CoroutineTestExtension
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
class CoroutineReceiverTest {
    @Test
    fun `completion hook runs after finishing broadcast`() = runTest {
        val events = mutableListOf<String>()
        val receiver = receiver(events, receiveDelay = 1)

        receiver.onReceive(mockk(), mockk())
        advanceUntilIdle()

        assertEquals(listOf("received", "finished", "cleanup"), events)
    }

    @Test
    fun `timeout finishes broadcast before suspending cleanup`() = runTest {
        val events = mutableListOf<String>()
        val receiver = receiver(events, receiveDelay = 10_000)

        receiver.onReceive(mockk(), mockk())
        advanceUntilIdle()

        assertEquals(listOf("timeout", "finished", "cleanup"), events)
    }

    private fun receiver(events: MutableList<String>, receiveDelay: Long): CoroutineReceiver {
        val result = mockk<BroadcastReceiver.PendingResult>()
        every { result.finish() } answers { events.add("finished") }
        val receiver = spyk(object : CoroutineReceiver() {
            override suspend fun receive(context: Context, intent: Intent) {
                delay(receiveDelay)
                events.add("received")
            }
            override suspend fun onReceiveFinished(context: Context, intent: Intent) {
                delay(1)
                events.add("cleanup")
            }
            override fun onTimeout(context: Context, intent: Intent, exception: Exception) {
                events.add("timeout")
            }
        })
        every { receiver.goAsync() } returns result
        return receiver
    }
}
