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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.progress.CenteredCircularProgressBarIndicator
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.conversations.details.options.ArrowType
import com.wire.android.ui.home.conversations.details.options.GroupConversationOptionsItem
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

@Composable
fun MultiFactorAuthenticationScreen(
    onBackPressed: () -> Unit,
    viewModel: MultiFactorAuthenticationViewModel,
    onSetUp: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()
    val placeholderMessage = stringResource(R.string.multi_factor_authentication_flow_placeholder)
    MultiFactorAuthenticationScreenContent(
        state = state,
        onBackPressed = onBackPressed,
        onRetry = viewModel::refresh,
        onAction = { action ->
            if (action == MultiFactorAuthenticationAction.SetUp) {
                onSetUp()
            } else {
                coroutineScope.launch { snackbarHostState.showSnackbar(placeholderMessage) }
            }
        },
    )
}

@Composable
internal fun MultiFactorAuthenticationScreenContent(
    state: MultiFactorAuthenticationOverviewState,
    onBackPressed: () -> Unit,
    onRetry: () -> Unit,
    onAction: (MultiFactorAuthenticationAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    WireScaffold(
        modifier = modifier,
        topBar = {
            WireCenterAlignedTopAppBar(
                title = stringResource(R.string.settings_multi_factor_authentication_label),
                onNavigationPressed = onBackPressed,
                elevation = dimensions().spacing0x,
            )
        }
    ) { internalPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(internalPadding)
        ) {
            when (state) {
                MultiFactorAuthenticationOverviewState.Loading -> CenteredCircularProgressBarIndicator()
                MultiFactorAuthenticationOverviewState.Error -> OverviewError(
                    onRetry = onRetry,
                    modifier = Modifier.align(Alignment.Center),
                )
                is MultiFactorAuthenticationOverviewState.Loaded -> OverviewContent(state, onRetry, onAction)
            }
        }
    }
}

@Composable
private fun OverviewContent(
    state: MultiFactorAuthenticationOverviewState.Loaded,
    onRetry: () -> Unit,
    onAction: (MultiFactorAuthenticationAction) -> Unit,
) {
    val overview = state.overview
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            if (state.isRefreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            GroupConversationOptionsItem(
                title = stringResource(R.string.settings_multi_factor_authentication_label),
                arrowLabel = stringResource(
                    if (overview is MultiFactorAuthenticationOverview.Enabled) R.string.label_on else R.string.label_off
                ),
                arrowType = ArrowType.NONE,
            )
            Text(
                text = stringResource(R.string.multi_factor_authentication_description),
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.secondaryText,
                modifier = Modifier.padding(dimensions().spacing16x),
            )
            if (overview is MultiFactorAuthenticationOverview.Enabled) {
                GroupConversationOptionsItem(
                    title = stringResource(R.string.multi_factor_authentication_authenticator_apps),
                    subtitle = pluralStringResource(
                        R.plurals.multi_factor_authentication_authenticator_apps_count,
                        overview.authenticatorApps.size,
                        overview.authenticatorApps.size,
                    ),
                    clickable = Clickable { onAction(MultiFactorAuthenticationAction.ManageAuthenticatorApps) },
                )
                GroupConversationOptionsItem(
                    title = stringResource(R.string.multi_factor_authentication_recovery_codes),
                    subtitle = stringResource(
                        R.string.multi_factor_authentication_recovery_codes_remaining,
                        overview.recoveryCodesStatus.remaining,
                        overview.recoveryCodesStatus.total,
                    ),
                    clickable = Clickable { onAction(MultiFactorAuthenticationAction.ManageRecoveryCodes) },
                )
            }
            if (state.hasRefreshError) {
                OverviewError(onRetry = onRetry)
            }
        }
        if (overview is MultiFactorAuthenticationOverview.Disabled) {
            WirePrimaryButton(
                text = stringResource(R.string.multi_factor_authentication_set_up),
                onClick = { onAction(MultiFactorAuthenticationAction.SetUp) },
                modifier = Modifier.padding(dimensions().spacing16x),
            )
        } else {
            WireSecondaryButton(
                text = stringResource(R.string.multi_factor_authentication_turn_off),
                onClick = { onAction(MultiFactorAuthenticationAction.TurnOff) },
                state = WireButtonState.Error,
                modifier = Modifier.padding(dimensions().spacing16x),
            )
        }
    }
}

@Composable
private fun OverviewError(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(dimensions().spacing16x),
        verticalArrangement = Arrangement.spacedBy(dimensions().spacing16x),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.multi_factor_authentication_load_error),
            style = MaterialTheme.wireTypography.body01,
            color = MaterialTheme.wireColorScheme.error,
        )
        WireSecondaryButton(text = stringResource(R.string.label_retry), onClick = onRetry)
    }
}

@PreviewMultipleThemes
@Composable
private fun PreviewMultiFactorAuthenticationDisabled() = WireTheme {
    OverviewPreview(MultiFactorAuthenticationOverviewState.Loaded(MultiFactorAuthenticationOverview.Disabled))
}

@PreviewMultipleThemes
@Composable
private fun PreviewMultiFactorAuthenticationEnabled() = WireTheme {
    OverviewPreview(
        MultiFactorAuthenticationOverviewState.Loaded(
            MultiFactorAuthenticationOverview.Enabled(
                authenticatorApps = listOf(AuthenticatorApp("phone", "Android phone", LocalDate(2026, 10, 14))),
                recoveryCodesStatus = RecoveryCodesStatus(remaining = 9, total = 10),
            )
        )
    )
}

@PreviewMultipleThemes
@Composable
private fun PreviewMultiFactorAuthenticationLoading() = WireTheme {
    OverviewPreview(MultiFactorAuthenticationOverviewState.Loading)
}

@PreviewMultipleThemes
@Composable
private fun PreviewMultiFactorAuthenticationError() = WireTheme {
    OverviewPreview(MultiFactorAuthenticationOverviewState.Error)
}

@Composable
private fun OverviewPreview(state: MultiFactorAuthenticationOverviewState) {
    MultiFactorAuthenticationScreenContent(state, onBackPressed = {}, onRetry = {}, onAction = {})
}
