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

package com.wire.android.feature

import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.user.UserId
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StartupAccountLimitGateTest {
    private val currentUserId = UserId("current", "wire.com")
    private val accounts = listOf(
        AccountInfo.Valid(currentUserId),
        AccountInfo.Valid(UserId("second", "wire.com")),
        AccountInfo.Valid(UserId("third", "wire.com")),
    )

    @Test
    fun `above limit warns once for the same task`() {
        val gate = StartupAccountLimitGate()
        assertTrue(gate.shouldShow(accounts, currentUserId, 1, taskId = 1))
        assertFalse(gate.shouldShow(accounts, currentUserId, 1, taskId = 1))
        assertFalse(gate.shouldShow(accounts, accounts[1].userId, 1, taskId = 1))
    }

    @Test
    fun `at or below limit does not warn`() {
        for (limit in listOf(3, 4)) {
            assertFalse(StartupAccountLimitGate().shouldShow(accounts, currentUserId, limit, taskId = 1))
        }
    }

    @Test
    fun `invalid accounts do not count`() {
        val sessions = listOf(
            accounts.first(),
            AccountInfo.Invalid(UserId("invalid", "wire.com"), LogoutReason.SESSION_EXPIRED),
        )
        assertFalse(StartupAccountLimitGate().shouldShow(sessions, currentUserId, 1, taskId = 1))
    }

    @Test
    fun `current account must be valid and present`() {
        assertFalse(StartupAccountLimitGate().shouldShow(accounts, null, 1, taskId = 1))
        assertFalse(StartupAccountLimitGate().shouldShow(accounts, UserId("missing", "wire.com"), 1, taskId = 1))
        assertFalse(StartupAccountLimitGate().shouldShow(emptyList(), currentUserId, 1, taskId = 1))
    }

    @Test
    fun `new task and new process reevaluate current count`() {
        val gate = StartupAccountLimitGate()
        assertTrue(gate.shouldShow(accounts, currentUserId, 1, taskId = 10))
        assertFalse(gate.shouldShow(accounts, currentUserId, 1, taskId = 10))
        assertTrue(gate.shouldShow(accounts, currentUserId, 1, taskId = 11))
        assertFalse(gate.shouldShow(accounts.take(1), currentUserId, 1, taskId = 12))
        assertTrue(StartupAccountLimitGate().shouldShow(accounts, currentUserId, 1, taskId = 10))
    }
}
