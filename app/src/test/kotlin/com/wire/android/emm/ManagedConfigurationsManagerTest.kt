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
import android.content.Context
import android.content.RestrictionsManager
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.wire.android.config.ServerConfigProvider
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.util.EMPTY
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.jupiter.api.assertInstanceOf
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ManagedConfigurationsManagerTest {

    // region Unified Format Tests (Backward Compatibility)

    @Test
    fun `given a server config is valid, then parse it to a corresponding ManagedServerConfig`() =
        runTest {
            val expected = ManagedServerConfig(
                endpoints = ManagedServerLinks(
                    accountsURL = "https://account.anta.wire.link",
                    backendURL = "https://nginz-https.anta.wire.link",
                    backendWSURL = "https://nginz-ssl.anta.wire.link",
                    blackListURL = "https://disallowed-clients.anta.wire.link",
                    teamsURL = "https://teams.anta.wire.link",
                    websiteURL = "https://wire.com"
                ),
                title = "anta.wire.link"
            )
            val (_, manager) = Arrangement()
                .withRestrictions(mapOf(ManagedConfigurationsKeys.DEFAULT_SERVER_URLS.asKey() to validServerConfigJson))
                .arrange()

            val result = manager.refreshServerConfig()
            assertInstanceOf<ServerConfigResult.Success>(result)

            val serverConfig = manager.currentServerConfig
            assertEquals(expected.title, serverConfig.title)
            assertEquals(expected.endpoints.accountsURL, serverConfig.accounts)
            assertEquals(expected.endpoints.backendURL, serverConfig.api)
            assertEquals(expected.endpoints.backendWSURL, serverConfig.webSocket)
            assertEquals(expected.endpoints.blackListURL, serverConfig.blackList)
            assertEquals(expected.endpoints.teamsURL, serverConfig.teams)
            assertEquals(expected.endpoints.websiteURL, serverConfig.website)
        }

    @Test
    fun `given an invalid server config, then return null`() = runTest {
        val (_, manager) = Arrangement()
            .withRestrictions(mapOf(ManagedConfigurationsKeys.DEFAULT_SERVER_URLS.asKey() to "invalid json"))
            .arrange()

        val result = manager.refreshServerConfig()
        assertInstanceOf<ServerConfigResult.Failure>(result)
        val serverConfig = manager.currentServerConfig
        assertEquals(ServerConfigProvider().getDefaultServerConfig(), serverConfig)
    }

    @Test
    fun `given a server config valid, and endpoints not valid urls, then return null`() = runTest {
        val (_, manager) = Arrangement()
            .withRestrictions(mapOf(ManagedConfigurationsKeys.DEFAULT_SERVER_URLS.asKey() to validServerConfigJsonWithInvalidEndpoints))
            .arrange()

        val result = manager.refreshServerConfig()
        assertInstanceOf<ServerConfigResult.Failure>(result)
        val serverConfig = manager.currentServerConfig
        assertEquals(ServerConfigProvider().getDefaultServerConfig(), serverConfig)
    }

    @Test
    fun `given a valid SSO code, then parse it to a corresponding ManagedSSOConfig`() = runTest {
        val expected = "fd994b20-b9af-11ec-ae36-00163e9b33ca"
        val (_, manager) = Arrangement()
            .withRestrictions(mapOf(ManagedConfigurationsKeys.SSO_CODE.asKey() to validSSOCodeConfigJson))
            .arrange()

        val result = manager.refreshSSOCodeConfig()
        assertInstanceOf<SSOCodeConfigResult.Success>(result)
        val ssoCode = manager.currentSSOCodeConfig

        assertEquals(expected, ssoCode)
    }

    @Test
    fun `given an invalid SSO code, then return empty string`() = runTest {
        val (_, manager) = Arrangement()
            .withRestrictions(mapOf(ManagedConfigurationsKeys.SSO_CODE.asKey() to invalidSSOCodeConfigJson))
            .arrange()

        val result = manager.refreshSSOCodeConfig()
        assertInstanceOf<SSOCodeConfigResult.Failure>(result)
        val ssoCode = manager.currentSSOCodeConfig
        assertEquals(String.EMPTY, ssoCode)
    }

    @Test
    fun `given no SSO code restriction, then return empty string`() = runTest {
        val (_, manager) = Arrangement()
            .withRestrictions(emptyMap())
            .arrange()

        val result = manager.refreshSSOCodeConfig()
        assertInstanceOf<SSOCodeConfigResult.Empty>(result)
        val ssoCode = manager.currentSSOCodeConfig
        assertEquals(String.EMPTY, ssoCode)
    }

    @Test
    fun `given no server config restriction, then return default server config`() = runTest {
        val (_, manager) = Arrangement()
            .withRestrictions(emptyMap())
            .arrange()

        val result = manager.refreshServerConfig()
        assertInstanceOf<ServerConfigResult.Empty>(result)
        val serverConfig = manager.currentServerConfig
        assertEquals(ServerConfigProvider().getDefaultServerConfig(), serverConfig)
    }

    @Test
    fun `given keep_websocket_connection is true, then persistentWebSocketEnforcedByMDM returns true`() = runTest {
        val (_, manager) = Arrangement()
            .withBooleanRestrictions(mapOf(ManagedConfigurationsKeys.KEEP_WEBSOCKET_CONNECTION.asKey() to true))
            .arrange()

        val result = manager.refreshPersistentWebSocketConfig()
        assertEquals(true, result)
        assertEquals(true, manager.persistentWebSocketEnforcedByMDM.value)
    }

    @Test
    fun `given keep_websocket_connection is false, then persistentWebSocketEnforcedByMDM returns false`() = runTest {
        val (_, manager) = Arrangement()
            .withBooleanRestrictions(mapOf(ManagedConfigurationsKeys.KEEP_WEBSOCKET_CONNECTION.asKey() to false))
            .arrange()

        val result = manager.refreshPersistentWebSocketConfig()
        assertEquals(false, result)
        assertEquals(false, manager.persistentWebSocketEnforcedByMDM.value)
    }

    @Test
    fun `given no keep_websocket_connection restriction, then persistentWebSocketEnforcedByMDM returns false`() = runTest {
        val (_, manager) = Arrangement()
            .withRestrictions(emptyMap())
            .arrange()

        val result = manager.refreshPersistentWebSocketConfig()
        assertEquals(false, result)
        assertEquals(false, manager.persistentWebSocketEnforcedByMDM.value)
    }

    // endregion

    // region Context-Mapped Format Tests (Multi-App Support)

    @Test
    fun `given context-mapped server config with matching user ID, then return correct config`() =
        runTest {
            val (_, manager) = Arrangement()
                .withUserIdKey("0")
                .withRestrictions(mapOf(ManagedConfigurationsKeys.DEFAULT_SERVER_URLS.asKey() to contextMappedServerConfigJson))
                .arrange()

            val result = manager.refreshServerConfig()
            assertInstanceOf<ServerConfigResult.Success>(result)

            val serverConfig = manager.currentServerConfig
            assertEquals("Secure Server", serverConfig.title)
            assertEquals("https://secure-account.wire.link", serverConfig.accounts)
        }

    @Test
    fun `given context-mapped server config with non-matching user ID, then fallback to default`() =
        runTest {
            val (_, manager) = Arrangement()
                .withUserIdKey("99") // User ID not in config
                .withRestrictions(mapOf(ManagedConfigurationsKeys.DEFAULT_SERVER_URLS.asKey() to contextMappedServerConfigJson))
                .arrange()

            val result = manager.refreshServerConfig()
            assertInstanceOf<ServerConfigResult.Success>(result)

            val serverConfig = manager.currentServerConfig
            assertEquals("General Server", serverConfig.title)
            assertEquals("https://general-account.wire.link", serverConfig.accounts)
        }

    @Test
    fun `given context-mapped server config without default and non-matching user ID, then return empty`() =
        runTest {
            val (_, manager) = Arrangement()
                .withUserIdKey("99")
                .withRestrictions(mapOf(ManagedConfigurationsKeys.DEFAULT_SERVER_URLS.asKey() to contextMappedServerConfigNoDefaultJson))
                .arrange()

            val result = manager.refreshServerConfig()
            assertInstanceOf<ServerConfigResult.Empty>(result)

            val serverConfig = manager.currentServerConfig
            assertEquals(ServerConfigProvider().getDefaultServerConfig(), serverConfig)
        }

    @Test
    fun `given context-mapped SSO config with matching user ID, then return correct SSO code`() =
        runTest {
            val (_, manager) = Arrangement()
                .withUserIdKey("0")
                .withRestrictions(mapOf(ManagedConfigurationsKeys.SSO_CODE.asKey() to contextMappedSSOConfigJson))
                .arrange()

            val result = manager.refreshSSOCodeConfig()
            assertInstanceOf<SSOCodeConfigResult.Success>(result)

            val ssoCode = manager.currentSSOCodeConfig
            assertEquals("00000000-0000-0000-0000-000000000000", ssoCode)
        }

    @Test
    fun `given context-mapped SSO config with non-matching user ID, then fallback to default`() =
        runTest {
            val (_, manager) = Arrangement()
                .withUserIdKey("99")
                .withRestrictions(mapOf(ManagedConfigurationsKeys.SSO_CODE.asKey() to contextMappedSSOConfigJson))
                .arrange()

            val result = manager.refreshSSOCodeConfig()
            assertInstanceOf<SSOCodeConfigResult.Success>(result)

            val ssoCode = manager.currentSSOCodeConfig
            assertEquals("fd994b20-b9af-11ec-ae36-00163e9b33ca", ssoCode)
        }

    @Test
    fun `given context-mapped SSO config without default and non-matching user ID, then return empty`() =
        runTest {
            val (_, manager) = Arrangement()
                .withUserIdKey("99")
                .withRestrictions(mapOf(ManagedConfigurationsKeys.SSO_CODE.asKey() to contextMappedSSOConfigNoDefaultJson))
                .arrange()

            val result = manager.refreshSSOCodeConfig()
            assertInstanceOf<SSOCodeConfigResult.Empty>(result)

            val ssoCode = manager.currentSSOCodeConfig
            assertEquals(String.EMPTY, ssoCode)
        }

    // endregion

    private class Arrangement {

        private val context: Context = ApplicationProvider.getApplicationContext()
        private var userIdKey: String = "0"

        fun withUserIdKey(userIdKey: String) = apply {
            this.userIdKey = userIdKey
        }

        fun withRestrictions(restrictions: Map<String, String>) = apply {
            val restrictionsManager =
                context.getSystemService(Context.RESTRICTIONS_SERVICE) as RestrictionsManager
            val shadowRestrictionsManager = Shadows.shadowOf(restrictionsManager)
            shadowRestrictionsManager.setApplicationRestrictions(
                Bundle().apply {
                    restrictions.forEach { (key, value) ->
                        putString(key, value)
                    }
                }
            )
        }

        fun withBooleanRestrictions(restrictions: Map<String, Boolean>) = apply {
            val restrictionsManager =
                context.getSystemService(Context.RESTRICTIONS_SERVICE) as RestrictionsManager
            val shadowRestrictionsManager = Shadows.shadowOf(restrictionsManager)
            shadowRestrictionsManager.setApplicationRestrictions(
                Bundle().apply {
                    restrictions.forEach { (key, value) ->
                        putBoolean(key, value)
                    }
                }
            )
        }

        fun arrange(): Pair<Arrangement, ManagedConfigurationsManager> {
            val userContextProvider = object : AndroidUserContextProvider {
                override fun getCurrentAndroidUserId(): Int = userIdKey.toIntOrNull() ?: 0
                override fun getCurrentUserIdKey(): String = userIdKey
            }
            val configParser = ManagedConfigParserImpl(userContextProvider)

            return this to ManagedConfigurationsManagerImpl(
                context = context,
                serverConfigProvider = ServerConfigProvider(),
                dispatchers = TestDispatcherProvider(),
                configParser = configParser,
                globalDataStore = GlobalDataStore(context),
            )
        }
    }

    companion object {
        val validServerConfigJson = """
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

        val validServerConfigJsonWithInvalidEndpoints = """
            {
              "endpoints": {
                "accountsURL": "account.anta.wire.link",
                "backendURL": "nginz-https.anta.wire.link",
                "backendWSURL": "nginz-ssl.anta.wire.",
                "blackListURL": "https://disallowed-clients.anta.wire.link",
                "teamsURL": "https://teams.anta.wire.link",
                "websiteURL": "https://wire.com"
              },
              "title": "anta.wire.link"
            }
        """.trimIndent()

        val validSSOCodeConfigJson = """
            {
              "sso_code": "fd994b20-b9af-11ec-ae36-00163e9b33ca"
            }
        """.trimIndent()

        val invalidSSOCodeConfigJson = """
            {
              "sso_code": "invalid-sso-code"
            }
        """.trimIndent()

        val contextMappedServerConfigJson = """
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

        val contextMappedServerConfigNoDefaultJson = """
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
              }
            }
        """.trimIndent()

        val contextMappedSSOConfigJson = """
            {
              "0": {
                "sso_code": "00000000-0000-0000-0000-000000000000"
              },
              "default": {
                "sso_code": "fd994b20-b9af-11ec-ae36-00163e9b33ca"
              }
            }
        """.trimIndent()

        val contextMappedSSOConfigNoDefaultJson = """
            {
              "0": {
                "sso_code": "00000000-0000-0000-0000-000000000000"
              }
            }
        """.trimIndent()
    }
}
