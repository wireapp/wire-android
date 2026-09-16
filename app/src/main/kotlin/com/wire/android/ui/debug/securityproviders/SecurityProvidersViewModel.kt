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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.util.crypto.AppCryptoServiceInfo
import com.wire.android.util.crypto.appCryptoServices
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.feature.debug.CryptoServiceUsage
import com.wire.kalium.logic.feature.debug.GetCryptoServiceReportUseCase
import com.wire.kalium.logic.feature.debug.GetSqlCipherVersionUseCase
import com.wire.kalium.logic.feature.user.SelfServerConfigUseCase
import com.wire.kalium.network.NetworkStateObserver
import com.wire.kalium.util.DebugKaliumApi
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(DebugKaliumApi::class)
class SecurityProvidersViewModel @Inject constructor(
    private val appPathsProvider: AppPathsProvider,
    private val getCryptoServiceReport: GetCryptoServiceReportUseCase,
    private val networkDiagnosticsProvider: NetworkDiagnosticsProvider,
    private val networkStateObserver: NetworkStateObserver,
    private val selfServerConfig: SelfServerConfigUseCase,
    private val getSqlCipherVersion: GetSqlCipherVersionUseCase,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(SecurityProvidersViewState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val databaseSecurity = withContext(dispatchers.io()) {
                DatabaseSecurityInfo(
                    sqlCipherVersion = getSqlCipherVersion(),
                    userDatabase = appPathsProvider.userDatabaseSecurityStatus(),
                )
            }
            val appServices = withContext(dispatchers.io()) { appCryptoServices() }
            val cryptoServices = getCryptoServiceReport().map(CryptoServiceUsage::toRow) +
                    appServices.map(AppCryptoServiceInfo::toRow)
            _state.update { current ->
                current.copy(
                    appPaths = appPathsProvider(),
                    cryptoServices = cryptoServices,
                    databaseSecurity = databaseSecurity
                )
            }
        }
        viewModelScope.launch {
            observeNetworkDiagnostics()
        }
    }

    private suspend fun observeNetworkDiagnostics() {
        val apiUrl = apiUrl() ?: return
        networkStateObserver.observeCurrentNetwork()
            .map { networkDiagnosticsProvider(apiUrl) }
            .flowOn(dispatchers.io())
            .collect { diagnostics -> _state.update { current -> current.copy(network = diagnostics) } }
    }

    private suspend fun apiUrl(): String? = when (val result = selfServerConfig()) {
        is SelfServerConfigUseCase.Result.Success -> result.serverLinks.links.api
        is SelfServerConfigUseCase.Result.Failure -> {
            appLogger.w("Could not read the server config, skipping network diagnostics")
            null
        }
    }
}

private fun AppCryptoServiceInfo.toRow() = CryptoServiceRow(
    label = name,
    lookup = lookup,
    algorithm = algorithm,
    providerName = providerName,
    providerVersion = providerVersion,
)

@OptIn(DebugKaliumApi::class)
private fun CryptoServiceUsage.toRow() = CryptoServiceRow(
    label = name,
    lookup = lookup,
    algorithm = algorithm,
    providerName = providerName,
    providerVersion = providerVersion,
)

/** One cryptographic lookup, and the provider that serves it on this device. */
data class CryptoServiceRow(
    val label: String,
    val lookup: String,
    val algorithm: String,
    val providerName: String,
    val providerVersion: String,
)

data class SecurityProvidersViewState(
    val appPaths: List<LabelledValue> = emptyList(),
    val network: NetworkDiagnostics? = null,
    val cryptoServices: List<CryptoServiceRow>? = null,
    val databaseSecurity: DatabaseSecurityInfo? = null,
)

data class DatabaseSecurityInfo(
    val sqlCipherVersion: String?,
    val userDatabase: UserDatabaseSecurityStatus,
)
