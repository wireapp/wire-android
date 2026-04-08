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

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64

class NomadIntentSignatureValidatorTest {

    @Test
    fun `given null parameter and null signature, returns true`() = runTest {
        val validator = Arrangement().arrange()

        assertTrue(validator.isValid(parameter = null, signature = null))
    }

    @Test
    fun `given null parameter and non-null signature, returns false`() = runTest {
        val validator = Arrangement().arrange()

        assertFalse(validator.isValid(parameter = null, signature = "signature"))
    }

    @Test
    fun `given non-null parameter and null signature, returns false`() = runTest {
        val validator = Arrangement().arrange()

        assertFalse(validator.isValid(parameter = FAKE_NOMAD_PROFILES_HOST, signature = null))
    }

    @Test
    fun `given skip token when signature enforcement is disabled, returns true`() = runTest {
        val validator = Arrangement().arrange()

        assertTrue(
            validator.isValid(
                parameter = FAKE_NOMAD_PROFILES_HOST,
                signature = NomadIntentSignatureValidator.SKIP_SIGNATURE_VERIFICATION_TOKEN
            )
        )
    }

    @Test
    fun `given skip token when signature enforcement is enabled, returns false`() = runTest {
        val validator = Arrangement()
            .withConfigurationSignatureEnforced(true)
            .arrange()

        assertFalse(
            validator.isValid(
                parameter = FAKE_NOMAD_PROFILES_HOST,
                signature = NomadIntentSignatureValidator.SKIP_SIGNATURE_VERIFICATION_TOKEN
            )
        )
    }

    @Test
    fun `given valid Ed25519 signature, returns true`() = runTest {
        val signedValue = SignedValue.sign(FAKE_NOMAD_PROFILES_HOST)
        val validator = Arrangement()
            .withConfigurationSignatureKey(signedValue.publicKeyDerBase64)
            .arrange()

        assertTrue(
            validator.isValid(
                parameter = FAKE_NOMAD_PROFILES_HOST,
                signature = signedValue.signatureBase64
            )
        )
    }

    @Test
    fun `given invalid Ed25519 signature, returns false`() = runTest {
        val signedValue = SignedValue.sign(FAKE_NOMAD_PROFILES_HOST)
        val invalidSignature = Base64.getEncoder()
            .encodeToString(
                Base64.getDecoder().decode(signedValue.signatureBase64).apply {
                    this[lastIndex] = (this[lastIndex].toInt() xor 0x01).toByte()
                }
            )
        val validator = Arrangement()
            .withConfigurationSignatureKey(signedValue.publicKeyDerBase64)
            .arrange()

        assertFalse(
            validator.isValid(
                parameter = FAKE_NOMAD_PROFILES_HOST,
                signature = invalidSignature
            )
        )
    }

    @Test
    fun `given malformed base64 signature, returns false`() = runTest {
        val signedValue = SignedValue.sign(FAKE_NOMAD_PROFILES_HOST)
        val validator = Arrangement()
            .withConfigurationSignatureKey(signedValue.publicKeyDerBase64)
            .arrange()

        assertFalse(
            validator.isValid(
                parameter = FAKE_NOMAD_PROFILES_HOST,
                signature = "%%%not-base64%%%"
            )
        )
    }

    @Test
    fun `given configured key with invalid raw key length, returns false`() = runTest {
        val signedValue = SignedValue.sign(FAKE_NOMAD_PROFILES_HOST)
        val invalidLengthKey = Base64.getEncoder().encodeToString(ByteArray(12 + 31))
        val validator = Arrangement()
            .withConfigurationSignatureKey(invalidLengthKey)
            .arrange()

        assertFalse(
            validator.isValid(
                parameter = FAKE_NOMAD_PROFILES_HOST,
                signature = signedValue.signatureBase64
            )
        )
    }

    @Test
    fun `given configured key with invalid der header, returns false`() = runTest {
        val signedValue = SignedValue.sign(FAKE_NOMAD_PROFILES_HOST)
        val invalidHeaderKey = Base64.getEncoder()
            .encodeToString(
                Base64.getDecoder().decode(signedValue.publicKeyDerBase64).apply {
                    this[0] = (this[0].toInt() xor 0x01).toByte()
                }
            )
        val validator = Arrangement()
            .withConfigurationSignatureKey(invalidHeaderKey)
            .arrange()

        assertFalse(
            validator.isValid(
                parameter = FAKE_NOMAD_PROFILES_HOST,
                signature = signedValue.signatureBase64
            )
        )
    }

    private class Arrangement {
        private var configurationSignatureKeys: Map<String, String> = emptyMap()
        private var isConfigurationSignatureEnforced = false

        fun arrange() = NomadIntentSignatureValidator(
            configurationSignatureKeys = configurationSignatureKeys,
            isConfigurationSignatureEnforced = isConfigurationSignatureEnforced
        )

        fun withConfigurationSignatureKey(vararg key: String) = apply {
            configurationSignatureKeys = key.mapIndexed { index, value -> index.toString() to value }.toMap()
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
        const val FAKE_NOMAD_PROFILES_HOST = "https://nomad.example.com"
    }
}
