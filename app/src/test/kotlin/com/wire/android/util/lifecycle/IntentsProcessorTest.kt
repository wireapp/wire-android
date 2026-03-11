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
import java.nio.charset.StandardCharsets
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64

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
            .withAutomatedLoginExtra(
                automatedLoginJson(
                    "backendConfig" to FAKE_BACKEND_CONFIG,
                    "ssoCode" to FAKE_SSO_CODE,
                    "nomadProfilesHost" to FAKE_NOMAD_PROFILES_HOST,
                    "sigNomadProfilesHost" to IntentsProcessor.SKIP_SIGNATURE_VERIFICATION_TOKEN
                )
            )
            .arrange()
        assertEquals(
            AutomatedLoginViaSSO(
                backendConfig = FAKE_BACKEND_CONFIG,
                ssoCode = FAKE_SSO_CODE,
                nomadProfilesHost = FAKE_NOMAD_PROFILES_HOST
            ),
            intentsProcessor(arrangement.intent)
        )
    }

    @Test
    fun `given valid JSON with ssoCode and nomadProfilesHost but no backendConfig, returns null`() {
        val (arrangement, intentsProcessor) = Arrangement()
            .withAutomatedLoginExtra(
                automatedLoginJson(
                    "ssoCode" to FAKE_SSO_CODE,
                    "nomadProfilesHost" to FAKE_NOMAD_PROFILES_HOST,
                    "sigNomadProfilesHost" to IntentsProcessor.SKIP_SIGNATURE_VERIFICATION_TOKEN
                )
            )
            .arrange()
        assertNull(intentsProcessor(arrangement.intent))
    }

    @Test
    fun `given valid JSON with only backendConfig and nomadProfilesHost, returns AutomatedLoginViaSSO`() {
        val (arrangement, intentsProcessor) = Arrangement()
            .withAutomatedLoginExtra(
                automatedLoginJson(
                    "backendConfig" to FAKE_BACKEND_CONFIG,
                    "nomadProfilesHost" to FAKE_NOMAD_PROFILES_HOST,
                    "sigNomadProfilesHost" to IntentsProcessor.SKIP_SIGNATURE_VERIFICATION_TOKEN
                )
            )
            .arrange()
        assertEquals(
            AutomatedLoginViaSSO(
                backendConfig = FAKE_BACKEND_CONFIG,
                nomadProfilesHost = FAKE_NOMAD_PROFILES_HOST
            ),
            intentsProcessor(arrangement.intent)
        )
    }

    @Test
    fun `given valid JSON with only nomadProfilesHost, returns AutomatedLoginViaSSO`() {
        val (arrangement, intentsProcessor) = Arrangement()
            .withAutomatedLoginExtra(
                automatedLoginJson(
                    "nomadProfilesHost" to FAKE_NOMAD_PROFILES_HOST,
                    "sigNomadProfilesHost" to IntentsProcessor.SKIP_SIGNATURE_VERIFICATION_TOKEN
                )
            )
            .arrange()
        assertEquals(
            AutomatedLoginViaSSO(nomadProfilesHost = FAKE_NOMAD_PROFILES_HOST),
            intentsProcessor(arrangement.intent)
        )
    }

    @Test
    fun `given real Ed25519 signature, verifies nomadProfilesHost with configured key`() {
        val signedValue = SignedValue.sign(FAKE_NOMAD_PROFILES_HOST)
        val (arrangement, intentsProcessor) = Arrangement()
            .withConfigurationSignatureKey(signedValue.publicKeyDerBase64)
            .withAutomatedLoginExtra(
                automatedLoginJson(
                    "nomadProfilesHost" to FAKE_NOMAD_PROFILES_HOST,
                    "sigNomadProfilesHost" to signedValue.signatureBase64
                )
            )
            .arrange()
        assertEquals(
            AutomatedLoginViaSSO(nomadProfilesHost = FAKE_NOMAD_PROFILES_HOST),
            intentsProcessor(arrangement.intent)
        )
    }

    @Test
    fun `given invalid Ed25519 signature, returns null`() {
        val signedValue = SignedValue.sign(FAKE_NOMAD_PROFILES_HOST)
        val invalidSignature = Base64.getEncoder()
            .encodeToString(
                Base64.getDecoder().decode(signedValue.signatureBase64).apply {
                    this[lastIndex] = (this[lastIndex].toInt() xor 0x01).toByte()
                }
            )
        val (arrangement, intentsProcessor) = Arrangement()
            .withConfigurationSignatureKey(signedValue.publicKeyDerBase64)
            .withAutomatedLoginExtra(
                automatedLoginJson(
                    "nomadProfilesHost" to FAKE_NOMAD_PROFILES_HOST,
                    "sigNomadProfilesHost" to invalidSignature
                )
            )
            .arrange()
        assertNull(intentsProcessor(arrangement.intent))
    }

    @Test
    fun `given configured key with invalid raw key length, returns null`() {
        val signedValue = SignedValue.sign(FAKE_NOMAD_PROFILES_HOST)
        val invalidLengthKey = Base64.getEncoder().encodeToString(ByteArray(12 + 31))
        val (arrangement, intentsProcessor) = Arrangement()
            .withConfigurationSignatureKey(invalidLengthKey)
            .withAutomatedLoginExtra(
                automatedLoginJson(
                    "nomadProfilesHost" to FAKE_NOMAD_PROFILES_HOST,
                    "sigNomadProfilesHost" to signedValue.signatureBase64
                )
            )
            .arrange()
        assertNull(intentsProcessor(arrangement.intent))
    }

    @Test
    fun `given configured key with invalid der header, returns null`() {
        val signedValue = SignedValue.sign(FAKE_NOMAD_PROFILES_HOST)
        val invalidHeaderKey = Base64.getEncoder()
            .encodeToString(
                Base64.getDecoder().decode(signedValue.publicKeyDerBase64).apply {
                    this[0] = (this[0].toInt() xor 0x01).toByte()
                }
            )
        val (arrangement, intentsProcessor) = Arrangement()
            .withConfigurationSignatureKey(invalidHeaderKey)
            .withAutomatedLoginExtra(
                automatedLoginJson(
                    "nomadProfilesHost" to FAKE_NOMAD_PROFILES_HOST,
                    "sigNomadProfilesHost" to signedValue.signatureBase64
                )
            )
            .arrange()
        assertNull(intentsProcessor(arrangement.intent))
    }

    @Test
    fun `given nomad profiles feature disabled, skips the whole intent`() {
        val (arrangement, intentsProcessor) = Arrangement()
            .withNomadProfilesFeatureEnabled(false)
            .withAutomatedLoginExtra(
                automatedLoginJson(
                    "backendConfig" to FAKE_BACKEND_CONFIG,
                    "ssoCode" to FAKE_SSO_CODE,
                    "nomadProfilesHost" to FAKE_NOMAD_PROFILES_HOST,
                    "sigNomadProfilesHost" to IntentsProcessor.SKIP_SIGNATURE_VERIFICATION_TOKEN
                )
            )
            .arrange()
        assertNull(intentsProcessor(arrangement.intent))
    }

    @Test
    fun `given invalid nomadProfilesHost and feature disabled, skips the whole intent`() {
        val (arrangement, intentsProcessor) = Arrangement()
            .withNomadProfilesFeatureEnabled(false)
            .withAutomatedLoginExtra(
                automatedLoginJson(
                    "backendConfig" to FAKE_BACKEND_CONFIG,
                    "ssoCode" to FAKE_SSO_CODE,
                    "nomadProfilesHost" to "not a url"
                )
            )
            .arrange()
        assertNull(intentsProcessor(arrangement.intent))
    }

    @Test
    fun `given only nomadProfilesHost and feature disabled, returns null`() {
        val (arrangement, intentsProcessor) = Arrangement()
            .withNomadProfilesFeatureEnabled(false)
            .withAutomatedLoginExtra(
                automatedLoginJson(
                    "nomadProfilesHost" to FAKE_NOMAD_PROFILES_HOST,
                    "sigNomadProfilesHost" to IntentsProcessor.SKIP_SIGNATURE_VERIFICATION_TOKEN
                )
            )
            .arrange()
        assertNull(intentsProcessor(arrangement.intent))
    }

    @Test
    fun `given JSON with all fields null, returns null`() {
        val (arrangement, intentsProcessor) = Arrangement()
            .withAutomatedLoginExtra(automatedLoginJson())
            .arrange()
        assertNull(intentsProcessor(arrangement.intent))
    }

    @Test
    fun `given JSON without nomadProfilesHost, returns null`() {
        val (arrangement, intentsProcessor) = Arrangement()
            .withAutomatedLoginExtra(
                automatedLoginJson(
                    "backendConfig" to FAKE_BACKEND_CONFIG,
                    "ssoCode" to FAKE_SSO_CODE
                )
            )
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
            .withAutomatedLoginExtra(
                automatedLoginJson(
                    "backendConfig" to "http://insecure.wire.com/deeplink.json",
                    "nomadProfilesHost" to FAKE_NOMAD_PROFILES_HOST,
                    "sigNomadProfilesHost" to IntentsProcessor.SKIP_SIGNATURE_VERIFICATION_TOKEN
                )
            )
            .arrange()
        assertNull(intentsProcessor(arrangement.intent))
    }

    @Test
    fun `given backendConfig with empty host, returns null`() {
        val (arrangement, intentsProcessor) = Arrangement()
            .withAutomatedLoginExtra(
                automatedLoginJson(
                    "backendConfig" to "https:///path",
                    "nomadProfilesHost" to FAKE_NOMAD_PROFILES_HOST,
                    "sigNomadProfilesHost" to IntentsProcessor.SKIP_SIGNATURE_VERIFICATION_TOKEN
                )
            )
            .arrange()
        assertNull(intentsProcessor(arrangement.intent))
    }

    @Test
    fun `given backendConfig with malformed URI, returns null`() {
        val (arrangement, intentsProcessor) = Arrangement()
            .withAutomatedLoginExtra(
                automatedLoginJson(
                    "backendConfig" to "not a url",
                    "nomadProfilesHost" to FAKE_NOMAD_PROFILES_HOST,
                    "sigNomadProfilesHost" to IntentsProcessor.SKIP_SIGNATURE_VERIFICATION_TOKEN
                )
            )
            .arrange()
        assertNull(intentsProcessor(arrangement.intent))
    }

    @Test
    fun `given nomadProfilesHost with HTTP instead of HTTPS, returns null`() {
        val (arrangement, intentsProcessor) = Arrangement()
            .withAutomatedLoginExtra(
                automatedLoginJson(
                    "nomadProfilesHost" to "http://insecure.nomad.example.com",
                    "sigNomadProfilesHost" to IntentsProcessor.SKIP_SIGNATURE_VERIFICATION_TOKEN
                )
            )
            .arrange()
        assertNull(intentsProcessor(arrangement.intent))
    }

    @Test
    fun `given nomadProfilesHost with empty host, returns null`() {
        val (arrangement, intentsProcessor) = Arrangement()
            .withAutomatedLoginExtra(
                automatedLoginJson(
                    "nomadProfilesHost" to "https:///path",
                    "sigNomadProfilesHost" to IntentsProcessor.SKIP_SIGNATURE_VERIFICATION_TOKEN
                )
            )
            .arrange()
        assertNull(intentsProcessor(arrangement.intent))
    }

    @Test
    fun `given nomadProfilesHost with malformed URI, returns null`() {
        val (arrangement, intentsProcessor) = Arrangement()
            .withAutomatedLoginExtra(
                automatedLoginJson(
                    "nomadProfilesHost" to "not a url",
                    "sigNomadProfilesHost" to IntentsProcessor.SKIP_SIGNATURE_VERIFICATION_TOKEN
                )
            )
            .arrange()
        assertNull(intentsProcessor(arrangement.intent))
    }

    @Test
    fun `given skip token when signature enforcement is enabled, returns null`() {
        val (arrangement, intentsProcessor) = Arrangement()
            .withConfigurationSignatureEnforced(true)
            .withAutomatedLoginExtra(
                automatedLoginJson(
                    "nomadProfilesHost" to FAKE_NOMAD_PROFILES_HOST,
                    "sigNomadProfilesHost" to IntentsProcessor.SKIP_SIGNATURE_VERIFICATION_TOKEN
                )
            )
            .arrange()
        assertNull(intentsProcessor(arrangement.intent))
    }

    private fun automatedLoginJson(vararg entries: Pair<String, String>): String =
        if (entries.isEmpty()) {
            "{}"
        } else {
            buildString {
                append("{\n")
                entries.joinTo(this, separator = ",\n") { (key, value) ->
                    """"$key":"$value""""
                }
                append("\n}")
            }
        }

    class Arrangement {
        internal val intent: Intent = mockk()
        private val nomadProfilesFeatureConfig = mockk<NomadProfilesFeatureConfig>()
        private var configurationSignatureKeys: List<String>? = null
        private var isConfigurationSignatureEnforced = false

        init {
            every { intent.getStringExtra(any()) } returns null
            every { nomadProfilesFeatureConfig.isEnabled() } returns true
        }

        fun arrange() = this to (
                configurationSignatureKeys?.let { configuredKeys ->
                    IntentsProcessor(
                        nomadProfilesFeatureConfig = nomadProfilesFeatureConfig,
                        configurationSignatureKeys = configuredKeys,
                        isConfigurationSignatureEnforced = isConfigurationSignatureEnforced
                    )
                } ?: IntentsProcessor(
                    nomadProfilesFeatureConfig = nomadProfilesFeatureConfig,
                    configurationSignatureKeys = emptyList(),
                    isConfigurationSignatureEnforced = isConfigurationSignatureEnforced
                )
                )

        fun withAutomatedLoginExtra(json: String?) = apply {
            every { intent.getStringExtra("automated_login") } returns json
        }

        fun withNomadProfilesFeatureEnabled(enabled: Boolean) = apply {
            every { nomadProfilesFeatureConfig.isEnabled() } returns enabled
        }

        fun withConfigurationSignatureKey(vararg key: String) = apply {
            configurationSignatureKeys = key.toList()
        }

        fun withConfigurationSignatureEnforced(isEnforced: Boolean) = apply {
            isConfigurationSignatureEnforced = isEnforced
        }
    }

    private data class SignedValue(
        val publicKeyDerBase64: String,
        val signatureBase64: String
    ) {
        companion object {
            fun sign(value: String): SignedValue {
                val keyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
                val signature = Signature.getInstance("Ed25519").run {
                    initSign(keyPair.private)
                    update(value.toByteArray(StandardCharsets.UTF_8))
                    sign()
                }
                return SignedValue(
                    publicKeyDerBase64 = Base64.getEncoder().encodeToString(keyPair.public.encoded),
                    signatureBase64 = Base64.getEncoder().encodeToString(signature)
                )
            }
        }
    }

    private companion object {
        const val FAKE_BACKEND_CONFIG = "https://example.com/deeplink.json"
        const val FAKE_SSO_CODE = "wire-87080ee2-7855-47e2-a60a-4b3def45bbd4"
        const val FAKE_NOMAD_PROFILES_HOST = "https://nomad.example.com"
    }
}
