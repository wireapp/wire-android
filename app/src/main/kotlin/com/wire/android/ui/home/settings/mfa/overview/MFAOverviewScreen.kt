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

package com.wire.android.ui.home.settings.mfa.overview

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
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.conversations.details.options.ArrowType
import com.wire.android.ui.home.conversations.details.options.GroupConversationOptionsItem
import com.wire.android.ui.home.settings.mfa.AuthenticatorApp
import com.wire.android.ui.home.settings.mfa.MFAOverview
import com.wire.android.ui.home.settings.mfa.RecoveryCodesStatus
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import kotlinx.datetime.LocalDate

@Composable
fun MFAOverviewScreen(
    onBackPressed: () -> Unit,
    viewModel: MFAOverviewViewModel,
    onSetUp: () -> Unit,
    onManageRecoveryCodes: (RecoveryCodesStatus) -> Unit,
    onTurnOff: () -> Unit,
    onManageAuthenticatorApps: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MFAOverviewScreenContent(
        state = state,
        onBackPressed = onBackPressed,
        onRetry = viewModel::refresh,
        onAction = { action ->
            when (action) {
                MFAOverviewAction.SetUp -> onSetUp()
                MFAOverviewAction.TurnOff -> onTurnOff()
                MFAOverviewAction.ManageAuthenticatorApps -> onManageAuthenticatorApps()
                MFAOverviewAction.ManageRecoveryCodes -> {
                    val loaded = state as? MFAOverviewState.Loaded
                    (loaded?.overview as? MFAOverview.Enabled)?.let {
                        onManageRecoveryCodes(it.recoveryCodesStatus)
                    }
                }
            }
        },
    )
}

@Composable
internal fun MFAOverviewScreenContent(
    state: MFAOverviewState,
    onBackPressed: () -> Unit,
    onRetry: () -> Unit,
    onAction: (MFAOverviewAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    WireScaffold(
        modifier = modifier,
        topBar = {
            WireCenterAlignedTopAppBar(
                title = stringResource(R.string.settings_mfa_label),
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
                MFAOverviewState.Loading -> CenteredCircularProgressBarIndicator()
                MFAOverviewState.Error -> OverviewError(
                    onRetry = onRetry,
                    modifier = Modifier.align(Alignment.Center),
                )
                is MFAOverviewState.Loaded -> OverviewContent(state, onRetry, onAction)
            }
        }
    }
}

@Composable
private fun OverviewContent(
    state: MFAOverviewState.Loaded,
    onRetry: () -> Unit,
    onAction: (MFAOverviewAction) -> Unit,
) {
    val overview = state.overview
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            if (state.isRefreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            GroupConversationOptionsItem(
                title = stringResource(R.string.settings_mfa_label),
                arrowLabel = stringResource(
                    if (overview is MFAOverview.Enabled) R.string.label_on else R.string.label_off
                ),
                arrowType = ArrowType.NONE,
            )
            Text(
                text = stringResource(R.string.mfa_description),
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.secondaryText,
                modifier = Modifier.padding(dimensions().spacing16x),
            )
            if (overview is MFAOverview.Enabled) {
                GroupConversationOptionsItem(
                    title = stringResource(R.string.mfa_authenticator_apps),
                    subtitle = pluralStringResource(
                        R.plurals.mfa_authenticator_apps_count,
                        overview.authenticatorApps.size,
                        overview.authenticatorApps.size,
                    ),
                    clickable = Clickable { onAction(MFAOverviewAction.ManageAuthenticatorApps) },
                )
                GroupConversationOptionsItem(
                    title = stringResource(R.string.mfa_recovery_codes),
                    subtitle = stringResource(
                        R.string.mfa_recovery_codes_remaining,
                        overview.recoveryCodesStatus.remaining,
                        overview.recoveryCodesStatus.total,
                    ),
                    clickable = Clickable { onAction(MFAOverviewAction.ManageRecoveryCodes) },
                )
            }
            if (state.hasRefreshError) {
                OverviewError(onRetry = onRetry)
            }
        }
        if (overview is MFAOverview.Disabled) {
            WirePrimaryButton(
                text = stringResource(R.string.mfa_set_up),
                onClick = { onAction(MFAOverviewAction.SetUp) },
                modifier = Modifier.padding(dimensions().spacing16x),
            )
        } else {
            WireSecondaryButton(
                text = stringResource(R.string.mfa_turn_off),
                onClick = { onAction(MFAOverviewAction.TurnOff) },
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
            text = stringResource(R.string.mfa_load_error),
            style = MaterialTheme.wireTypography.body01,
            color = MaterialTheme.wireColorScheme.error,
        )
        WireSecondaryButton(text = stringResource(R.string.label_retry), onClick = onRetry)
    }
}

@PreviewMultipleThemes
@Composable
private fun PreviewMFAOverviewDisabled() = WireTheme {
    OverviewPreview(MFAOverviewState.Loaded(MFAOverview.Disabled))
}

@PreviewMultipleThemes
@Composable
private fun PreviewMFAOverviewEnabled() = WireTheme {
    OverviewPreview(
        MFAOverviewState.Loaded(
            MFAOverview.Enabled(
                authenticatorApps = listOf(AuthenticatorApp("phone", "Android phone", LocalDate(2026, 10, 14))),
                recoveryCodesStatus = RecoveryCodesStatus(remaining = 9, total = 10),
            )
        )
    )
}

@PreviewMultipleThemes
@Composable
private fun PreviewMFAOverviewLoading() = WireTheme {
    OverviewPreview(MFAOverviewState.Loading)
}

@PreviewMultipleThemes
@Composable
private fun PreviewMFAOverviewError() = WireTheme {
    OverviewPreview(MFAOverviewState.Error)
}

@Composable
private fun OverviewPreview(state: MFAOverviewState) {
    MFAOverviewScreenContent(state, onBackPressed = {}, onRetry = {}, onAction = {})
}
