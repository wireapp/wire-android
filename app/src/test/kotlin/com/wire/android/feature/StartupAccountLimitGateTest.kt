/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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
    fun `three accounts with reduced limit of one trigger warning`() {
        assertTrue(StartupAccountLimitGate().shouldShow(accounts, currentUserId, 1, taskId = 1))
    }

    @Test
    fun `at or below limit does not trigger warning`() {
        for (limit in listOf(3, 4)) {
            assertFalse(StartupAccountLimitGate().shouldShow(accounts, currentUserId, limit, taskId = 1))
        }
    }

    @Test
    fun `invalid accounts do not count towards limit`() {
        val sessions = listOf(
            accounts.first(),
            AccountInfo.Invalid(UserId("invalid", "wire.com"), LogoutReason.SESSION_EXPIRED),
        )
        assertFalse(StartupAccountLimitGate().shouldShow(sessions, currentUserId, 1, taskId = 1))
    }

    @Test
    fun `absent valid current account does not trigger warning`() {
        assertFalse(StartupAccountLimitGate().shouldShow(accounts, null, 1, taskId = 1))
        assertFalse(StartupAccountLimitGate().shouldShow(accounts, UserId("missing", "wire.com"), 1, taskId = 1))
        assertFalse(StartupAccountLimitGate().shouldShow(emptyList(), currentUserId, 1, taskId = 1))
    }

    @Test
    fun `same task does not repeat warning but fresh process does`() {
        val gate = StartupAccountLimitGate()
        assertTrue(gate.shouldShow(accounts, currentUserId, 1, taskId = 1))
        assertFalse(gate.shouldShow(accounts, currentUserId, 1, taskId = 1))
        assertFalse(gate.shouldShow(accounts, accounts[1].userId, 1, taskId = 1))
        assertTrue(StartupAccountLimitGate().shouldShow(accounts, currentUserId, 1, taskId = 1))
    }

    @Test
    fun `new main task can show warning again within the same process`() {
        val gate = StartupAccountLimitGate()
        assertTrue(gate.shouldShow(accounts, currentUserId, 1, taskId = 10))
        assertFalse(gate.shouldShow(accounts, currentUserId, 1, taskId = 10))
        assertTrue(gate.shouldShow(accounts, currentUserId, 1, taskId = 11))
        assertFalse(gate.shouldShow(accounts, currentUserId, 1, taskId = 11))
        assertFalse(gate.shouldShow(accounts, currentUserId, 1, taskId = 10))
    }

    @Test
    fun `new task rechecks the current count rather than reusing previous eligibility`() {
        val gate = StartupAccountLimitGate()
        assertTrue(gate.shouldShow(accounts, currentUserId, 1, taskId = 10))
        assertFalse(gate.shouldShow(accounts.take(1), currentUserId, 1, taskId = 11))
    }
}
