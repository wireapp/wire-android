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
package com.wire.android.util.lifecycle

import android.content.Intent
import com.wire.android.config.NomadProfilesFeatureConfig
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class IntentsProcessorTest {

    @Test
    fun `given null intent, returns null`() {
        val (_, intentsProcessor) = Arrangement().arrange()
        assertNull(intentsProcessor(null))
    }

    @Test
    fun `given intent without automated_login extra, returns null`() {
        val (arrangement, intentsProcessor) = Arrangement()
            .withAutomatedLoginExtra(null)
            .arrange()
        assertNull(intentsProcessor(arrangement.intent))
    }

    @Test
    fun `given valid JSON with backendConfig, ssoCode and nomadProfilesHost, returns AutomatedLoginViaSSO`() {
        val (arrangement, intentsProcessor) = Arrangement()
            .withAutomatedLoginExtra("""{"backendConfig":"$FAKE_BACKEND_CONFIG","ssoCode":"$FAKE_SSO_CODE","nomadProfilesHost":"$FAKE_NOMAD_PROFILES_HOST","sigNomadProfilesHost":"skip"}""")
            .arrange()
        assertEquals(
            AutomatedLoginViaSSO(backendConfig = FAKE_BACKEND_CONFIG, ssoCode = FAKE_SSO_CODE, nomadProfilesHost = FAKE_NOMAD_PROFILES_HOST),
            intentsProcessor(arrangement.intent)
        )
    }

    @Test
    fun `given valid JSON with ssoCode and nomadProfilesHost, returns AutomatedLoginViaSSO`() {
        val (arrangement, intentsProcessor) = Arrangement()
            .withAutomatedLoginExtra("""{"ssoCode":"$FAKE_SSO_CODE","nomadProfilesHost":"$FAKE_NOMAD_PROFILES_HOST","sigNomadProfilesHost":"skip"}""")
            .arrange()
        assertEquals(
            AutomatedLoginViaSSO(ssoCode = FAKE_SSO_CODE, nomadProfilesHost = FAKE_NOMAD_PROFILES_HOST),
            intentsProcessor(arrangement.intent)
        )
    }

    @Test
    fun `given valid JSON with only backendConfig and nomadProfilesHost, returns AutomatedLoginViaSSO`() {
        val (arrangement, intentsProcessor) = Arrangement()
            .withAutomatedLoginExtra("""{"backendConfig":"$FAKE_BACKEND_CONFIG","nomadProfilesHost":"$FAKE_NOMAD_PROFILES_HOST","sigNomadProfilesHost":"skip"}""")
            .arrange()
        assertEquals(
            AutomatedLoginViaSSO(backendConfig = FAKE_BACKEND_CONFIG, nomadProfilesHost = FAKE_NOMAD_PROFILES_HOST),
            intentsProcessor(arrangement.intent)
        )
    }

    @Test
    fun `given valid JSON with only nomadProfilesHost, returns AutomatedLoginViaSSO`() {
        val (arrangement, intentsProcessor) = Arrangement()
            .withAutomatedLoginExtra("""{"nomadProfilesHost":"$FAKE_NOMAD_PROFILES_HOST","sigNomadProfilesHost":"skip"}""")
            .arrange()
        assertEquals(
            AutomatedLoginViaSSO(nomadProfilesHost = FAKE_NOMAD_PROFILES_HOST),
            intentsProcessor(arrangement.intent)
        )
    }

    @Test
    fun `given nomad profiles feature disabled, strips nomadProfilesHost from result`() {
        val (arrangement, intentsProcessor) = Arrangement()
            .withNomadProfilesFeatureEnabled(false)
            .withAutomatedLoginExtra("""{"backendConfig":"$FAKE_BACKEND_CONFIG","ssoCode":"$FAKE_SSO_CODE","nomadProfilesHost":"$FAKE_NOMAD_PROFILES_HOST","sigNomadProfilesHost":"skip"}""")
            .arrange()
        assertEquals(
            AutomatedLoginViaSSO(backendConfig = FAKE_BACKEND_CONFIG, ssoCode = FAKE_SSO_CODE),
            intentsProcessor(arrangement.intent)
        )
    }

    @Test
    fun `given only nomadProfilesHost and feature disabled, returns null`() {
        val (arrangement, intentsProcessor) = Arrangement()
            .withNomadProfilesFeatureEnabled(false)
            .withAutomatedLoginExtra("""{"nomadProfilesHost":"$FAKE_NOMAD_PROFILES_HOST","sigNomadProfilesHost":"skip"}""")
            .arrange()
        assertNull(intentsProcessor(arrangement.intent))
    }

    @Test
    fun `given JSON with all fields null, returns null`() {
        val (arrangement, intentsProcessor) = Arrangement()
            .withAutomatedLoginExtra("""{}""")
            .arrange()
        assertNull(intentsProcessor(arrangement.intent))
    }

    @Test
    fun `given JSON without nomadProfilesHost, returns null`() {
        val (arrangement, intentsProcessor) = Arrangement()
            .withAutomatedLoginExtra("""{"backendConfig":"$FAKE_BACKEND_CONFIG","ssoCode":"$FAKE_SSO_CODE"}""")
            .arrange()
        assertNull(intentsProcessor(arrangement.intent))
    }

    @Test
    fun `given invalid JSON string, returns null`() {
        val (arrangement, intentsProcessor) = Arrangement()
            .withAutomatedLoginExtra("not-valid-json")
            .arrange()
        assertNull(intentsProcessor(arrangement.intent))
    }

    @Test
    fun `given backendConfig with HTTP instead of HTTPS, returns null`() {
        val (arrangement, intentsProcessor) = Arrangement()
            .withAutomatedLoginExtra("""{"backendConfig":"http://insecure.wire.com/deeplink.json","nomadProfilesHost":"$FAKE_NOMAD_PROFILES_HOST","sigNomadProfilesHost":"skip"}""")
            .arrange()
        assertNull(intentsProcessor(arrangement.intent))
    }

    @Test
    fun `given backendConfig with empty host, returns null`() {
        val (arrangement, intentsProcessor) = Arrangement()
            .withAutomatedLoginExtra("""{"backendConfig":"https:///path","nomadProfilesHost":"$FAKE_NOMAD_PROFILES_HOST","sigNomadProfilesHost":"skip"}""")
            .arrange()
        assertNull(intentsProcessor(arrangement.intent))
    }

    @Test
    fun `given backendConfig with malformed URI, returns null`() {
        val (arrangement, intentsProcessor) = Arrangement()
            .withAutomatedLoginExtra("""{"backendConfig":"not a url","nomadProfilesHost":"$FAKE_NOMAD_PROFILES_HOST","sigNomadProfilesHost":"skip"}""")
            .arrange()
        assertNull(intentsProcessor(arrangement.intent))
    }

    @Test
    fun `given nomadProfilesHost with HTTP instead of HTTPS, returns null`() {
        val (arrangement, intentsProcessor) = Arrangement()
            .withAutomatedLoginExtra("""{"nomadProfilesHost":"http://insecure.nomad.example.com","sigNomadProfilesHost":"skip"}""")
            .arrange()
        assertNull(intentsProcessor(arrangement.intent))
    }

    @Test
    fun `given nomadProfilesHost with empty host, returns null`() {
        val (arrangement, intentsProcessor) = Arrangement()
            .withAutomatedLoginExtra("""{"nomadProfilesHost":"https:///path","sigNomadProfilesHost":"skip"}""")
            .arrange()
        assertNull(intentsProcessor(arrangement.intent))
    }

    @Test
    fun `given nomadProfilesHost with malformed URI, returns null`() {
        val (arrangement, intentsProcessor) = Arrangement()
            .withAutomatedLoginExtra("""{"nomadProfilesHost":"not a url","sigNomadProfilesHost":"skip"}""")
            .arrange()
        assertNull(intentsProcessor(arrangement.intent))
    }

    class Arrangement {
        internal val intent: Intent = mockk()
        private val nomadProfilesFeatureConfig = mockk<NomadProfilesFeatureConfig>()

        init {
            every { intent.getStringExtra(any()) } returns null
            every { nomadProfilesFeatureConfig.isEnabled() } returns true
        }

        fun arrange() = this to IntentsProcessor(nomadProfilesFeatureConfig)

        fun withAutomatedLoginExtra(json: String?) = apply {
            every { intent.getStringExtra("automated_login") } returns json
        }

        fun withNomadProfilesFeatureEnabled(enabled: Boolean) = apply {
            every { nomadProfilesFeatureConfig.isEnabled() } returns enabled
        }
    }

    private companion object {
        const val FAKE_BACKEND_CONFIG = "https://example.com/deeplink.json"
        const val FAKE_SSO_CODE = "wire-87080ee2-7855-47e2-a60a-4b3def45bbd4"
        const val FAKE_NOMAD_PROFILES_HOST = "https://nomad.example.com"
    }
}
