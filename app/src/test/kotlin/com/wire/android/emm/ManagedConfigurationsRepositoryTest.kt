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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ManagedConfigurationsRepositoryTest {

    @Test
    fun `given a server config is valid, then parse it to a corresponding ManagedServerConfig`() = runTest {
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
        val (_, repository) = Arrangement()
            .withRestrictions(mapOf(ManagedConfigurationsKeys.DEFAULT_SERVER_URLS.asKey() to validServerConfigJson))
            .arrange()

        repository.refreshServerConfig()
        val serverConfig = repository.currentServerConfig

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
        val (_, repository) = Arrangement()
            .withRestrictions(mapOf(ManagedConfigurationsKeys.DEFAULT_SERVER_URLS.asKey() to "invalid json"))
            .arrange()

        repository.refreshServerConfig()
        val serverConfig = repository.currentServerConfig
        assertEquals(ServerConfigProvider().getDefaultServerConfig(), serverConfig)
    }

    @Test
    fun `given a server config valid, and endpoints not valid urls, then return null`() = runTest {
        val (_, repository) = Arrangement()
            .withRestrictions(mapOf(ManagedConfigurationsKeys.DEFAULT_SERVER_URLS.asKey() to validServerConfigJsonWithInvalidEndpoints))
            .arrange()

        repository.refreshServerConfig()
        val serverConfig = repository.currentServerConfig
        assertEquals(ServerConfigProvider().getDefaultServerConfig(), serverConfig)
    }

    @Test
    fun `given a valid SSO code, then parse it to a corresponding ManagedSSOConfig`() = runTest {
        val expected = "fd994b20-b9af-11ec-ae36-00163e9b33ca"
        val (_, repository) = Arrangement()
            .withRestrictions(mapOf(ManagedConfigurationsKeys.SSO_CODE.asKey() to validSSOCodeConfigJson))
            .arrange()

        repository.refreshSSOCodeConfig()
        val ssoCode = repository.currentSSOCodeConfig

        assertEquals(expected, ssoCode)
    }

    @Test
    fun `given an invalid SSO code, then return empty string`() = runTest {
        val (_, repository) = Arrangement()
            .withRestrictions(mapOf(ManagedConfigurationsKeys.SSO_CODE.asKey() to invalidSSOCodeConfigJson))
            .arrange()

        repository.refreshSSOCodeConfig()
        val ssoCode = repository.currentSSOCodeConfig
        assertEquals(String.EMPTY, ssoCode)
    }

    private class Arrangement {

        private val context: Context = ApplicationProvider.getApplicationContext()

        fun withRestrictions(restrictions: Map<String, String>) = apply {
            val restrictionsManager = context.getSystemService(Context.RESTRICTIONS_SERVICE) as RestrictionsManager
            val shadowRestrictionsManager = Shadows.shadowOf(restrictionsManager)
            shadowRestrictionsManager.setApplicationRestrictions(
                Bundle().apply {
                    restrictions.forEach { (key, value) ->
                        putString(key, value)
                    }
                }
            )
        }

        fun arrange() = this to ManagedConfigurationsRepositoryImpl(
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
