package com.wire.android.emm

import android.app.Application
import android.content.Context
import android.content.RestrictionsManager
import androidx.test.core.app.ApplicationProvider
import com.wire.android.config.TestDispatcherProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        val (_, repository) = Arrangement()
            .withRestrictions(mapOf(ManagedConfigurationsKeys.DEFAULT_SERVER_URLS.asKey() to validServerConfigJson))
            .arrange()

        val serverConfig = repository.getServerConfig()

        assertEquals(
            ManagedServerConfig(
                endpoints = ManagedServerLinks(
                    accountsURL = "https://account.anta.wire.link",
                    backendURL = "https://nginz-https.anta.wire.link",
                    backendWSURL = "https://nginz-ssl.anta.wire.link",
                    blackListURL = "https://disallowed-clients.anta.wire.link",
                    teamsURL = "https://teams.anta.wire.link",
                    websiteURL = "https://wire.com"
                ),
                title = "anta.wire.link"
            ),
            serverConfig
        )
    }

    @Test
    fun `given an invalid server config, then return null`() = runTest {
        val (_, repository) = Arrangement()
            .withRestrictions(mapOf(ManagedConfigurationsKeys.DEFAULT_SERVER_URLS.asKey() to "invalid json"))
            .arrange()

        val serverConfig = repository.getServerConfig()

        assertNull(serverConfig)
    }

    @Test
    fun `given a server config valid, and endpoints not valid urls, then return null`() = runTest {
        val (_, repository) = Arrangement()
            .withRestrictions(mapOf(ManagedConfigurationsKeys.DEFAULT_SERVER_URLS.asKey() to validServerConfigJsonWithInvalidEndpoints))
            .arrange()

        val serverConfig = repository.getServerConfig()

        assertNull(serverConfig)
    }

    private class Arrangement {

        private val context: Context = ApplicationProvider.getApplicationContext()

        fun withRestrictions(restrictions: Map<String, String>) = apply {
            val restrictionsManager = context.getSystemService(Context.RESTRICTIONS_SERVICE) as RestrictionsManager
            val shadowRestrictionsManager = Shadows.shadowOf(restrictionsManager)
            shadowRestrictionsManager.setApplicationRestrictions(
                android.os.Bundle().apply {
                    restrictions.forEach { (key, value) ->
                        putString(key, value)
                    }
                }
            )
        }

        fun arrange() = this to ManagedConfigurationsRepository(context, TestDispatcherProvider())
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
    }
}
