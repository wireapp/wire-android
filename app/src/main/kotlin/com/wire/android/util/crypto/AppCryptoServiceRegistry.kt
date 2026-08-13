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
package com.wire.android.util.crypto

import java.security.Provider
import java.util.concurrent.ConcurrentHashMap

/**
 * Records which security provider actually served each cryptographic call site in the app module.
 *
 * Mirrors kalium's `CryptoServiceRegistry` for the lookups the app makes directly, which kalium cannot
 * see. A call site that has not run yet is absent rather than guessed at, so every entry the debug screen
 * shows is something that actually happened.
 */
object AppCryptoServiceRegistry {

    private val records = ConcurrentHashMap<AppCryptoUsage, AppCryptoServiceRecord>()

    /**
     * Notes that [usage] was served by [provider].
     *
     * @param lookup the lookup performed, as written in the source.
     */
    fun record(usage: AppCryptoUsage, lookup: String, algorithm: String, provider: Provider) {
        records[usage] = AppCryptoServiceRecord(
            lookup = lookup,
            algorithm = algorithm,
            providerName = provider.name,
            providerVersion = provider.versionString(),
        )
    }

    /** Every app call site observed so far, in [AppCryptoUsage] declaration order. */
    fun recorded(): List<Pair<AppCryptoUsage, AppCryptoServiceRecord>> =
        AppCryptoUsage.entries.mapNotNull { usage -> records[usage]?.let { usage to it } }

    /**
     * `Provider.getVersionStr()` needs API 28 and `Provider.getVersion()` is deprecated, so read the
     * version out of the provider's own property map, where it is registered under this key.
     */
    private fun Provider.versionString(): String = getProperty(PROVIDER_VERSION_PROPERTY).orEmpty()

    private const val PROVIDER_VERSION_PROPERTY = "Provider.id version"
}

/** Which security provider served an app call site, as observed when it ran. */
data class AppCryptoServiceRecord(
    val lookup: String,
    val algorithm: String,
    val providerName: String,
    val providerVersion: String,
)
