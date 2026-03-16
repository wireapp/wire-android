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

import com.ionspin.kotlin.crypto.LibsodiumInitializer
import com.ionspin.kotlin.crypto.signature.Signature
import com.wire.android.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.io.encoding.Base64

@Singleton
class NomadIntentSignatureValidator internal constructor(
    private val configurationSignatureKeys: List<String>,
    private val isConfigurationSignatureEnforced: Boolean
) {

    @Inject
    constructor() : this(
        configurationSignatureKeys = BuildConfig.CONFIGURATION_SIGNATURE_KEYS,
        isConfigurationSignatureEnforced = BuildConfig.ENFORCE_CONFIGURATION_SIGNATURE
    )

    fun isValid(parameter: String?, signature: String?): Boolean {
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
        val signatureBytes = runCatching {
            Base64.decode(signature).toUByteArray()
        }.getOrElse { return false }
        // Any of the available keys can verify this signature.
        return configurationSignatureKeys.any { b64DerKey ->
            runCatching {
                // DER-encoded Ed25519 public key: 12 bytes ASN.1 header + 32 bytes raw key.
                val decodedKey = Base64.decode(b64DerKey.replace("\\s".toRegex(), ""))
                require(decodedKey.size == ED25519_DER_PUBLIC_KEY_HEADER_LENGTH + ED25519_PUBLIC_KEY_LENGTH)
                require(
                    decodedKey
                        .take(ED25519_DER_PUBLIC_KEY_HEADER_LENGTH)
                        .toByteArray()
                        .contentEquals(ED25519_DER_PUBLIC_KEY_HEADER)
                )
                val rawKey = decodedKey
                    .drop(ED25519_DER_PUBLIC_KEY_HEADER_LENGTH)
                    .toByteArray()
                    .also { require(it.size == ED25519_PUBLIC_KEY_LENGTH) }
                    .toUByteArray()
                // verifyDetached returns Unit on success, throws InvalidSignatureException on failure.
                Signature.verifyDetached(signatureBytes, messageBytes, rawKey)
                true
            }.getOrElse { false }
        }
    }

    companion object {
        internal const val SKIP_SIGNATURE_VERIFICATION_TOKEN = "skip"
        private const val ED25519_DER_PUBLIC_KEY_HEADER_LENGTH = 12
        private const val ED25519_PUBLIC_KEY_LENGTH = 32
        private val ED25519_DER_PUBLIC_KEY_HEADER = byteArrayOf(
            0x30, 0x2A, 0x30, 0x05, 0x06, 0x03, 0x2B, 0x65, 0x70, 0x03, 0x21, 0x00
        )
    }
}
