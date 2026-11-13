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
import com.wire.android.navigation.navigation3.WireNavigation3ResultType
import com.wire.android.navigation.navigation3.WireNavigation3Runtime
import com.wire.android.navigation.navigation3.wireViewModelStoreOwner
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.home.settings.settingsViewModel
import com.wire.navigation.WireNavResult
import com.wire.navigation.WireNavResultRequestId
import com.wire.navigation.WireViewModelOwner
import com.wire.navigation.WireNavigationCommand
import kotlinx.coroutines.launch

internal val AuthenticatorAppRenameResultType = WireNavigation3ResultType(
    contract = AuthenticatorAppRenameResultContract,
    serializer = AuthenticatorAppRenameResult.serializer(),
)

internal val AuthenticatorAppAddedResultType = WireNavigation3ResultType(
    contract = AuthenticatorAppAddedResultContract,
    serializer = AuthenticatorAppAddedResult.serializer(),
)

@Composable
internal fun MultiFactorAuthenticationAuthenticatorAppsEntry(
    runtime: WireNavigation3Runtime,
    route: MultiFactorAuthenticationAuthenticatorAppsRoute,
    onExit: () -> Unit,
) {
    val viewModel: MultiFactorAuthenticationViewModel = settingsViewModel(
        viewModelStoreOwner = wireViewModelStoreOwner(WireViewModelOwner.Flow(route.flowId)),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    var renameRequestId by rememberSaveable(route.entryId.value) { mutableStateOf<String?>(null) }
    var addRequestId by rememberSaveable(route.entryId.value, "add-app") { mutableStateOf<String?>(null) }
    val currentEntryId = runtime.navigator.currentRoute?.entryId
    val snackbar = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    val renamedMessage = stringResource(R.string.multi_factor_authentication_app_renamed)
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
    MultiFactorAuthenticationAuthenticatorAppsScreen(
        state = state,
        onBack = { if (!runtime.navigator.goBack()) onExit() },
        onRetry = viewModel::refresh,
        onEdit = { appId ->
            renameRequestId = runtime.navigateForResult(
                MultiFactorAuthenticationEditAuthenticatorAppRoute(route.sessionId, route.flowId, appId),
                AuthenticatorAppRenameResultType,
            )?.value
        },
        onAdd = {
            val loaded = viewModel.state.value as? MultiFactorAuthenticationOverviewState.Loaded
            val overview = loaded?.overview as? MultiFactorAuthenticationOverview.Enabled
            if (overview != null && overview.authenticatorApps.size < MAX_AUTHENTICATOR_APPS) {
                addRequestId = runtime.navigateForResult(
                    MultiFactorAuthenticationAddAuthenticatorAppRoute(route.sessionId),
                    AuthenticatorAppAddedResultType,
                )?.value
            }
        },
    )
}

@Composable
private fun ConsumeAddedAuthenticatorResult(
    runtime: WireNavigation3Runtime,
    route: MultiFactorAuthenticationAuthenticatorAppsRoute,
    requestIdValue: String?,
    viewModel: MultiFactorAuthenticationViewModel,
    onConsumed: () -> Unit,
) {
    val currentEntryId = runtime.navigator.currentRoute?.entryId
    val snackbar = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    val addedMessage = stringResource(R.string.multi_factor_authentication_app_added)
    val limitMessage = stringResource(R.string.multi_factor_authentication_authenticator_apps_limit, MAX_AUTHENTICATOR_APPS)
    val addFailureMessage = stringResource(R.string.multi_factor_authentication_add_app_error)
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

@Composable
internal fun MultiFactorAuthenticationEditAuthenticatorAppEntry(
    runtime: WireNavigation3Runtime,
    route: MultiFactorAuthenticationEditAuthenticatorAppRoute,
    onExit: () -> Unit,
) {
    val areaViewModel: MultiFactorAuthenticationViewModel = settingsViewModel(
        viewModelStoreOwner = wireViewModelStoreOwner(WireViewModelOwner.Flow(route.flowId)),
    )
    val editViewModel: MultiFactorAuthenticationEditAuthenticatorAppViewModel = settingsViewModel()
    val overviewState by areaViewModel.state.collectAsStateWithLifecycle()
    val state by editViewModel.state.collectAsStateWithLifecycle()
    val loaded = overviewState as? MultiFactorAuthenticationOverviewState.Loaded
    val overview = loaded?.overview as? MultiFactorAuthenticationOverview.Enabled
    val app = overview?.authenticatorApps?.find { it.id == route.appId }
    val currentEntryId = runtime.navigator.currentRoute?.entryId
    fun finish(result: AuthenticatorAppRenameResult) {
        if (runtime.navigator.currentRoute?.entryId != route.entryId) return
        if (!runtime.completeCurrentAndPop(AuthenticatorAppRenameResultType, WireNavResult.Value(result))) {
            if (!runtime.navigator.goBack()) onExit()
        }
    }
    LaunchedEffect(app?.id) { app?.let(editViewModel::initialize) }
    LaunchedEffect(state.result, currentEntryId) {
        if (currentEntryId == route.entryId) state.result?.let(::finish)
    }
    MultiFactorAuthenticationEditAuthenticatorAppScreen(
        appId = route.appId,
        overviewState = overviewState,
        state = state,
        name = editViewModel.name,
        onBack = {
            val result = editViewModel.state.value.result
            if (result != null) {
                finish(result)
            } else {
                editViewModel.cancel()
                if (!runtime.navigator.goBack()) onExit()
            }
        },
        onRetry = areaViewModel::refresh,
        onSave = editViewModel::save,
        onRemove = {
            if (overview != null && overview.authenticatorApps.size > 1) {
                runtime.navigator.navigate(
                    WireNavigationCommand(MultiFactorAuthenticationRemoveAuthenticatorAppRoute(route.sessionId, route.flowId, route.appId))
                )
            }
        },
    )
}
