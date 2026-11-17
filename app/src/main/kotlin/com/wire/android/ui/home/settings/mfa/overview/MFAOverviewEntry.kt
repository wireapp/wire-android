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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.navigation.navigation3.WireNavigation3Runtime
import com.wire.android.navigation.navigation3.wireViewModelStoreOwner
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.home.settings.settingsViewModel
import com.wire.android.ui.home.settings.mfa.authenticatorapps.MFAAuthenticatorAppsRoute
import com.wire.android.ui.home.settings.mfa.recoverycodes.MFARecoveryCodesResultType
import com.wire.android.ui.home.settings.mfa.recoverycodes.MFARecoveryCodesRoute
import com.wire.android.ui.home.settings.mfa.setup.MFASetupResultType
import com.wire.android.ui.home.settings.mfa.setup.MFASetupRoute
import com.wire.android.ui.home.settings.mfa.turnoff.ConsumeMFATurnOffResult
import com.wire.android.ui.home.settings.mfa.turnoff.MFATurnOffResultType
import com.wire.android.ui.home.settings.mfa.turnoff.MFATurnOffRoute
import com.wire.navigation.WireNavResult
import com.wire.navigation.WireNavResultRequestId
import com.wire.navigation.WireNavigationCommand
import com.wire.navigation.WireViewModelOwner
import kotlinx.coroutines.launch

@Composable
internal fun MFAOverviewEntry(
    runtime: WireNavigation3Runtime,
    route: MFAOverviewRoute,
    onExit: () -> Unit,
) {
    val viewModel: MFAOverviewViewModel = settingsViewModel(
        viewModelStoreOwner = wireViewModelStoreOwner(WireViewModelOwner.Flow(route.flowId)),
    )
    var setupRequestId by rememberSaveable(route.entryId.value) { mutableStateOf<String?>(null) }
    var recoveryCodesRequestId by rememberSaveable(route.entryId.value, "recovery-codes") { mutableStateOf<String?>(null) }
    var turnOffRequestId by rememberSaveable(route.entryId.value, "turn-off") { mutableStateOf<String?>(null) }
    val currentEntryId = runtime.navigator.currentRoute?.entryId
    val snackbarHostState = LocalSnackbarHostState.current
    val successMessage = stringResource(R.string.mfa_setup_success)
    val turnOffSuccessMessage = stringResource(R.string.mfa_turn_off_success)
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(setupRequestId, currentEntryId) {
        if (currentEntryId != route.entryId) return@LaunchedEffect
        val requestId = setupRequestId?.let(::WireNavResultRequestId) ?: return@LaunchedEffect
        when (runtime.consumeResult(requestId, MFASetupResultType)) {
            is WireNavResult.Value -> {
                setupRequestId = null
                coroutineScope.launch { snackbarHostState.showSnackbar(successMessage) }
            }
            WireNavResult.Canceled -> setupRequestId = null
            null -> Unit
        }
    }
    LaunchedEffect(recoveryCodesRequestId, currentEntryId) {
        if (currentEntryId != route.entryId) return@LaunchedEffect
        val requestId = recoveryCodesRequestId?.let(::WireNavResultRequestId) ?: return@LaunchedEffect
        when (runtime.consumeResult(requestId, MFARecoveryCodesResultType)) {
            is WireNavResult.Value -> {
                recoveryCodesRequestId = null
            }
            WireNavResult.Canceled -> recoveryCodesRequestId = null
            null -> Unit
        }
    }
    ConsumeMFATurnOffResult(
        runtime = runtime,
        route = route,
        requestIdValue = turnOffRequestId,
        onConsumed = { turnOffRequestId = null },
        onSuccess = {
            coroutineScope.launch { snackbarHostState.showSnackbar(turnOffSuccessMessage) }
        },
    )
    MFAOverviewScreen(
        onBackPressed = { if (!runtime.navigator.goBack()) onExit() },
        viewModel = viewModel,
        onSetUp = {
            setupRequestId = runtime.navigateForResult(
                MFASetupRoute(route.sessionId),
                MFASetupResultType,
            )?.value
        },
        onManageRecoveryCodes = { status ->
            recoveryCodesRequestId = runtime.navigateForResult(
                MFARecoveryCodesRoute(route.sessionId, status.remaining, status.total),
                MFARecoveryCodesResultType,
            )?.value
        },
        onTurnOff = {
            turnOffRequestId = runtime.navigateForResult(
                MFATurnOffRoute(route.sessionId),
                MFATurnOffResultType,
            )?.value
        },
        onManageAuthenticatorApps = {
            runtime.navigator.navigate(
                WireNavigationCommand(MFAAuthenticatorAppsRoute(route.sessionId, route.flowId))
            )
        },
    )
}
