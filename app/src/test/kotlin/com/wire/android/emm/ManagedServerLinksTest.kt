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

import android.app.Application
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ManagedServerLinksTest {

    @Test
    fun `given all valid URLs, then isValid should return true`() {
        val config = ManagedServerLinks(
            accountsURL = "https://accounts.wire.com",
            backendURL = "https://backend.wire.com",
            backendWSURL = "https://ws.wire.com",
            blackListURL = "https://blacklist.wire.com",
            teamsURL = "https://teams.wire.com",
            websiteURL = "https://wire.com"
        )

        assertTrue(config.isValid)
    }

    @Test
    fun `given invalid accountsURL, then isValid should return false`() {
        val config = ManagedServerLinks(
            accountsURL = "invalid-url",
            backendURL = "https://backend.wire.com",
            backendWSURL = "https://ws.wire.com",
            blackListURL = "https://blacklist.wire.com",
            teamsURL = "https://teams.wire.com",
            websiteURL = "https://wire.com"
        )

        assertFalse(config.isValid)
    }

    @Test
    fun `given invalid backendURL, then isValid should return false`() {
        val config = ManagedServerLinks(
            accountsURL = "https://accounts.wire.com",
            backendURL = "not-a-url",
            backendWSURL = "https://ws.wire.com",
            blackListURL = "https://blacklist.wire.com",
            teamsURL = "https://teams.wire.com",
            websiteURL = "https://wire.com"
        )

        assertFalse(config.isValid)
    }

    @Test
    fun `given invalid backendWSURL, then isValid should return false`() {
        val config = ManagedServerLinks(
            accountsURL = "https://accounts.wire.com",
            backendURL = "https://backend.wire.com",
            backendWSURL = "invalid",
            blackListURL = "https://blacklist.wire.com",
            teamsURL = "https://teams.wire.com",
            websiteURL = "https://wire.com"
        )

        assertFalse(config.isValid)
    }

    @Test
    fun `given invalid blackListURL, then isValid should return false`() {
        val config = ManagedServerLinks(
            accountsURL = "https://accounts.wire.com",
            backendURL = "https://backend.wire.com",
            backendWSURL = "https://ws.wire.com",
            blackListURL = "",
            teamsURL = "https://teams.wire.com",
            websiteURL = "https://wire.com"
        )

        assertFalse(config.isValid)
    }

    @Test
    fun `given invalid teamsURL, then isValid should return false`() {
        val config = ManagedServerLinks(
            accountsURL = "https://accounts.wire.com",
            backendURL = "https://backend.wire.com",
            backendWSURL = "https://ws.wire.com",
            blackListURL = "https://blacklist.wire.com",
            teamsURL = "ftp://teams.wire.com",
            websiteURL = "https://wire.com"
        )

        assertFalse(config.isValid)
    }

    @Test
    fun `given invalid websiteURL, then isValid should return false`() {
        val config = ManagedServerLinks(
            accountsURL = "https://accounts.wire.com",
            backendURL = "https://backend.wire.com",
            backendWSURL = "https://ws.wire.com",
            blackListURL = "https://blacklist.wire.com",
            teamsURL = "https://teams.wire.com",
            websiteURL = "wire"
        )

        assertFalse(config.isValid)
    }

    @Test
    fun `given all empty URLs, then isValid should return false`() {
        val config = ManagedServerLinks(
            accountsURL = "",
            backendURL = "",
            backendWSURL = "",
            blackListURL = "",
            teamsURL = "",
            websiteURL = ""
        )

        assertFalse(config.isValid)
    }
}
