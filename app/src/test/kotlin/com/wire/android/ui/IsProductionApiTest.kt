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

import com.wire.kalium.logic.configuration.server.ServerConfig
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class IsProductionApiTest {

    @Test
    fun `given production API URL, then returns true`() {
        val links = serverLinks(api = ServerConfig.PRODUCTION.api)
        assertTrue(links.isProductionApi())
    }

    @Test
    fun `given production API URL with trailing path, then returns true`() {
        val links = serverLinks(api = "https://prod-nginz-https.wire.com/some/path")
        assertTrue(links.isProductionApi())
    }

    @Test
    fun `given custom on-premises API URL, then returns false`() {
        val links = serverLinks(api = "https://custom-backend.example.com")
        assertFalse(links.isProductionApi())
    }

    @Test
    fun `given staging API URL, then returns false`() {
        val links = serverLinks(api = ServerConfig.STAGING.api)
        assertFalse(links.isProductionApi())
    }

    @Test
    fun `given URL with production host as substring, then returns false`() {
        val links = serverLinks(api = "https://not-prod-nginz-https.wire.com.evil.com")
        assertFalse(links.isProductionApi())
    }

    private fun serverLinks(api: String) = ServerConfig.Links(
        api = api,
        accounts = "https://accounts.example.com",
        webSocket = "https://ws.example.com",
        blackList = "https://blacklist.example.com",
        teams = "https://teams.example.com",
        website = "https://example.com",
        title = "test",
        isOnPremises = false,
        apiProxy = null
    )
}
