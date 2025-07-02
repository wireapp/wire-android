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

package com.wire.android.config

import com.wire.android.mdm.model.MdmServerConfig
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class DefaultServerConfigTest {

    @Test
    fun `withMdmConfig returns original config when mdmConfig is null`() {
        val originalConfig = DefaultServerConfig
        
        val result = originalConfig.withMdmConfig(null)
        
        assertEquals(originalConfig, result)
    }

    @Test
    fun `withMdmConfig overrides all fields when mdmConfig has all values`() {
        val mdmConfig = MdmServerConfig(
            serverTitle = "MDM Server",
            serverUrl = "https://mdm.example.com",
            federationUrl = "https://federation.mdm.example.com",
            websocketUrl = "wss://websocket.mdm.example.com",
            blacklistUrl = "https://blacklist.mdm.example.com",
            teamsUrl = "https://teams.mdm.example.com",
            accountsUrl = "https://accounts.mdm.example.com",
            websiteUrl = "https://website.mdm.example.com",
            isOnPremises = true
        )
        
        val result = DefaultServerConfig.withMdmConfig(mdmConfig)
        
        assertEquals("MDM Server", result.title)
        assertEquals("https://mdm.example.com", result.api)
        assertEquals("https://accounts.mdm.example.com", result.accounts)
        assertEquals("wss://websocket.mdm.example.com", result.webSocket)
        assertEquals("https://teams.mdm.example.com", result.teams)
        assertEquals("https://blacklist.mdm.example.com", result.blackList)
        assertEquals("https://website.mdm.example.com", result.website)
        assertTrue(result.isOnPremises)
    }

    @Test
    fun `withMdmConfig overrides only non-null fields from mdmConfig`() {
        val mdmConfig = MdmServerConfig(
            serverTitle = "Partial MDM Server",
            serverUrl = "https://partial.mdm.example.com",
            isOnPremises = true
        )
        
        val result = DefaultServerConfig.withMdmConfig(mdmConfig)
        
        assertEquals("Partial MDM Server", result.title)
        assertEquals("https://partial.mdm.example.com", result.api)
        assertTrue(result.isOnPremises)
        
        assertEquals(DefaultServerConfig.accounts, result.accounts)
        assertEquals(DefaultServerConfig.webSocket, result.webSocket)
        assertEquals(DefaultServerConfig.teams, result.teams)
        assertEquals(DefaultServerConfig.blackList, result.blackList)
        assertEquals(DefaultServerConfig.website, result.website)
    }

    @Test
    fun `createServerConfigWithMdm returns default config when mdmConfig is null`() {
        val result = createServerConfigWithMdm(null)
        
        assertEquals(DefaultServerConfig, result)
    }

    @Test
    fun `createServerConfigWithMdm returns mdm-enhanced config when mdmConfig provided`() {
        val mdmConfig = MdmServerConfig(
            serverTitle = "Enhanced Server",
            serverUrl = "https://enhanced.example.com"
        )
        
        val result = createServerConfigWithMdm(mdmConfig)
        
        assertEquals("Enhanced Server", result.title)
        assertEquals("https://enhanced.example.com", result.api)
    }

    @Test
    fun `server config priority - MDM overrides BuildConfig defaults`() {
        val mdmConfig = MdmServerConfig(
            serverTitle = "Priority Test Server",
            serverUrl = "https://priority.test.com",
            isOnPremises = true
        )
        
        val finalConfig = createServerConfigWithMdm(mdmConfig)
        
        assertEquals("Priority Test Server", finalConfig.title)
        assertEquals("https://priority.test.com", finalConfig.api)
        assertTrue(finalConfig.isOnPremises)
        
        assertNotEquals(DefaultServerConfig.title, finalConfig.title)
        assertNotEquals(DefaultServerConfig.api, finalConfig.api)
        assertNotEquals(DefaultServerConfig.isOnPremises, finalConfig.isOnPremises)
    }

    @Test
    fun `server config fallback - BuildConfig used when MDM config is incomplete`() {
        val incompleteMdmConfig = MdmServerConfig(
            serverTitle = "Incomplete Server"
        )
        
        val finalConfig = createServerConfigWithMdm(incompleteMdmConfig)
        
        assertEquals("Incomplete Server", finalConfig.title)
        assertEquals(DefaultServerConfig.api, finalConfig.api)
        assertEquals(DefaultServerConfig.accounts, finalConfig.accounts)
        assertEquals(DefaultServerConfig.webSocket, finalConfig.webSocket)
        assertEquals(DefaultServerConfig.teams, finalConfig.teams)
        assertEquals(DefaultServerConfig.blackList, finalConfig.blackList)
        assertEquals(DefaultServerConfig.website, finalConfig.website)
        assertEquals(DefaultServerConfig.isOnPremises, finalConfig.isOnPremises)
    }
}