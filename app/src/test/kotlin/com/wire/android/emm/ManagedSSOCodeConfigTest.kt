/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.emm

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManagedSSOCodeConfigTest {

    @Test
    fun `given a valid UUID SSO code, then isValid should return true`() {
        val validSSOCode = "fd994b20-b9af-11ec-ae36-00163e9b33ca"
        val config = ManagedSSOCodeConfig(validSSOCode)

        assertTrue(config.isValid)
    }

    @Test
    fun `given an invalid SSO code, then isValid should return false`() {
        val invalidSSOCode = "invalid-sso-code"
        val config = ManagedSSOCodeConfig(invalidSSOCode)

        assertFalse(config.isValid)
    }

    @Test
    fun `given an empty SSO code, then isValid should return false`() {
        val emptySSOCode = ""
        val config = ManagedSSOCodeConfig(emptySSOCode)

        assertFalse(config.isValid)
    }

    @Test
    fun `given a partial UUID SSO code, then isValid should return false`() {
        val partialUUID = "fd994b20-b9af-11ec"
        val config = ManagedSSOCodeConfig(partialUUID)

        assertFalse(config.isValid)
    }

    @Test
    fun `given a UUID with incorrect format, then isValid should return false`() {
        val incorrectFormat = "fd994b20b9af11ecae3600163e9b33ca"
        val config = ManagedSSOCodeConfig(incorrectFormat)

        assertFalse(config.isValid)
    }

    @Test
    fun `given a UUID in uppercase, then isValid should return true`() {
        val uppercaseUUID = "FD994B20-B9AF-11EC-AE36-00163E9B33CA"
        val config = ManagedSSOCodeConfig(uppercaseUUID)

        assertTrue(config.isValid)
    }
}
