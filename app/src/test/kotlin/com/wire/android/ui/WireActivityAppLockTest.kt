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
package com.wire.android.ui

import com.wire.kalium.logic.data.user.UserId
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class WireActivityAppLockTest {

    @Test
    fun givenAppIsLockedAndUserIdIsDelayed_whenUserIdLoads_thenAppLockUserIdIsEmitted() = runTest {
        val isAppLocked = MutableStateFlow(true)
        val currentUserId = MutableStateFlow<UserId?>(null)
        val expectedUserId = UserId("user", "domain")

        val result = async {
            observeAppLockUserId(isAppLocked, currentUserId).first()
        }
        runCurrent()
        assertFalse(result.isCompleted)

        currentUserId.value = expectedUserId

        assertEquals(expectedUserId, result.await())
    }
}
