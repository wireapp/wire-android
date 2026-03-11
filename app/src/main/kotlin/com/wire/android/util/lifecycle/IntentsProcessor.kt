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
import com.wire.android.config.NomadProfilesFeatureConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.io.encoding.Base64

data class AutomatedLoginViaSSO(
    val backendConfig: String? = null,
    val ssoCode: String? = null,
    val nomadProfilesHost: String? = null,
) {
    val isEmpty = backendConfig.isNullOrEmpty() &&
        ssoCode.isNullOrEmpty() &&
        nomadProfilesHost.isNullOrEmpty()
}

@Singleton
class IntentsProcessor internal constructor(
    private val nomadProfilesFeatureConfig: NomadProfilesFeatureConfig,
    private val configurationSignatureKeys: List<String>,
    private val isConfigurationSignatureEnforced: Boolean
) {

    @Inject
    constructor(
        nomadProfilesFeatureConfig: NomadProfilesFeatureConfig
    ) : this(
        nomadProfilesFeatureConfig = nomadProfilesFeatureConfig,
        configurationSignatureKeys = BuildConfig.CONFIGURATION_SIGNATURE_KEYS,
        isConfigurationSignatureEnforced = BuildConfig.ENFORCE_CONFIGURATION_SIGNATURE
    )

    companion object {
        private const val AUTOMATED_LOGIN = "automated_login"
        internal const val SKIP_SIGNATURE_VERIFICATION_TOKEN = "skip"
        private const val ED25519_DER_PUBLIC_KEY_HEADER_LENGTH = 12
        private const val ED25519_PUBLIC_KEY_LENGTH = 32
        private val ED25519_DER_PUBLIC_KEY_HEADER = byteArrayOf(
            0x30, 0x2A, 0x30, 0x05, 0x06, 0x03, 0x2B, 0x65, 0x70, 0x03, 0x21, 0x00
        )
    }

    @Suppress("ReturnCount")
    operator fun invoke(intent: Intent?): AutomatedLoginViaSSO? {
        @Serializable
        data class Parameters(
            val backendConfig: String? = null,
            val ssoCode: String? = null,
            val nomadProfilesHost: String? = null,
            val sigNomadProfilesHost: String? = null,
        )

        val parsed = runCatching {
            intent
                ?.getStringExtra(AUTOMATED_LOGIN)
                ?.let { Json.decodeFromString<Parameters>(it) }
        }.getOrNull() ?: return null

        if (!nomadProfilesFeatureConfig.isEnabled() || parsed.nomadProfilesHost.isNullOrEmpty()) {
            return null
        }

        if (parsed.ssoCode.isNullOrEmpty() || parsed.backendConfig.isNullOrEmpty()) {
            return null
        }

        return AutomatedLoginViaSSO(
            parsed.backendConfig,
            parsed.ssoCode,
            parsed.nomadProfilesHost,
        )
            .takeIf { !it.isEmpty }
            ?.takeIf {
                val validBackend = parsed.backendConfig == null || isValidHttpsUrl(parsed.backendConfig)
                val validNomadProfileHost = isValidHttpsUrl(parsed.nomadProfilesHost)
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
            signature == SKIP_SIGNATURE_VERIFICATION_TOKEN && !isConfigurationSignatureEnforced -> true
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
        return configurationSignatureKeys.any { b64DerKey ->
            runCatching {
                // DER-encoded Ed25519 public key: 12 bytes ASN.1 header + 32 bytes raw key
                val decodedKey = Base64.decode(b64DerKey.replace("\\s".toRegex(), ""))
                require(decodedKey.size == ED25519_DER_PUBLIC_KEY_HEADER_LENGTH + ED25519_PUBLIC_KEY_LENGTH)
                require(decodedKey.take(ED25519_DER_PUBLIC_KEY_HEADER_LENGTH).toByteArray().contentEquals(ED25519_DER_PUBLIC_KEY_HEADER))
                val rawKey = decodedKey
                    .drop(ED25519_DER_PUBLIC_KEY_HEADER_LENGTH)
                    .toByteArray()
                    .also { require(it.size == ED25519_PUBLIC_KEY_LENGTH) }
                    .toUByteArray()
                // verifyDetached returns Unit on success, throws InvalidSignatureException on failure
                Signature.verifyDetached(signatureBytes, messageBytes, rawKey)
                true
            }.getOrElse { false }
        }
    }
}
