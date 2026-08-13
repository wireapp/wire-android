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
package com.wire.android.ui.debug.securityproviders

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.feature.debug.CryptoUsage
import com.wire.kalium.logic.feature.debug.CryptoServiceUsage
import com.wire.kalium.logic.feature.debug.GetCryptoServiceReportUseCase
import com.wire.kalium.util.DebugKaliumApi
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.Provider
import java.security.Security

@OptIn(DebugKaliumApi::class)
class SecurityProvidersViewModel @Inject constructor(
    private val appPathsProvider: AppPathsProvider,
    private val appCryptoServicesProvider: AppCryptoServicesProvider,
    private val getCryptoServiceReport: GetCryptoServiceReportUseCase,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(SecurityProvidersViewState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val providers = withContext(dispatcherProvider.default()) {
                Security.getProviders().map { provider ->
                    SecurityProvider(
                        name = provider.name,
                        version = provider.versionString(),
                        info = provider.info,
                        entries = provider.entries
                            .map { (key, value) -> KeyValueEntry(key.toString(), value.toString()) }
                            .sortedBy(KeyValueEntry::key)
                    )
                }
            }
            val appCryptoServices = withContext(dispatcherProvider.io()) { appCryptoServicesProvider() }
            val cryptoServices = getCryptoServiceReport().map(CryptoServiceUsage::toRow) + appCryptoServices
            _state.update { current ->
                current.copy(
                    appPaths = appPathsProvider(),
                    cryptoServices = cryptoServices,
                    providers = providers,
                )
            }
        }
    }
}

@OptIn(DebugKaliumApi::class)
private fun CryptoServiceUsage.toRow() = CryptoServiceRow(
    labelRes = usage.labelRes(),
    lookup = lookup,
    algorithm = algorithm,
    providerName = providerName,
    providerVersion = providerVersion,
)

@OptIn(DebugKaliumApi::class)
@StringRes
private fun CryptoUsage.labelRes(): Int = when (this) {
    CryptoUsage.ASSET_ENCRYPTION_IV -> R.string.debug_settings_crypto_asset_iv
    CryptoUsage.ASSET_KEY -> R.string.debug_settings_crypto_asset_key
    CryptoUsage.ASSET_CIPHER -> R.string.debug_settings_crypto_asset_cipher
    CryptoUsage.DATABASE_SECRET -> R.string.debug_settings_crypto_database_secret
}

/** One cryptographic call site, and the provider that served it on this device. */
data class CryptoServiceRow(
    @StringRes val labelRes: Int,
    val lookup: String,
    val algorithm: String,
    val providerName: String,
    val providerVersion: String,
)

/**
 * `Provider.getVersionStr()` needs API 28 and `Provider.getVersion()` is deprecated, so read the version
 * straight out of the provider's own property map, where it is registered under this key.
 */
private const val PROVIDER_VERSION_PROPERTY = "Provider.id version"

private fun Provider.versionString(): String = getProperty(PROVIDER_VERSION_PROPERTY).orEmpty()

data class SecurityProvidersViewState(
    val appPaths: List<LabelledValue> = emptyList(),
    val cryptoServices: List<CryptoServiceRow> = emptyList(),
    val providers: List<SecurityProvider>? = null,
)
