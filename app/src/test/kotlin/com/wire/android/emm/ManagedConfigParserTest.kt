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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ManagedConfigParserTest {

    // region Server Config - Unified Format

    @Test
    fun `given unified server config, then parse correctly`() {
        val parser = createParser(userIdKey = "0")
        val result = parser.parseServerConfig(UNIFIED_SERVER_CONFIG)

        assertNotNull(result)
        assertEquals("anta.wire.link", result!!.title)
        assertEquals("https://account.anta.wire.link", result.endpoints.accountsURL)
        assertEquals("https://nginz-https.anta.wire.link", result.endpoints.backendURL)
        assertEquals("https://nginz-ssl.anta.wire.link", result.endpoints.backendWSURL)
        assertEquals("https://disallowed-clients.anta.wire.link", result.endpoints.blackListURL)
        assertEquals("https://teams.anta.wire.link", result.endpoints.teamsURL)
        assertEquals("https://wire.com", result.endpoints.websiteURL)
    }

    // endregion

    // region Server Config - Context-Mapped Format

    @Test
    fun `given context-mapped server config with matching user ID, then return correct config`() {
        val parser = createParser(userIdKey = "0")
        val result = parser.parseServerConfig(CONTEXT_MAPPED_SERVER_CONFIG)

        assertNotNull(result)
        assertEquals("Secure Server", result!!.title)
        assertEquals("https://secure-account.wire.link", result.endpoints.accountsURL)
    }

    @Test
    fun `given context-mapped server config with non-matching user ID, then fallback to default`() {
        val parser = createParser(userIdKey = "99") // User ID not in config
        val result = parser.parseServerConfig(CONTEXT_MAPPED_SERVER_CONFIG)

        assertNotNull(result)
        assertEquals("General Server", result!!.title)
        assertEquals("https://general-account.wire.link", result.endpoints.accountsURL)
    }

    @Test
    fun `given context-mapped server config without default and non-matching user ID, then return null`() {
        val parser = createParser(userIdKey = "99")
        val result = parser.parseServerConfig(CONTEXT_MAPPED_SERVER_CONFIG_NO_DEFAULT)

        assertNull(result)
    }

    @Test
    fun `given context-mapped server config with only default, then use default for any user`() {
        val parser = createParser(userIdKey = "42")
        val result = parser.parseServerConfig(CONTEXT_MAPPED_SERVER_CONFIG_ONLY_DEFAULT)

        assertNotNull(result)
        assertEquals("Default Server", result!!.title)
    }

    // endregion

    // region SSO Config - Unified Format

    @Test
    fun `given unified SSO config, then parse correctly`() {
        val parser = createParser(userIdKey = "0")
        val result = parser.parseSSOCodeConfig(UNIFIED_SSO_CONFIG)

        assertNotNull(result)
        assertEquals("fd994b20-b9af-11ec-ae36-00163e9b33ca", result!!.ssoCode)
    }

    // endregion

    // region SSO Config - Context-Mapped Format

    @Test
    fun `given context-mapped SSO config with matching user ID, then return correct config`() {
        val parser = createParser(userIdKey = "0")
        val result = parser.parseSSOCodeConfig(CONTEXT_MAPPED_SSO_CONFIG)

        assertNotNull(result)
        assertEquals("secure-sso-code-0000-0000-000000000000", result!!.ssoCode)
    }

    @Test
    fun `given context-mapped SSO config with non-matching user ID, then fallback to default`() {
        val parser = createParser(userIdKey = "99")
        val result = parser.parseSSOCodeConfig(CONTEXT_MAPPED_SSO_CONFIG)

        assertNotNull(result)
        assertEquals("default-sso-code-0000-0000-000000000000", result!!.ssoCode)
    }

    @Test
    fun `given context-mapped SSO config without default and non-matching user ID, then return null`() {
        val parser = createParser(userIdKey = "99")
        val result = parser.parseSSOCodeConfig(CONTEXT_MAPPED_SSO_CONFIG_NO_DEFAULT)

        assertNull(result)
    }

    // endregion

    // region Invalid JSON

    @Test
    fun `given invalid JSON, then throw InvalidManagedConfig`() {
        val parser = createParser(userIdKey = "0")

        assertThrows(InvalidManagedConfig::class.java) {
            parser.parseServerConfig("invalid json")
        }
    }

    @Test
    fun `given empty JSON object for server config, then return null as no context matched`() {
        val parser = createParser(userIdKey = "0")
        // Empty JSON object {} has no keys matching user ID or "default"
        val result = parser.parseServerConfig("{}")
        assertNull(result)
    }

    @Test
    fun `given malformed server config with partial unified format, then return null`() {
        val parser = createParser(userIdKey = "0")
        // This JSON has title but no endpoints - doesn't match unified format, falls through to context-mapped
        // Since it has no matching context keys, returns null
        val malformedJson = """{"title": "Test"}"""

        val result = parser.parseServerConfig(malformedJson)
        assertNull(result)
    }

    @Test
    fun `given completely invalid JSON structure, then throw InvalidManagedConfig`() {
        val parser = createParser(userIdKey = "0")
        // Not a valid JSON object structure
        assertThrows(InvalidManagedConfig::class.java) {
            parser.parseServerConfig("not json at all")
        }
    }

    // endregion

    // region Helper Methods

    private fun createParser(userIdKey: String): ManagedConfigParser {
        return ManagedConfigParserImpl(
            userContextProvider = object : AndroidUserContextProvider {
                override fun getCurrentAndroidUserId(): Int = userIdKey.toIntOrNull() ?: 0
                override fun getCurrentUserIdKey(): String = userIdKey
            }
        )
    }

    // endregion

    companion object {
        val UNIFIED_SERVER_CONFIG = """
            {
              "endpoints": {
                "accountsURL": "https://account.anta.wire.link",
                "backendURL": "https://nginz-https.anta.wire.link",
                "backendWSURL": "https://nginz-ssl.anta.wire.link",
                "blackListURL": "https://disallowed-clients.anta.wire.link",
                "teamsURL": "https://teams.anta.wire.link",
                "websiteURL": "https://wire.com"
              },
              "title": "anta.wire.link"
            }
        """.trimIndent()

        val CONTEXT_MAPPED_SERVER_CONFIG = """
            {
              "0": {
                "title": "Secure Server",
                "endpoints": {
                  "accountsURL": "https://secure-account.wire.link",
                  "backendURL": "https://secure-api.wire.link",
                  "backendWSURL": "https://secure-ws.wire.link",
                  "blackListURL": "https://secure-blacklist.wire.link",
                  "teamsURL": "https://secure-teams.wire.link",
                  "websiteURL": "https://secure.wire.com"
                }
              },
              "default": {
                "title": "General Server",
                "endpoints": {
                  "accountsURL": "https://general-account.wire.link",
                  "backendURL": "https://general-api.wire.link",
                  "backendWSURL": "https://general-ws.wire.link",
                  "blackListURL": "https://general-blacklist.wire.link",
                  "teamsURL": "https://general-teams.wire.link",
                  "websiteURL": "https://general.wire.com"
                }
              }
            }
        """.trimIndent()

        val CONTEXT_MAPPED_SERVER_CONFIG_NO_DEFAULT = """
            {
              "0": {
                "title": "Secure Server",
                "endpoints": {
                  "accountsURL": "https://secure-account.wire.link",
                  "backendURL": "https://secure-api.wire.link",
                  "backendWSURL": "https://secure-ws.wire.link",
                  "blackListURL": "https://secure-blacklist.wire.link",
                  "teamsURL": "https://secure-teams.wire.link",
                  "websiteURL": "https://secure.wire.com"
                }
              },
              "10": {
                "title": "Work Profile Server",
                "endpoints": {
                  "accountsURL": "https://work-account.wire.link",
                  "backendURL": "https://work-api.wire.link",
                  "backendWSURL": "https://work-ws.wire.link",
                  "blackListURL": "https://work-blacklist.wire.link",
                  "teamsURL": "https://work-teams.wire.link",
                  "websiteURL": "https://work.wire.com"
                }
              }
            }
        """.trimIndent()

        val CONTEXT_MAPPED_SERVER_CONFIG_ONLY_DEFAULT = """
            {
              "default": {
                "title": "Default Server",
                "endpoints": {
                  "accountsURL": "https://default-account.wire.link",
                  "backendURL": "https://default-api.wire.link",
                  "backendWSURL": "https://default-ws.wire.link",
                  "blackListURL": "https://default-blacklist.wire.link",
                  "teamsURL": "https://default-teams.wire.link",
                  "websiteURL": "https://default.wire.com"
                }
              }
            }
        """.trimIndent()

        val UNIFIED_SSO_CONFIG = """
            {
              "sso_code": "fd994b20-b9af-11ec-ae36-00163e9b33ca"
            }
        """.trimIndent()

        val CONTEXT_MAPPED_SSO_CONFIG = """
            {
              "0": {
                "sso_code": "secure-sso-code-0000-0000-000000000000"
              },
              "default": {
                "sso_code": "default-sso-code-0000-0000-000000000000"
              }
            }
        """.trimIndent()

        val CONTEXT_MAPPED_SSO_CONFIG_NO_DEFAULT = """
            {
              "0": {
                "sso_code": "secure-sso-code-0000-0000-000000000000"
              }
            }
        """.trimIndent()
    }
}
