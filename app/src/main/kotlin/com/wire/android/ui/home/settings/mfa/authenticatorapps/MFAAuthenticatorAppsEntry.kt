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

package com.wire.android.ui.home.settings.mfa.authenticatorapps

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wire.android.R
import com.wire.android.navigation.navigation3.WireNavigation3Runtime
import com.wire.android.navigation.navigation3.wireViewModelStoreOwner
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.home.settings.settingsViewModel
import com.wire.android.ui.home.settings.mfa.MFAOverview
import com.wire.android.ui.home.settings.mfa.authenticatorapps.add.AuthenticatorAppAddedResultType
import com.wire.android.ui.home.settings.mfa.authenticatorapps.add.MFAAddAuthenticatorAppRoute
import com.wire.android.ui.home.settings.mfa.authenticatorapps.edit.AuthenticatorAppRenameResultType
import com.wire.android.ui.home.settings.mfa.authenticatorapps.edit.MFAEditAuthenticatorAppRoute
import com.wire.android.ui.home.settings.mfa.overview.MFAOverviewState
import com.wire.android.ui.home.settings.mfa.overview.MFAOverviewViewModel
import com.wire.navigation.WireNavResult
import com.wire.navigation.WireNavResultRequestId
import com.wire.navigation.WireViewModelOwner
import kotlinx.coroutines.launch

@Composable
internal fun MFAAuthenticatorAppsEntry(
    runtime: WireNavigation3Runtime,
    route: MFAAuthenticatorAppsRoute,
    onExit: () -> Unit,
) {
    val viewModel: MFAOverviewViewModel = settingsViewModel(
        viewModelStoreOwner = wireViewModelStoreOwner(WireViewModelOwner.Flow(route.flowId)),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    var renameRequestId by rememberSaveable(route.entryId.value) { mutableStateOf<String?>(null) }
    var addRequestId by rememberSaveable(route.entryId.value, "add-app") { mutableStateOf<String?>(null) }
    val currentEntryId = runtime.navigator.currentRoute?.entryId
    val snackbar = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    val renamedMessage = stringResource(R.string.mfa_app_renamed)
    LaunchedEffect(renameRequestId, currentEntryId) {
        if (currentEntryId != route.entryId) return@LaunchedEffect
        val requestId = renameRequestId?.let(::WireNavResultRequestId) ?: return@LaunchedEffect
        when (val result = runtime.consumeResult(requestId, AuthenticatorAppRenameResultType)) {
            is WireNavResult.Value -> {
                renameRequestId = null
                viewModel.onAuthenticatorAppRenamed(result.value)
                scope.launch { snackbar.showSnackbar(renamedMessage) }
            }
            WireNavResult.Canceled -> renameRequestId = null
            null -> Unit
        }
    }
    ConsumeAddedAuthenticatorResult(runtime, route, addRequestId, viewModel) { addRequestId = null }
    MFAAuthenticatorAppsScreen(
        state = state,
        onBack = { if (!runtime.navigator.goBack()) onExit() },
        onRetry = viewModel::refresh,
        onEdit = { appId ->
            renameRequestId = runtime.navigateForResult(
                MFAEditAuthenticatorAppRoute(route.sessionId, route.flowId, appId),
                AuthenticatorAppRenameResultType,
            )?.value
        },
        onAdd = {
            val loaded = viewModel.state.value as? MFAOverviewState.Loaded
            val overview = loaded?.overview as? MFAOverview.Enabled
            if (overview != null && overview.authenticatorApps.size < MAX_AUTHENTICATOR_APPS) {
                addRequestId = runtime.navigateForResult(
                    MFAAddAuthenticatorAppRoute(route.sessionId),
                    AuthenticatorAppAddedResultType,
                )?.value
            }
        },
    )
}

@Composable
private fun ConsumeAddedAuthenticatorResult(
    runtime: WireNavigation3Runtime,
    route: MFAAuthenticatorAppsRoute,
    requestIdValue: String?,
    viewModel: MFAOverviewViewModel,
    onConsumed: () -> Unit,
) {
    val currentEntryId = runtime.navigator.currentRoute?.entryId
    val snackbar = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    val addedMessage = stringResource(R.string.mfa_app_added)
    val limitMessage = stringResource(R.string.mfa_authenticator_apps_limit, MAX_AUTHENTICATOR_APPS)
    val addFailureMessage = stringResource(R.string.mfa_add_app_error)
    LaunchedEffect(requestIdValue, currentEntryId) {
        if (currentEntryId != route.entryId) return@LaunchedEffect
        val requestId = requestIdValue?.let(::WireNavResultRequestId) ?: return@LaunchedEffect
        when (val result = runtime.consumeResult(requestId, AuthenticatorAppAddedResultType)) {
            is WireNavResult.Value -> {
                val outcome = viewModel.onAuthenticatorAppAdded(result.value).await()
                onConsumed()
                val message = when (outcome) {
                    AuthenticatorAppMutationResult.SUCCESS -> addedMessage
                    AuthenticatorAppMutationResult.LIMIT_REACHED -> limitMessage
                    else -> addFailureMessage
                }
                scope.launch { snackbar.showSnackbar(message) }
            }
            WireNavResult.Canceled -> onConsumed()
            null -> Unit
        }
    }
}
