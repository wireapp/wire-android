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
package com.wire.android.services

import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class PendingMessagesForegroundSyncHandlerTest {

    @Test
    fun `given current session is valid when sending pending messages then sends only for current user`() = runTest {
        val currentUserId = UserId("currentUser", "wire.com")
        val inactiveUserId = UserId("inactiveUser", "wire.com")
        val sentPendingMessagesFor = mutableListOf<UserId>()
        val handler = PendingMessagesForegroundSyncHandler(
            currentSession = { CurrentSessionResult.Success(AccountInfo.Valid(currentUserId)) },
            sendPendingMessagesAfterForegroundSync = { sentPendingMessagesFor.add(it) }
        )

        handler.sendPendingMessagesForCurrentSession(currentUserId)

        assertEquals(listOf(currentUserId), sentPendingMessagesFor)
        assertFalse(sentPendingMessagesFor.contains(inactiveUserId))
    }

    @Test
    fun `given current session is for different user when sending pending messages then does not send`() = runTest {
        val currentUserId = UserId("currentUser", "wire.com")
        val scheduledUserId = UserId("scheduledUser", "wire.com")
        val sentPendingMessagesFor = mutableListOf<UserId>()
        val handler = PendingMessagesForegroundSyncHandler(
            currentSession = { CurrentSessionResult.Success(AccountInfo.Valid(currentUserId)) },
            sendPendingMessagesAfterForegroundSync = { sentPendingMessagesFor.add(it) }
        )

        handler.sendPendingMessagesForCurrentSession(scheduledUserId)

        assertEquals(emptyList<UserId>(), sentPendingMessagesFor)
    }

    @Test
    fun `given current session is invalid when sending pending messages then does not send`() = runTest {
        val currentUserId = UserId("currentUser", "wire.com")
        val sentPendingMessagesFor = mutableListOf<UserId>()
        val handler = PendingMessagesForegroundSyncHandler(
            currentSession = {
                CurrentSessionResult.Success(AccountInfo.Invalid(currentUserId, LogoutReason.SELF_SOFT_LOGOUT))
            },
            sendPendingMessagesAfterForegroundSync = { sentPendingMessagesFor.add(it) }
        )

        handler.sendPendingMessagesForCurrentSession(currentUserId)

        assertEquals(emptyList<UserId>(), sentPendingMessagesFor)
    }

    @Test
    fun `given current session is missing when sending pending messages then does not send`() = runTest {
        val sentPendingMessagesFor = mutableListOf<UserId>()
        val handler = PendingMessagesForegroundSyncHandler(
            currentSession = { CurrentSessionResult.Failure.SessionNotFound },
            sendPendingMessagesAfterForegroundSync = { sentPendingMessagesFor.add(it) }
        )

        handler.sendPendingMessagesForCurrentSession(UserId("currentUser", "wire.com"))

        assertEquals(emptyList<UserId>(), sentPendingMessagesFor)
    }

    @Test
    fun `given current session lookup fails when sending pending messages then does not send`() = runTest {
        val sentPendingMessagesFor = mutableListOf<UserId>()
        val handler = PendingMessagesForegroundSyncHandler(
            currentSession = { CurrentSessionResult.Failure.Generic(CoreFailure.Unknown(null)) },
            sendPendingMessagesAfterForegroundSync = { sentPendingMessagesFor.add(it) }
        )

        handler.sendPendingMessagesForCurrentSession(UserId("currentUser", "wire.com"))

        assertEquals(emptyList<UserId>(), sentPendingMessagesFor)
    }
}
