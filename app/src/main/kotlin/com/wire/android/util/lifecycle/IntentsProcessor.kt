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
import com.ionspin.kotlin.crypto.LibsodiumInitializer
import com.ionspin.kotlin.crypto.signature.Signature
import com.wire.android.BuildConfig
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.io.encoding.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class AutomatedLoginViaSSO(
    val backendConfig: String? = null,
    val ssoCode: String? = null,
    val nomadProfilesHost: String? = null
) {
    val isEmpty = backendConfig.isNullOrEmpty() && ssoCode.isNullOrEmpty() && nomadProfilesHost.isNullOrEmpty()
}

@Singleton
class IntentsProcessor @Inject constructor() {

    companion object {
        private const val AUTOMATED_LOGIN = "automated_login"
    }

    operator fun invoke(intent: Intent?): AutomatedLoginViaSSO? {
        @Serializable
        data class Parameters(
            val backendConfig: String? = null,
            val ssoCode: String? = null,
            val nomadProfilesHost: String? = null,
            val sigNomadProfilesHost: String? = null
        )

        val parsed = runCatching {
            intent
                ?.getStringExtra(AUTOMATED_LOGIN)
                ?.let { Json.decodeFromString<Parameters>(it) }
        }.getOrNull() ?: return null

        return AutomatedLoginViaSSO(
            parsed.backendConfig,
            parsed.ssoCode,
            if (BuildConfig.NOMAD_PROFILES_AVAILABLE) parsed.nomadProfilesHost else null
        )
            .takeIf { !it.isEmpty }
            ?.takeIf {
                val validBackend = parsed.backendConfig == null || isValidHttpsUrl(parsed.backendConfig)
                val validNomadProfileHost = parsed.nomadProfilesHost == null || isValidHttpsUrl(parsed.nomadProfilesHost)
                val validSignature = isValidSignature(parsed.nomadProfilesHost, parsed.sigNomadProfilesHost)
                validBackend && validNomadProfileHost && validSignature
            }
    }

    private fun isValidHttpsUrl(url: String): Boolean {
        // Validate that backendConfig is a valid HTTPS URL (HTTP is not allowed for security)
        val uri = runCatching { URI(url) }.getOrNull()
        return uri?.scheme?.lowercase() == "https" && !uri.host.isNullOrEmpty()
    }

    private fun isValidSignature(parameter: String?, signature: String?): Boolean {
        return when {
            parameter == null -> signature == null
            signature == "skip" && !BuildConfig.ENFORCE_CONFIGURATION_SIGNATURE -> true
            signature != null -> verifySignatureWithAvailableKeys(parameter, signature)
            else -> false
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    private fun verifySignatureWithAvailableKeys(parameter: String, signature: String): Boolean {
        if (!LibsodiumInitializer.isInitialized()) LibsodiumInitializer.initializeWithCallback {}
        val messageBytes = parameter.toByteArray().toUByteArray()
        val signatureBytes = Base64.decode(signature).toUByteArray()
        // Any of the available keys can verify this signature
        return BuildConfig.CONFIGURATION_SIGNATURE_KEYS.any { b64DerKey ->
            runCatching {
                // DER-encoded Ed25519 public key: 12 bytes ASN.1 header + 32 bytes raw key
                val rawKey = Base64.decode(b64DerKey.replace("\\s".toRegex(), ""))
                    .drop(12).toByteArray().toUByteArray()
                // verifyDetached returns Unit on success, throws InvalidSignatureException on failure
                Signature.verifyDetached(signatureBytes, messageBytes, rawKey)
                true
            }.getOrElse { false }
        }
    }
}
