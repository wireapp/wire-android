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

package com.wire.android.mdm

import android.content.Context
import android.content.Intent
import android.content.RestrictionsManager
import android.os.Bundle
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.mdm.model.MdmServerConfig
import com.wire.android.services.ServicesManager
import com.wire.kalium.logic.CoreLogic
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class MdmConfigurationManagerTest {

    private val context = mockk<Context>()
    private val restrictionsManager = mockk<RestrictionsManager>()
    private val json = Json.Default
    private val mdmConfigurationManager = MdmConfigurationManager(context, json)

    init {
        every { context.getSystemService(Context.RESTRICTIONS_SERVICE) } returns restrictionsManager
    }

    @Test
    fun `getCertificatePinningConfig returns empty map when no restrictions`() {
        every { restrictionsManager.applicationRestrictions } returns null

        val result = mdmConfigurationManager.getCertificatePinningConfig()

        assertEquals(emptyMap<String, List<String>>(), result)
    }

    @Test
    fun `getCertificatePinningConfig returns empty map when restrictions is empty`() {
        val emptyBundle = Bundle()
        every { restrictionsManager.applicationRestrictions } returns emptyBundle

        val result = mdmConfigurationManager.getCertificatePinningConfig()

        assertEquals(emptyMap<String, List<String>>(), result)
    }

    @Test
    fun `getCertificatePinningConfig returns parsed config when valid JSON`() {
        val bundle = Bundle().apply {
            putString("certificate_pinning_config", """{"sha256/ABC123": ["example.com", "api.example.com"]}""")
        }
        every { restrictionsManager.applicationRestrictions } returns bundle

        val result = mdmConfigurationManager.getCertificatePinningConfig()

        val expected = mapOf("sha256/ABC123" to listOf("example.com", "api.example.com"))
        assertEquals(expected, result)
    }

    @Test
    fun `getCertificatePinningConfig returns empty map when invalid JSON`() {
        val bundle = Bundle().apply {
            putString("certificate_pinning_config", "invalid json")
        }
        every { restrictionsManager.applicationRestrictions } returns bundle

        val result = mdmConfigurationManager.getCertificatePinningConfig()

        assertEquals(emptyMap<String, List<String>>(), result)
    }

    @Test
    fun `mergeCertificatePinningConfigs merges configs correctly`() {
        val defaultConfig = mapOf(
            "sha256/DEFAULT1" to listOf("default1.com"),
            "sha256/SHARED" to listOf("shared.com")
        )
        val mdmConfig = mapOf(
            "sha256/MDM1" to listOf("mdm1.com"),
            "sha256/SHARED" to listOf("shared-mdm.com")
        )

        val result = mdmConfigurationManager.mergeCertificatePinningConfigs(defaultConfig, mdmConfig)

        val expected = mapOf(
            "sha256/DEFAULT1" to listOf("default1.com"),
            "sha256/SHARED" to listOf("shared.com", "shared-mdm.com"),
            "sha256/MDM1" to listOf("mdm1.com")
        )
        assertEquals(expected, result)
    }

    @Test
    fun `mergeCertificatePinningConfigs removes duplicates`() {
        val defaultConfig = mapOf("sha256/TEST" to listOf("example.com", "duplicate.com"))
        val mdmConfig = mapOf("sha256/TEST" to listOf("duplicate.com", "new.com"))

        val result = mdmConfigurationManager.mergeCertificatePinningConfigs(defaultConfig, mdmConfig)

        val expected = mapOf("sha256/TEST" to listOf("example.com", "duplicate.com", "new.com"))
        assertEquals(expected, result)
    }

    @Test
    fun `getServerConfig returns null when no restrictions`() {
        every { restrictionsManager.applicationRestrictions } returns null

        val result = mdmConfigurationManager.getServerConfig()

        assertNull(result)
    }

    @Test
    fun `getServerConfig returns null when restrictions is empty`() {
        val emptyBundle = Bundle()
        every { restrictionsManager.applicationRestrictions } returns emptyBundle

        val result = mdmConfigurationManager.getServerConfig()

        assertNull(result)
    }

    @Test
    fun `getServerConfig returns parsed config when valid JSON`() {
        val serverConfigJson = """{
            "serverTitle": "Test Server",
            "serverUrl": "https://test.example.com",
            "isOnPremises": true
        }""".trimIndent()
        
        val bundle = Bundle().apply {
            putString("server_config", serverConfigJson)
        }
        every { restrictionsManager.applicationRestrictions } returns bundle

        val result = mdmConfigurationManager.getServerConfig()

        assertNotNull(result)
        assertEquals("Test Server", result?.serverTitle)
        assertEquals("https://test.example.com", result?.serverUrl)
        assertTrue(result?.isOnPremises == true)
    }

    @Test
    fun `getServerConfig returns null when invalid JSON`() {
        val bundle = Bundle().apply {
            putString("server_config", "invalid json")
        }
        every { restrictionsManager.applicationRestrictions } returns bundle

        val result = mdmConfigurationManager.getServerConfig()

        assertNull(result)
    }

    @Test
    fun `getServerConfig extracts from individual keys when server_config is null`() {
        val bundle = Bundle().apply {
            putString("server_title", "Individual Keys Server")
            putString("server_url", "https://individual.example.com")
            putString("federation_url", "https://federation.example.com")
            putBoolean("is_on_premises", true)
        }
        every { restrictionsManager.applicationRestrictions } returns bundle

        val result = mdmConfigurationManager.getServerConfig()

        assertNotNull(result)
        assertEquals("Individual Keys Server", result?.serverTitle)
        assertEquals("https://individual.example.com", result?.serverUrl)
        assertEquals("https://federation.example.com", result?.federationUrl)
        assertTrue(result?.isOnPremises == true)
    }

    @Test
    fun `getServerConfig returns null when no server_url or server_title in individual keys`() {
        val bundle = Bundle().apply {
            putString("federation_url", "https://federation.example.com")
            putBoolean("is_on_premises", true)
        }
        every { restrictionsManager.applicationRestrictions } returns bundle

        val result = mdmConfigurationManager.getServerConfig()

        assertNull(result)
    }

    @Test
    fun `validateServerConfig returns true for valid config`() {
        val validConfig = MdmServerConfig(
            serverTitle = "Valid Server",
            serverUrl = "https://valid.example.com",
            federationUrl = "https://federation.example.com",
            websocketUrl = "wss://websocket.example.com"
        )

        val result = mdmConfigurationManager.validateServerConfig(validConfig)

        assertTrue(result)
    }

    @Test
    fun `validateServerConfig returns false for invalid http URL`() {
        val invalidConfig = MdmServerConfig(
            serverUrl = "invalid-url"
        )

        val result = mdmConfigurationManager.validateServerConfig(invalidConfig)

        assertFalse(result)
    }

    @Test
    fun `validateServerConfig returns false for invalid websocket URL`() {
        val invalidConfig = MdmServerConfig(
            serverUrl = "https://valid.example.com",
            websocketUrl = "invalid-websocket-url"
        )

        val result = mdmConfigurationManager.validateServerConfig(invalidConfig)

        assertFalse(result)
    }

    @Test
    fun `validateServerConfig returns true for config with null URLs`() {
        val configWithNulls = MdmServerConfig(
            serverTitle = "Server with nulls",
            serverUrl = null,
            federationUrl = null,
            websocketUrl = null
        )

        val result = mdmConfigurationManager.validateServerConfig(configWithNulls)

        assertTrue(result)
    }

    @Test
    fun `getServerConfigAndNotify emits ServerConfigChanged event when config changes`() = runTest {
        val bundle = Bundle().apply {
            putString("server_title", "New Server")
            putString("server_url", "https://new.example.com")
        }
        every { restrictionsManager.applicationRestrictions } returns bundle

        val events = mutableListOf<MdmConfigurationEvent>()
        mdmConfigurationManager.configurationEvents.collect { event ->
            events.add(event)
        }

        val result = mdmConfigurationManager.getServerConfigAndNotify()

        assertNotNull(result)
        assertEquals("New Server", result?.serverTitle)
        assertTrue(events.any { it is MdmConfigurationEvent.ServerConfigChanged })
    }

    @Test
    fun `getServerConfigAndNotify emits ServerConfigCleared event when config is removed`() = runTest {
        val bundle = Bundle().apply {
            putString("server_title", "Test Server")
            putString("server_url", "https://test.example.com")
        }
        every { restrictionsManager.applicationRestrictions } returns bundle

        mdmConfigurationManager.getServerConfigAndNotify()

        every { restrictionsManager.applicationRestrictions } returns Bundle()

        val events = mutableListOf<MdmConfigurationEvent>()
        mdmConfigurationManager.configurationEvents.collect { event ->
            events.add(event)
        }

        val result = mdmConfigurationManager.getServerConfigAndNotify()

        assertNull(result)
        assertTrue(events.any { it is MdmConfigurationEvent.ServerConfigCleared })
    }
}