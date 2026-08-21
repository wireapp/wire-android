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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireRootDestination
import com.wire.android.ui.common.rememberTopBarElevationState
import com.wire.android.ui.common.rowitem.SectionHeader
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.topappbar.WireTopAppBarTitle
import com.wire.android.ui.common.typography
import com.wire.android.ui.debug.securityProvidersViewModel
import com.wire.android.ui.home.settings.SettingsItem

@WireRootDestination
@Composable
fun SecurityProvidersScreen(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: SecurityProvidersViewModel = securityProvidersViewModel(),
) {
    val scrollState = rememberScrollState()

    WireScaffold(
        modifier = modifier,
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = scrollState.rememberTopBarElevationState().value,
                titleContent = {
                    WireTopAppBarTitle(
                        title = stringResource(R.string.debug_settings_security_diagnostics),
                        style = typography().title01,
                        maxLines = 2
                    )
                },
                navigationIconType = NavigationIconType.Close(R.string.content_description_conversation_details_close_btn),
                onNavigationPressed = {
                    navigator.navigateBack()
                }
            )
        },
        content = { paddingValues ->

            val state by viewModel.state.collectAsState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
            ) {
                SectionHeader(stringResource(R.string.debug_settings_app_paths))
                state.appPaths.forEach { entry ->
                    SettingsItem(title = stringResource(entry.labelRes), text = entry.value)
                }

                SectionHeader(stringResource(R.string.debug_settings_entropy_sources))
                if (state.cryptoServices?.isEmpty() == true) {
                    SettingsItem(text = stringResource(R.string.debug_settings_crypto_services_empty))
                }
                state.cryptoServices?.forEach { row ->
                    CryptoServiceListItem(row)
                }

                state.network?.let { network ->
                    SectionHeader(stringResource(R.string.debug_settings_network))
                    NetworkSection(network)
                }

                state.databaseSecurity?.let { security ->
                    SectionHeader(stringResource(R.string.debug_settings_sqlcipher))
                    SQLSecuritySection(security)
                }
            }
        }
    )
}

@Composable
private fun SQLSecuritySection(security: DatabaseSecurityInfo) {
    SettingsItem(
        title = stringResource(R.string.debug_settings_sqlcipher_version),
        text = security.sqlCipherVersion ?: stringResource(R.string.debug_settings_network_unknown),
    )
    SettingsItem(
        title = stringResource(R.string.debug_settings_sqlcipher_database_path),
        text = security.userDatabase.path,
    )
    SettingsItem(
        title = stringResource(R.string.debug_settings_sqlcipher_header_value),
        text = security.userDatabase.header.value,
    )
    SettingsItem(
        title = stringResource(R.string.debug_settings_sqlcipher_header_interpretation),
        text = stringResource(security.userDatabase.header.labelRes),
    )
    SettingsItem(
        title = stringResource(R.string.debug_settings_sqlcipher_internal_storage),
        text = security.userDatabase.isInInternalDataDirectory.toString(),
    )
}

@Composable
private fun NetworkSection(network: NetworkDiagnostics) {
    val unknown = stringResource(R.string.debug_settings_network_unknown)

    SettingsItem(
        title = stringResource(R.string.debug_settings_network_vpn),
        text = stringResource(
            if (network.isVpn) R.string.debug_settings_network_vpn_active else R.string.debug_settings_network_vpn_inactive
        ),
    )
    SettingsItem(
        title = stringResource(R.string.debug_settings_network_type),
        text = network.networkTypes.joinToString().ifEmpty { unknown },
    )
    SettingsItem(
        title = stringResource(R.string.debug_settings_network_backend_host),
        text = network.backendHost.ifEmpty { unknown },
    )

    val addressesLabel = stringResource(R.string.debug_settings_network_resolved_addresses)
    when (val addresses = network.addresses) {
        is AddressResolution.Resolved -> if (addresses.addresses.isEmpty()) {
            SettingsItem(title = addressesLabel, text = unknown)
        } else {
            addresses.addresses.forEach { resolved ->
                SettingsItem(
                    title = stringResource(
                        R.string.debug_settings_network_resolved_address,
                        stringResource(resolved.version.labelRes())
                    ),
                    text = resolved.address,
                )
            }
        }

        AddressResolution.NoActiveNetwork -> SettingsItem(
            title = addressesLabel,
            text = stringResource(R.string.debug_settings_network_no_active_network),
        )

        AddressResolution.Failed -> SettingsItem(
            title = addressesLabel,
            text = stringResource(R.string.debug_settings_network_resolution_failed),
        )
    }
}

@StringRes
private fun IpVersion.labelRes(): Int = when (this) {
    IpVersion.V4 -> R.string.debug_settings_network_ip_v4
    IpVersion.V6 -> R.string.debug_settings_network_ip_v6
}

private val SqliteHeaderStatus.labelRes: Int
    get() = when (this) {
        SqliteHeaderStatus.PlainSqlite -> R.string.debug_settings_sqlcipher_header_plain
        is SqliteHeaderStatus.NotPlainSqlite -> R.string.debug_settings_sqlcipher_header_not_plain
        SqliteHeaderStatus.NotCreated -> R.string.debug_settings_sqlcipher_header_not_created
        SqliteHeaderStatus.Unavailable -> R.string.debug_settings_network_unknown
    }
