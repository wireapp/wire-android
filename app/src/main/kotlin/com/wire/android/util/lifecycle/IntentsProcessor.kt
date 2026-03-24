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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

data class AutomatedLoginViaSSO(
    val backendConfig: String? = null,
    val ssoCode: String? = null,
    val nomadProfilesHost: String? = null,
)

@Singleton
class IntentsProcessor @Inject internal constructor(
    private val nomadIntentSignatureValidator: NomadIntentSignatureValidator
) {

    companion object {
        private const val AUTOMATED_LOGIN = "automated_login"
        internal const val SKIP_SIGNATURE_VERIFICATION_TOKEN = NomadIntentSignatureValidator.SKIP_SIGNATURE_VERIFICATION_TOKEN
    }

    @Suppress("ReturnCount")
    operator fun invoke(intent: Intent?): AutomatedLoginViaSSO? {
        @Serializable
        data class Parameters(
            val backendConfig: String? = null,
            val ssoCode: String? = null,
            val nomadProfilesHost: String? = null,
            val signatureNomadProfilesHost: String? = null,
        )

        val parsed = runCatching {
            intent
                ?.getStringExtra(AUTOMATED_LOGIN)
                ?.let { Json.decodeFromString<Parameters>(it) }
        }.getOrNull() ?: return null

        if (parsed.nomadProfilesHost.isNullOrEmpty()) {
            return null
        }

        if (parsed.ssoCode.isNullOrEmpty() || parsed.backendConfig.isNullOrEmpty()) {
            return null
        }

        val signedPayload = createSignedPayload(
            backendConfig = parsed.backendConfig,
            nomadProfilesHost = parsed.nomadProfilesHost
        )
        if (!nomadIntentSignatureValidator.isValid(signedPayload, parsed.signatureNomadProfilesHost)) {
            return null
        }

        return AutomatedLoginViaSSO(
            parsed.backendConfig,
            parsed.ssoCode,
            parsed.nomadProfilesHost,
        ).takeIf {
                val validBackend = isValidHttpsUrl(parsed.backendConfig)
                val validNomadProfileHost = isValidHttpsUrl(parsed.nomadProfilesHost)
                validBackend && validNomadProfileHost
            }
    }

    private fun isValidHttpsUrl(url: String): Boolean {
        // Validate that backendConfig is a valid HTTPS URL (HTTP is not allowed for security)
        val uri = runCatching { URI(url) }.getOrNull()
        return uri?.scheme?.lowercase() == "https" && !uri.host.isNullOrEmpty()
    }

    private fun createSignedPayload(
        backendConfig: String,
        nomadProfilesHost: String
    ): String = "wire=$backendConfig,nomad=$nomadProfilesHost"
}
