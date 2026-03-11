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
import java.net.URI
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

data class AutomatedLoginViaSSO(
    val backendConfig: String? = null,
    val ssoCode: String? = null
) {
    val isEmpty = backendConfig == null && ssoCode == null
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
            val ssoCode: String? = null
        )

        val automatedLoginParameter = intent?.getStringExtra(AUTOMATED_LOGIN) ?: return null
        return runCatching {
            val parsed = Json.decodeFromString<Parameters>(automatedLoginParameter)
            AutomatedLoginViaSSO(parsed.backendConfig, parsed.ssoCode)
        }.getOrNull()
            ?.takeIf { !it.isEmpty }
            ?.takeIf { params ->
                params.backendConfig == null || isValidHttpsUrl(params.backendConfig)
            }
    }

    private fun isValidHttpsUrl(url: String): Boolean {
        // Validate that backendConfig is a valid HTTPS URL (HTTP is not allowed for security)
        val uri = runCatching { URI(url) }.getOrNull()
        return uri?.scheme?.lowercase() == "https" && !uri.host.isNullOrEmpty()
    }
}
