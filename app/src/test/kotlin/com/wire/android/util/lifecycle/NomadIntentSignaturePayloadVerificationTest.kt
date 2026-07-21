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
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * Manual verification test: checks whether a real Nomad intent payload validates against the
 * PRODUCTION signature configuration, exactly as a shipped (prod/beta/internal) app would.
 *
 * It exercises the same code path a production app uses when it receives the startup intent:
 *   1. parse the `automated_login` JSON extra
 *   2. rebuild the signed message "wire=<backendConfig>,nomad=<nomadProfilesHost>"
 *   3. verify the Ed25519 `signatureNomadProfilesHost` against the production public keys
 *   4. validate that backendConfig and nomadProfilesHost are HTTPS URLs
 *
 * The keys below and `ENFORCE = true` mirror `default.json` (`configuration_signature_keys` /
 * `enforce_configuration_signature`) — i.e. a production-like app.
 *
 * HOW TO USE:
 *   1. Paste your intent payload JSON into [INTENT_PAYLOAD].
 *   2. Run: ./gradlew :app:testDevDebugUnitTest --tests "*NomadIntentSignaturePayloadVerificationTest*"
 *   3. Green  -> the signature is valid for a production app and Nomad login would proceed.
 *      Red    -> the signature (or a field) does NOT validate; the app would ignore the intent.
 */
class NomadIntentSignaturePayloadVerificationTest {

    @Test
    fun `given my payload, signature validates against production keys`() = runTest {
        val intent: Intent = mockk()
        every { intent.getStringExtra(any()) } returns null
        every { intent.getStringExtra("automated_login") } returns INTENT_PAYLOAD

        val processor = IntentsProcessor(
            nomadIntentSignatureValidator = NomadIntentSignatureValidator(
                configurationSignatureKeys = PRODUCTION_CONFIGURATION_SIGNATURE_KEYS,
                isConfigurationSignatureEnforced = ENFORCE_CONFIGURATION_SIGNATURE
            )
        )

        val result = processor(intent)

        assertNotNull(
            result,
            "Payload did NOT validate against the production configuration. " +
                "Either the Ed25519 signature is invalid for the signed message " +
                "\"wire=<backendConfig>,nomad=<nomadProfilesHost>\", a required field is missing, " +
                "or a URL is not HTTPS."
        )
    }

    private companion object {
        // Production signature configuration, mirrored from default.json.
        const val ENFORCE_CONFIGURATION_SIGNATURE = true
        val PRODUCTION_CONFIGURATION_SIGNATURE_KEYS: Map<String, String> = mapOf(
            "0" to "MCowBQYDK2VwAyEAxPrvUdt+531eDcnAFfLAv9K9gkxPDBMVEz75BkNgd1E=",
            "1" to "MCowBQYDK2VwAyEAA3lA6/1j+VJNKUsbHdlmTcjIOtjdnxzG4TkYr4nEFJw="
        )

        // Real intent payload (the `automated_login` JSON extra) under verification.
        const val INTENT_PAYLOAD = """
            
        """
    }
}
