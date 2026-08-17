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
import com.wire.android.util.crypto.AppCryptoServiceInfo
import com.wire.android.util.crypto.appCryptoServices
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.feature.debug.CryptoServiceUsage
import com.wire.kalium.logic.feature.debug.GetCryptoServiceReportUseCase
import com.wire.kalium.util.DebugKaliumApi
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(DebugKaliumApi::class)
class SecurityProvidersViewModel @Inject constructor(
    private val appPathsProvider: AppPathsProvider,
    private val getCryptoServiceReport: GetCryptoServiceReportUseCase,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(SecurityProvidersViewState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val appServices = withContext(dispatcherProvider.io()) { appCryptoServices() }
            val cryptoServices = getCryptoServiceReport().map(CryptoServiceUsage::toRow) +
                    appServices.map(AppCryptoServiceInfo::toRow)
            _state.update { current ->
                current.copy(
                    appPaths = appPathsProvider(),
                    cryptoServices = cryptoServices,
                )
            }
        }
    }
}

@OptIn(DebugKaliumApi::class)
private fun CryptoServiceUsage.toRow() = CryptoServiceRow(
    label = name,
    lookup = lookup,
    algorithm = algorithm,
    providerName = providerName,
    providerVersion = providerVersion,
)

private fun AppCryptoServiceInfo.toRow() = CryptoServiceRow(
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
    val cryptoServices: List<CryptoServiceRow> = emptyList(),
)
