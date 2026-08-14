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

import com.wire.android.datastore.EncryptionManager
import com.wire.android.feature.e2ei.OAuthUseCase
import java.security.Provider

/**
 * Which security provider serves each cryptographic lookup in the app module.
 *
 * Mirrors kalium's `cryptoServices()` for the lookups the app makes directly, which kalium cannot see.
 * Which implementation backs an algorithm is decided at runtime by walking the installed security
 * providers, so it varies per device, per OEM and per OS version. Each contributing function performs the
 * same lookups its call sites perform, from the same file and with the same algorithm constants, and reads
 * the provider off what comes back.
 *
 * Only for the security providers debug screen. Performs lookups and nothing else: no key is persisted and
 * no crypto state is mutated.
 *
 * `SecureRandom` lookups can block while the platform gathers entropy, so call this off the main thread.
 */
fun appCryptoServices(): List<AppCryptoServiceInfo> =
    EncryptionManager.cryptoServices() + OAuthUseCase.cryptoServices()

/**
 * Performs [resolve] and reads the algorithm and provider off the instance it returned, so what is reported
 * is what the platform actually handed back.
 *
 * Null when the lookup fails: a debug screen must not bring down the caller over a missing algorithm.
 *
 * @param name what the lookup is for, e.g. `DataStore cipher`.
 * @param lookup the lookup performed, as written in the source. Interpolate the same constants [resolve]
 * uses, so this cannot describe a lookup the call sites do not make.
 * @param resolve the JCA lookup, returning its result's `algorithm` and `provider`.
 */
fun appCryptoServiceInfo(name: String, lookup: String, resolve: () -> Pair<String, Provider>): AppCryptoServiceInfo? =
    runCatching(resolve).getOrNull()?.let { (algorithm, provider) ->
        AppCryptoServiceInfo(
            name = name,
            lookup = lookup,
            algorithm = algorithm,
            providerName = provider.name,
            providerVersion = provider.versionString(),
        )
    }

/**
 * Which security provider serves one cryptographic lookup, read off the instance the platform returned.
 *
 * @param name what the lookup is for, e.g. `DataStore cipher`.
 * @param lookup the lookup performed, as written in the source, e.g. `KeyGenerator.getInstance("AES")`.
 * @param algorithm the algorithm the resolved instance reports, e.g. `AES/GCM/NoPadding`.
 */
data class AppCryptoServiceInfo(
    val name: String,
    val lookup: String,
    val algorithm: String,
    val providerName: String,
    val providerVersion: String,
)

/**
 * `Provider.getVersionStr()` needs API 28 and `Provider.getVersion()` is deprecated, so read the version
 * out of the provider's own property map, where it is registered under this key.
 */
private fun Provider.versionString(): String = getProperty(PROVIDER_VERSION_PROPERTY).orEmpty()

private const val PROVIDER_VERSION_PROPERTY = "Provider.id version"
