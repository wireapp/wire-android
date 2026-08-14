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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireRootDestination
import com.wire.android.ui.common.R as commonR
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
                state.databaseSecurity?.let { security ->
                    SectionHeader(stringResource(R.string.debug_settings_sqlcipher))
                    SettingsItem(
                        title = stringResource(R.string.debug_settings_sqlcipher_version),
                        text = security.sqlCipherVersion ?: stringResource(R.string.debug_settings_unavailable),
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

                state.keyAttestation?.apply { KeyAttestationDiagnosticsSection(this) }

                SectionHeader(stringResource(R.string.debug_settings_app_paths))
                state.appPaths.forEach { entry ->
                    SettingsItem(title = stringResource(entry.labelRes), text = entry.path)
                }
            }
        }
    )
}

@Composable
private fun KeyAttestationDiagnosticsSection(diagnostics: KeyAttestationDiagnostics) {
    var expanded by remember { mutableStateOf(false) }

    SectionHeader(stringResource(R.string.debug_settings_key_attestation))
    SettingsItem(
        title = stringResource(R.string.debug_settings_key_attestation_result),
        text = when (diagnostics) {
            is KeyAttestationDiagnostics.Success -> stringResource(R.string.debug_settings_key_attestation_success)
            is KeyAttestationDiagnostics.Failure -> stringResource(R.string.debug_settings_key_attestation_failed)
        },
        trailingIcon = if (expanded) commonR.drawable.ic_collapse else commonR.drawable.ic_expand_more,
        onRowPressed = Clickable { expanded = !expanded },
    )

    AnimatedVisibility(expanded) {
        Column {
            KeyAttestationDiagnosticsDetails(diagnostics)
        }
    }
}

@Composable
private fun KeyAttestationDiagnosticsDetails(diagnostics: KeyAttestationDiagnostics) {
    when (diagnostics) {
        is KeyAttestationDiagnostics.Failure -> {
            SettingsItem(
                title = stringResource(R.string.debug_settings_key_attestation_failure),
                text = listOf(diagnostics.exceptionType, diagnostics.message)
                    .filter(String::isNotBlank)
                    .joinToString(": "),
            )
        }

        is KeyAttestationDiagnostics.Success -> {
            SettingsItem(
                title = stringResource(R.string.debug_settings_key_attestation_chain_length),
                text = diagnostics.rawCertificateChainLength.toString(),
            )
            diagnostics.attestation?.let { attestation ->
                SectionHeader(stringResource(R.string.debug_settings_key_attestation_device_state))
                SettingsItem(
                    title = stringResource(R.string.debug_settings_key_attestation_security_level),
                    text = attestation.attestationSecurityLevel,
                )
                SettingsItem(
                    title = stringResource(R.string.debug_settings_key_attestation_keymaster_security_level),
                    text = attestation.keymasterSecurityLevel,
                )
                SettingsItem(
                    title = stringResource(R.string.debug_settings_key_attestation_verified_boot_state),
                    text = attestation.verifiedBootState ?: stringResource(R.string.debug_settings_unavailable),
                )
            }
            KeyInspectionItem(diagnostics.key)
            diagnostics.certificates.forEach { certificate ->
                SectionHeader(stringResource(R.string.debug_settings_key_attestation_certificate, certificate.index + 1))
                SettingsItem(title = stringResource(R.string.debug_settings_key_attestation_subject), text = certificate.subject)
                SettingsItem(title = stringResource(R.string.debug_settings_key_attestation_issuer), text = certificate.issuer)
                SettingsItem(
                    title = stringResource(R.string.debug_settings_key_attestation_signature_algorithm),
                    text = certificate.signatureAlgorithm
                )
                SettingsItem(title = stringResource(R.string.debug_settings_key_attestation_sha256), text = certificate.sha256Fingerprint)
            }
        }
    }
}

@Composable
private fun KeyInspectionItem(key: KeyInspection) {
    when (key) {
        is KeyInspection.Available -> {
            SettingsItem(title = stringResource(R.string.debug_settings_key_algorithm), text = key.algorithm)
            SettingsItem(title = stringResource(R.string.debug_settings_key_security_level), text = key.securityLevel)
        }

        is KeyInspection.Unavailable -> {
            SettingsItem(title = key.label, text = key.reason)
        }
    }
}

private val SqliteHeaderStatus.labelRes: Int
    get() = when (this) {
        SqliteHeaderStatus.PlainSqlite -> R.string.debug_settings_sqlcipher_header_plain
        is SqliteHeaderStatus.NotPlainSqlite -> R.string.debug_settings_sqlcipher_header_not_plain
        SqliteHeaderStatus.NotCreated -> R.string.debug_settings_sqlcipher_header_not_created
        SqliteHeaderStatus.Unavailable -> R.string.debug_settings_unavailable
    }
