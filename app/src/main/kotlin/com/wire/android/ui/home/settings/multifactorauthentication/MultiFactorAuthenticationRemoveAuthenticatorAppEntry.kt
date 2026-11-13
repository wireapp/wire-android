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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wire.android.R
import com.wire.android.navigation.navigation3.WireNavigation3Runtime
import com.wire.android.navigation.navigation3.wireViewModelStoreOwner
import com.wire.android.ui.home.settings.settingsViewModel
import com.wire.navigation.WireRoute
import com.wire.navigation.WireViewModelOwner

@Composable
internal fun MultiFactorAuthenticationRemoveAuthenticatorAppEntry(
    runtime: WireNavigation3Runtime,
    route: MultiFactorAuthenticationRemoveAuthenticatorAppRoute,
    onExit: () -> Unit,
) {
    val areaViewModel: MultiFactorAuthenticationViewModel = settingsViewModel(
        viewModelStoreOwner = wireViewModelStoreOwner(WireViewModelOwner.Flow(route.flowId)),
    )
    val viewModel: MultiFactorAuthenticationRemoveAuthenticatorAppViewModel = settingsViewModel()
    val overviewState by areaViewModel.state.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val loaded = overviewState as? MultiFactorAuthenticationOverviewState.Loaded
    val apps = (loaded?.overview as? MultiFactorAuthenticationOverview.Enabled)?.authenticatorApps.orEmpty()
    val app = apps.find { it.id == route.appId }
    val canRemove = app != null && apps.size > 1
    val currentEntryId = runtime.navigator.currentRoute?.entryId
    LaunchedEffect(state.isValidated, currentEntryId) {
        if (!state.isValidated || currentEntryId != route.entryId) return@LaunchedEffect
        val outcome = areaViewModel.removeAuthenticatorApp(route.appId).await()
        if (outcome == AuthenticatorAppMutationResult.SUCCESS) {
            val popCount = authenticatorAppsReturnPopCount(runtime.navigator.routes, route.flowId)
            repeat(popCount) { runtime.navigator.goBack() }
        } else {
            viewModel.removalRejected(outcome)
        }
    }
    val error = removalErrorText(state.error, app == null, apps.size <= 1)
    MultiFactorAuthenticationCredentialsScreen(
        title = stringResource(R.string.multi_factor_authentication_remove_app_title),
        heading = app?.let { stringResource(R.string.multi_factor_authentication_remove_app_heading, it.name) }
            ?: stringResource(R.string.multi_factor_authentication_remove_app),
        description = stringResource(R.string.multi_factor_authentication_remove_app_description),
        confirmLabel = stringResource(R.string.label_remove),
        password = viewModel.password,
        code = viewModel.code,
        isBusy = state.isBusy || state.isValidated,
        actionAvailable = canRemove,
        errorText = error,
        isCodeInvalid = state.error == RemoveAuthenticatorAppError.INVALID_CODE,
        onBack = {
            if (!viewModel.state.value.isValidated) {
                viewModel.cancel()
                if (!runtime.navigator.goBack()) onExit()
            }
        },
        onConfirm = { if (canRemove) viewModel.confirmRemoval(route.appId) },
    )
}

@Composable
private fun removalErrorText(error: RemoveAuthenticatorAppError?, unavailable: Boolean, lastApp: Boolean): String? {
    val resource = when {
        unavailable -> R.string.multi_factor_authentication_authenticator_app_unavailable
        lastApp || error == RemoveAuthenticatorAppError.LAST_APP -> R.string.multi_factor_authentication_only_app_edit_description
        error == RemoveAuthenticatorAppError.INVALID_CODE -> R.string.multi_factor_authentication_invalid_code
        error == RemoveAuthenticatorAppError.GENERIC -> R.string.multi_factor_authentication_setup_error
        error == RemoveAuthenticatorAppError.UNAVAILABLE -> R.string.multi_factor_authentication_authenticator_app_unavailable
        else -> null
    }
    return resource?.let { stringResource(it) }
}

/** Ordinary pops also cancel the editor's pending rename result rather than leaving a stale request. */
internal fun authenticatorAppsReturnPopCount(routes: List<WireRoute>, flowId: String): Int {
    val index = routes.indexOfLast { it is MultiFactorAuthenticationAuthenticatorAppsRoute && it.flowId == flowId }
    return if (index >= 0) routes.lastIndex - index else 1
}
