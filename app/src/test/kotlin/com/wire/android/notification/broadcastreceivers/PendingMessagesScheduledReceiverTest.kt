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

import android.content.Context
import android.content.Intent
import com.wire.android.services.ServicesManager
import com.wire.kalium.logic.sync.PendingMessagesForegroundSync
import io.mockk.every
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class PendingMessagesScheduledReceiverTest {

    @Test
    fun `given scheduled pending messages broadcast without user id when received then starts foreground service`() = runTest {
        val (arrangement, receiver) = Arrangement().arrange()

        receiver.onReceive(arrangement.context, scheduledIntent())

        verify(exactly = 1) { arrangement.servicesManager.startPendingMessagesForegroundService() }
    }

    @Test
    fun `given unexpected broadcast when received then does not start foreground service`() = runTest {
        val (arrangement, receiver) = Arrangement().arrange()

        receiver.onReceive(arrangement.context, mockk { every { action } returns "unexpected-action" })

        verify(exactly = 0) { arrangement.servicesManager.startPendingMessagesForegroundService() }
    }

    @Test
    fun `given cancelled pending messages broadcast when received then stops foreground service`() = runTest {
        val (arrangement, receiver) = Arrangement().arrange()

        receiver.onReceive(
            context = arrangement.context,
            intent = cancelledIntent()
        )

        verify(exactly = 1) { arrangement.servicesManager.stopPendingMessagesForegroundService() }
        verify(exactly = 0) { arrangement.servicesManager.startPendingMessagesForegroundService() }
    }

    private class Arrangement {

        @MockK
        lateinit var context: Context

        @MockK(relaxUnitFun = true)
        lateinit var servicesManager: ServicesManager

        init {
            MockKAnnotations.init(this)
        }

        fun arrange(): Pair<Arrangement, PendingMessagesScheduledReceiver> =
            this to PendingMessagesScheduledReceiver(servicesManager)
    }

    private companion object {
        fun scheduledIntent(): Intent =
            mockk {
                every { action } returns PendingMessagesForegroundSync.ACTION_SENDING_OF_PENDING_MESSAGES_SCHEDULED
            }

        fun cancelledIntent(): Intent =
            mockk {
                every { action } returns PendingMessagesForegroundSync.ACTION_SENDING_OF_PENDING_MESSAGES_CANCELLED
            }
    }
}
