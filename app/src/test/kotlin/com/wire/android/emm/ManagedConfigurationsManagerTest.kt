package com.wire.android.emm

import android.app.Application
import android.content.Context
import android.content.RestrictionsManager
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.wire.android.config.ServerConfigProvider
import com.wire.android.config.TestDispatcherProvider
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

        manager.refreshPersistentWebSocketConfig()
        assertEquals(true, manager.persistentWebSocketEnforcedByMDM.value)
    }

    @Test
    fun `given keep_websocket_connection is false, then persistentWebSocketEnforcedByMDM returns false`() = runTest {
        val (_, manager) = Arrangement()
            .withBooleanRestrictions(mapOf(ManagedConfigurationsKeys.KEEP_WEBSOCKET_CONNECTION.asKey() to false))
            .arrange()

        manager.refreshPersistentWebSocketConfig()
        assertEquals(false, manager.persistentWebSocketEnforcedByMDM.value)
    }

    @Test
    fun `given no keep_websocket_connection restriction, then persistentWebSocketEnforcedByMDM returns false`() = runTest {
        val (_, manager) = Arrangement()
            .withRestrictions(emptyMap())
            .arrange()

        manager.refreshPersistentWebSocketConfig()
        assertEquals(false, manager.persistentWebSocketEnforcedByMDM.value)
    }

    private class Arrangement {

        private val context: Context = ApplicationProvider.getApplicationContext()

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

        fun arrange() = this to ManagedConfigurationsManagerImpl(
            context = context,
            serverConfigProvider = ServerConfigProvider(),
            dispatchers = TestDispatcherProvider()
        )
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
    }
}
