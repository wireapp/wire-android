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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
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
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate

@Composable
internal fun MultiFactorAuthenticationAuthenticatorAppsScreen(
    state: MultiFactorAuthenticationOverviewState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onEdit: (String) -> Unit,
    onAdd: () -> Unit,
) {
    val loaded = state as? MultiFactorAuthenticationOverviewState.Loaded
    val overview = loaded?.overview as? MultiFactorAuthenticationOverview.Enabled
    WireScaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                title = stringResource(R.string.multi_factor_authentication_authenticator_apps),
                onNavigationPressed = onBack,
                elevation = dimensions().spacing0x,
            )
        },
        bottomBar = {
            if (overview != null) {
                WirePrimaryButton(
                    text = stringResource(R.string.multi_factor_authentication_add_authenticator_app),
                    onClick = onAdd,
                    state = if (overview.authenticatorApps.size < MAX_AUTHENTICATOR_APPS) {
                        WireButtonState.Default
                    } else {
                        WireButtonState.Disabled
                    },
                    modifier = Modifier.padding(dimensions().spacing16x),
                )
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (state) {
                MultiFactorAuthenticationOverviewState.Loading -> CenteredCircularProgressBarIndicator()
                MultiFactorAuthenticationOverviewState.Error -> AuthenticatorAppsLoadError(onRetry, Modifier.align(Alignment.Center))
                is MultiFactorAuthenticationOverviewState.Loaded -> AuthenticatorAppsList(
                    apps = overview?.authenticatorApps.orEmpty(),
                    state = state,
                    onRetry = onRetry,
                    onEdit = onEdit,
                )
            }
        }
    }
}

@Composable
private fun AuthenticatorAppsList(
    apps: List<AuthenticatorApp>,
    state: MultiFactorAuthenticationOverviewState.Loaded,
    onRetry: () -> Unit,
    onEdit: (String) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize()) {
        if (state.isRefreshing) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        }
        item {
            AuthenticatorAppsDescription(stringResource(R.string.multi_factor_authentication_authenticator_apps_description))
        }
        items(apps, key = { it.id }) { app ->
            val locale = LocalLocale.current.platformLocale
            val addedDate = remember(app.addedOn, locale) {
                app.addedOn.toJavaLocalDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
            }
            GroupConversationOptionsItem(
                title = app.name,
                subtitle = stringResource(R.string.multi_factor_authentication_authenticator_added_on, addedDate),
                arrowLabel = stringResource(R.string.multi_factor_authentication_edit_app_action),
                arrowType = ArrowType.NONE,
                clickable = Clickable { onEdit(app.id) },
            )
        }
        if (apps.isEmpty()) {
            item { AuthenticatorAppsDescription(stringResource(R.string.multi_factor_authentication_no_authenticator_apps)) }
        }
        if (apps.size == 1) {
            item { AuthenticatorAppsDescription(stringResource(R.string.multi_factor_authentication_only_app_list_description)) }
        }
        if (apps.size >= MAX_AUTHENTICATOR_APPS) {
            item {
                AuthenticatorAppsDescription(
                    stringResource(R.string.multi_factor_authentication_authenticator_apps_limit, MAX_AUTHENTICATOR_APPS)
                )
            }
        }
        if (state.hasRefreshError) {
            item { AuthenticatorAppsLoadError(onRetry) }
        }
    }
}

@Composable
internal fun AuthenticatorAppsDescription(text: String) {
    Text(
        text = text,
        style = MaterialTheme.wireTypography.body01,
        color = MaterialTheme.wireColorScheme.secondaryText,
        modifier = Modifier.padding(dimensions().spacing16x),
    )
}

@Composable
internal fun AuthenticatorAppsLoadError(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(dimensions().spacing16x),
        verticalArrangement = Arrangement.spacedBy(dimensions().spacing16x),
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
private fun PreviewAuthenticatorAppsSingle() = AuthenticatorAppsPreview(1)

@PreviewMultipleThemes
@Composable
private fun PreviewAuthenticatorAppsMultiple() = AuthenticatorAppsPreview(2)

@PreviewMultipleThemes
@Composable
private fun PreviewAuthenticatorAppsAtLimit() = AuthenticatorAppsPreview(MAX_AUTHENTICATOR_APPS)

@Composable
private fun AuthenticatorAppsPreview(count: Int) = WireTheme {
    MultiFactorAuthenticationAuthenticatorAppsScreen(
        state = MultiFactorAuthenticationOverviewState.Loaded(
            MultiFactorAuthenticationOverview.Enabled(
                authenticatorApps = List(count) { AuthenticatorApp("app-$it", "Android phone ${it + 1}", LocalDate(2026, 10, 14)) },
                recoveryCodesStatus = RecoveryCodesStatus(10, 10),
            )
        ),
        onBack = {},
        onRetry = {},
        onEdit = {},
        onAdd = {},
    )
}
