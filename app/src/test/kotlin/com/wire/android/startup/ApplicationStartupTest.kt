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

package com.wire.android.startup

import com.wire.kalium.persistence.kmmSettings.MasterKeyInitializationException
import java.io.IOException
import java.security.GeneralSecurityException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ApplicationStartupTest {
    @Test
    fun `successful initialization enables normal startup`() {
        val startup = ApplicationStartup()
        assertEquals(StartupState.Initializing, startup.state)
        var initialized = false
        assertTrue(startup.initialize { initialized = true })
        assertTrue(initialized)
        assertEquals(StartupState.Ready, startup.state)
    }

    @Test
    fun `terminal key failure blocks startup including dependency wrappers`() {
        val keyFailure = MasterKeyInitializationException(GeneralSecurityException("test"))
        listOf(keyFailure, RuntimeException("DI initialization", keyFailure)).forEach { failure ->
            val startup = ApplicationStartup()
            assertFalse(startup.initialize { throw failure })
            assertEquals(StartupState.SecureStorageUnavailable, startup.state)
        }
    }

    @Test
    fun `unrelated failures remain visible`() {
        listOf(IllegalArgumentException("test"), IOException("preferences failure"), GeneralSecurityException("untyped"))
            .forEach { failure ->
                val startup = ApplicationStartup()
                val actual = assertThrows(failure.javaClass) { startup.initialize { throw failure } }
                assertSame(failure, actual)
                assertEquals(StartupState.Initializing, startup.state)
            }
    }

    @Test
    fun `fresh process state permits a new initialization attempt`() {
        val failed = ApplicationStartup()
        failed.initialize { throw MasterKeyInitializationException(IOException("test")) }
        val fresh = ApplicationStartup()
        assertTrue(fresh.initialize { })
        assertEquals(StartupState.Ready, fresh.state)
    }
}
