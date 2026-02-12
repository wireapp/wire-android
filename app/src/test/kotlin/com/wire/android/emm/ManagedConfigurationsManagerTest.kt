package com.wire.android.emm

import android.app.Application
import android.content.Context
import android.content.RestrictionsManager
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.wire.android.config.ServerConfigProvider
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.feature.IsSecureFolderUseCase
import com.wire.android.util.EMPTY
import io.mockk.every
import io.mockk.mockk
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

    // region context-mapped server config

    @Test
    fun `given context-mapped server config and regular context, then resolve regular sub-config`() = runTest {
        val (_, manager) = Arrangement()
            .withRestrictions(mapOf(ManagedConfigurationsKeys.DEFAULT_SERVER_URLS.asKey() to contextMappedServerConfigJson))
            .arrange(isSecureFolder = false)

        val result = manager.refreshServerConfig()
        assertInstanceOf<ServerConfigResult.Success>(result)
        assertEquals("Primary", manager.currentServerConfig.title)
        assertEquals("https://nginz-https.regular.wire.link", manager.currentServerConfig.api)
    }

    @Test
    fun `given context-mapped server config and secure context, then resolve secure sub-config`() = runTest {
        val (_, manager) = Arrangement()
            .withRestrictions(mapOf(ManagedConfigurationsKeys.DEFAULT_SERVER_URLS.asKey() to contextMappedServerConfigJson))
            .arrange(isSecureFolder = true)

        val result = manager.refreshServerConfig()
        assertInstanceOf<ServerConfigResult.Success>(result)
        assertEquals("Secure Folder", manager.currentServerConfig.title)
        assertEquals("https://nginz-https.secure.wire.link", manager.currentServerConfig.api)
    }

    @Test
    fun `given context-mapped server config with only default, then resolve default sub-config`() = runTest {
        val (_, manager) = Arrangement()
            .withRestrictions(mapOf(ManagedConfigurationsKeys.DEFAULT_SERVER_URLS.asKey() to contextMappedServerConfigDefaultOnlyJson))
            .arrange(isSecureFolder = false)

        val result = manager.refreshServerConfig()
        assertInstanceOf<ServerConfigResult.Success>(result)
        assertEquals("Default", manager.currentServerConfig.title)
        assertEquals("https://nginz-https.default.wire.link", manager.currentServerConfig.api)
    }

    @Test
    fun `given context-mapped server config with no match and no default, then fall back to app defaults`() = runTest {
        val (_, manager) = Arrangement()
            .withRestrictions(mapOf(ManagedConfigurationsKeys.DEFAULT_SERVER_URLS.asKey() to contextMappedServerConfigNoMatchJson))
            .arrange(isSecureFolder = false)

        val result = manager.refreshServerConfig()
        assertInstanceOf<ServerConfigResult.Failure>(result)
        assertEquals(ServerConfigProvider().getDefaultServerConfig(), manager.currentServerConfig)
    }

    // endregion

    // region context-mapped SSO code

    @Test
    fun `given context-mapped SSO code and regular context, then resolve regular sso code`() = runTest {
        val (_, manager) = Arrangement()
            .withRestrictions(mapOf(ManagedConfigurationsKeys.SSO_CODE.asKey() to contextMappedSSOCodeConfigJson))
            .arrange(isSecureFolder = false)

        val result = manager.refreshSSOCodeConfig()
        assertInstanceOf<SSOCodeConfigResult.Success>(result)
        assertEquals("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", manager.currentSSOCodeConfig)
    }

    @Test
    fun `given context-mapped SSO code and secure context, then resolve secure sso code`() = runTest {
        val (_, manager) = Arrangement()
            .withRestrictions(mapOf(ManagedConfigurationsKeys.SSO_CODE.asKey() to contextMappedSSOCodeConfigJson))
            .arrange(isSecureFolder = true)

        val result = manager.refreshSSOCodeConfig()
        assertInstanceOf<SSOCodeConfigResult.Success>(result)
        assertEquals("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", manager.currentSSOCodeConfig)
    }

    @Test
    fun `given context-mapped SSO code with only default, then resolve default sso code`() = runTest {
        val (_, manager) = Arrangement()
            .withRestrictions(mapOf(ManagedConfigurationsKeys.SSO_CODE.asKey() to contextMappedSSOCodeDefaultOnlyJson))
            .arrange(isSecureFolder = true)

        val result = manager.refreshSSOCodeConfig()
        assertInstanceOf<SSOCodeConfigResult.Success>(result)
        assertEquals("cccccccc-cccc-cccc-cccc-cccccccccccc", manager.currentSSOCodeConfig)
    }

    @Test
    fun `given context-mapped SSO code with no match and no default, then return empty string`() = runTest {
        val (_, manager) = Arrangement()
            .withRestrictions(mapOf(ManagedConfigurationsKeys.SSO_CODE.asKey() to contextMappedSSOCodeNoMatchJson))
            .arrange(isSecureFolder = false)

        val result = manager.refreshSSOCodeConfig()
        assertInstanceOf<SSOCodeConfigResult.Failure>(result)
        assertEquals(String.EMPTY, manager.currentSSOCodeConfig)
    }

    // endregion

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

        fun arrange(isSecureFolder: Boolean = false): Pair<Arrangement, ManagedConfigurationsManagerImpl> {
            val isSecureFolderUseCase = mockk<IsSecureFolderUseCase>()
            every { isSecureFolderUseCase() } returns isSecureFolder
            return this to ManagedConfigurationsManagerImpl(
                context = context,
                serverConfigProvider = ServerConfigProvider(),
                dispatchers = TestDispatcherProvider(),
                globalDataStore = GlobalDataStore(context),
                isSecureFolder = isSecureFolderUseCase
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

        private fun serverEndpoints(subdomain: String) = """
            {
              "accountsURL": "https://account.$subdomain.wire.link",
              "backendURL": "https://nginz-https.$subdomain.wire.link",
              "backendWSURL": "https://nginz-ssl.$subdomain.wire.link",
              "blackListURL": "https://disallowed-clients.$subdomain.wire.link",
              "teamsURL": "https://teams.$subdomain.wire.link",
              "websiteURL": "https://wire.com"
            }
        """.trimIndent()

        val contextMappedServerConfigJson = """
            {
              "regular": { "title": "Primary", "endpoints": ${serverEndpoints("regular")} },
              "secure": { "title": "Secure Folder", "endpoints": ${serverEndpoints("secure")} },
              "default": { "title": "Default", "endpoints": ${serverEndpoints("default")} }
            }
        """.trimIndent()

        val contextMappedServerConfigDefaultOnlyJson = """
            {
              "default": { "title": "Default", "endpoints": ${serverEndpoints("default")} }
            }
        """.trimIndent()

        val contextMappedServerConfigNoMatchJson = """
            {
              "secure": { "title": "Secure Only", "endpoints": ${serverEndpoints("secure")} }
            }
        """.trimIndent()

        val contextMappedSSOCodeConfigJson = """
            {
              "regular": { "sso_code": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa" },
              "secure": { "sso_code": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb" },
              "default": { "sso_code": "cccccccc-cccc-cccc-cccc-cccccccccccc" }
            }
        """.trimIndent()

        val contextMappedSSOCodeDefaultOnlyJson = """
            {
              "default": { "sso_code": "cccccccc-cccc-cccc-cccc-cccccccccccc" }
            }
        """.trimIndent()

        val contextMappedSSOCodeNoMatchJson = """
            {
              "secure": { "sso_code": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb" }
            }
        """.trimIndent()
    }
}
