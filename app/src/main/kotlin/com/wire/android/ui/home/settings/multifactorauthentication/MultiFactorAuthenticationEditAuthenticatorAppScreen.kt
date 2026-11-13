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

package com.wire.android.ui.home.settings.multifactorauthentication

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.progress.CenteredCircularProgressBarIndicator
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import kotlinx.datetime.LocalDate

@Composable
internal fun MultiFactorAuthenticationEditAuthenticatorAppScreen(
    appId: String,
    overviewState: MultiFactorAuthenticationOverviewState,
    state: MultiFactorAuthenticationEditAuthenticatorAppState,
    name: TextFieldState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSave: () -> Unit,
    onRemove: () -> Unit,
) {
    val loaded = overviewState as? MultiFactorAuthenticationOverviewState.Loaded
    val apps = (loaded?.overview as? MultiFactorAuthenticationOverview.Enabled)?.authenticatorApps.orEmpty()
    val appExists = apps.any { it.id == appId }
    BackHandler(onBack = onBack)
    WireScaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                title = stringResource(R.string.multi_factor_authentication_edit_app_title),
                onNavigationPressed = onBack,
                elevation = dimensions().spacing0x,
            )
        },
        bottomBar = {
            val draft = name.text.toString().trim()
            val canSave = appExists && state.app != null && draft.isNotEmpty() && draft != state.app.name
            WirePrimaryButton(
                text = stringResource(R.string.label_save),
                onClick = onSave,
                state = if (canSave && !state.isSaving && state.result == null) WireButtonState.Default else WireButtonState.Disabled,
                loading = state.isSaving,
                modifier = Modifier.padding(dimensions().spacing16x),
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (overviewState) {
                MultiFactorAuthenticationOverviewState.Loading -> CenteredCircularProgressBarIndicator()
                MultiFactorAuthenticationOverviewState.Error -> AuthenticatorAppsLoadError(onRetry, Modifier.align(Alignment.Center))
                is MultiFactorAuthenticationOverviewState.Loaded -> {
                    if (!appExists) {
                        AuthenticatorAppsDescription(stringResource(R.string.multi_factor_authentication_authenticator_app_unavailable))
                    } else if (state.app == null) {
                        CenteredCircularProgressBarIndicator()
                    } else {
                        EditAuthenticatorAppFields(state, name, apps.size, onRemove)
                    }
                }
            }
        }
    }
}

@Composable
private fun EditAuthenticatorAppFields(
    state: MultiFactorAuthenticationEditAuthenticatorAppState,
    name: TextFieldState,
    appCount: Int,
    onRemove: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(dimensions().spacing16x),
        verticalArrangement = Arrangement.spacedBy(dimensions().spacing16x),
    ) {
        WireTextField(
            textState = name,
            labelText = stringResource(R.string.multi_factor_authentication_app_name),
            state = if (state.isSaving) WireTextFieldState.Disabled else WireTextFieldState.Default,
        )
        Text(
            text = stringResource(R.string.multi_factor_authentication_app_name_description),
            style = MaterialTheme.wireTypography.body01,
            color = MaterialTheme.wireColorScheme.secondaryText,
        )
        if (appCount > 1) {
            WireSecondaryButton(
                text = stringResource(R.string.multi_factor_authentication_remove_app),
                onClick = onRemove,
                state = if (state.isSaving) WireButtonState.Disabled else WireButtonState.Error,
            )
        } else {
            Text(
                text = stringResource(R.string.multi_factor_authentication_only_app_edit_description),
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.secondaryText,
            )
        }
        if (state.hasError) {
            Text(
                text = stringResource(R.string.multi_factor_authentication_rename_app_error),
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.error,
            )
        }
    }
}

@PreviewMultipleThemes
@Composable
private fun PreviewEditAuthenticatorApp() = WireTheme {
    val app = AuthenticatorApp("phone", "Android phone", LocalDate(2026, 10, 14))
    MultiFactorAuthenticationEditAuthenticatorAppScreen(
        appId = app.id,
        overviewState = MultiFactorAuthenticationOverviewState.Loaded(
            MultiFactorAuthenticationOverview.Enabled(listOf(app), RecoveryCodesStatus(10, 10))
        ),
        state = MultiFactorAuthenticationEditAuthenticatorAppState(app = app),
        name = remember { TextFieldState(app.name) },
        onBack = {},
        onRetry = {},
        onSave = {},
        onRemove = {},
    )
}
