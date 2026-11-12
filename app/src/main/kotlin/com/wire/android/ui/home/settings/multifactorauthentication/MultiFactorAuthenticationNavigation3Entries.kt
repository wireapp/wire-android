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
import com.wire.android.navigation.navigation3.WireEntryProviderInstaller
import com.wire.android.navigation.navigation3.WireNavigation3ResultType
import com.wire.android.navigation.navigation3.WireNavigation3Runtime
import com.wire.android.navigation.navigation3.wireEntry
import com.wire.android.navigation.navigation3.wireViewModelStoreOwner
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.home.settings.MultiFactorAuthenticationRoute
import com.wire.android.ui.home.settings.settingsViewModel
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireNavResult
import com.wire.navigation.WireNavResultRequestId
import com.wire.navigation.WireNavigationCommand
import com.wire.navigation.WireRoute
import com.wire.navigation.WireViewModelOwner
import kotlinx.coroutines.launch

internal val MultiFactorAuthenticationSetupResultType = WireNavigation3ResultType(
    contract = MultiFactorAuthenticationSetupResultContract,
    serializer = MultiFactorAuthenticationSetupResult.serializer(),
)

internal val MultiFactorAuthenticationRecoveryCodesResultType = WireNavigation3ResultType(
    contract = MultiFactorAuthenticationRecoveryCodesResultContract,
    serializer = MultiFactorAuthenticationRecoveryCodesResult.serializer(),
)

internal fun multiFactorAuthenticationNavigation3Entries(
    runtime: WireNavigation3Runtime,
    onExit: () -> Unit,
): WireEntryProviderInstaller = {
    wireEntry<MultiFactorAuthenticationRoute> { route ->
        MultiFactorAuthenticationOverviewEntry(runtime, route, onExit)
    }
    wireEntry<MultiFactorAuthenticationSetupRoute> { route ->
        MultiFactorAuthenticationSetupEntry(runtime, route, onExit)
    }
    wireEntry<MultiFactorAuthenticationRecoveryCodesRoute> { route ->
        MultiFactorAuthenticationRecoveryCodesEntry(runtime, route, onExit)
    }
}

@Composable
private fun MultiFactorAuthenticationOverviewEntry(
    runtime: WireNavigation3Runtime,
    route: MultiFactorAuthenticationRoute,
    onExit: () -> Unit,
) {
    val viewModel: MultiFactorAuthenticationViewModel = settingsViewModel(
        viewModelStoreOwner = wireViewModelStoreOwner(WireViewModelOwner.Flow(route.flowId)),
    )
    var setupRequestId by rememberSaveable(route.entryId.value) { mutableStateOf<String?>(null) }
    var recoveryCodesRequestId by rememberSaveable(route.entryId.value, "recovery-codes") { mutableStateOf<String?>(null) }
    val currentEntryId = runtime.navigator.currentRoute?.entryId
    val snackbarHostState = LocalSnackbarHostState.current
    val successMessage = stringResource(R.string.multi_factor_authentication_setup_success)
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(setupRequestId, currentEntryId) {
        if (currentEntryId != route.entryId) return@LaunchedEffect
        val requestId = setupRequestId?.let(::WireNavResultRequestId) ?: return@LaunchedEffect
        when (val result = runtime.consumeResult(requestId, MultiFactorAuthenticationSetupResultType)) {
            is WireNavResult.Value -> {
                setupRequestId = null
                viewModel.onSetupCompleted(result.value)
                coroutineScope.launch { snackbarHostState.showSnackbar(successMessage) }
            }
            WireNavResult.Canceled -> setupRequestId = null
            null -> Unit
        }
    }
    LaunchedEffect(recoveryCodesRequestId, currentEntryId) {
        if (currentEntryId != route.entryId) return@LaunchedEffect
        val requestId = recoveryCodesRequestId?.let(::WireNavResultRequestId) ?: return@LaunchedEffect
        when (val result = runtime.consumeResult(requestId, MultiFactorAuthenticationRecoveryCodesResultType)) {
            is WireNavResult.Value -> {
                recoveryCodesRequestId = null
                viewModel.onRecoveryCodesGenerated(result.value)
            }
            WireNavResult.Canceled -> recoveryCodesRequestId = null
            null -> Unit
        }
    }
    MultiFactorAuthenticationScreen(
        onBackPressed = { if (!runtime.navigator.goBack()) onExit() },
        viewModel = viewModel,
        onSetUp = {
            setupRequestId = runtime.navigateForResult(
                MultiFactorAuthenticationSetupRoute(route.sessionId),
                MultiFactorAuthenticationSetupResultType,
            )?.value
        },
        onManageRecoveryCodes = { status ->
            recoveryCodesRequestId = runtime.navigateForResult(
                MultiFactorAuthenticationRecoveryCodesRoute(route.sessionId, status.remaining, status.total),
                MultiFactorAuthenticationRecoveryCodesResultType,
            )?.value
        },
    )
}

@Composable
private fun MultiFactorAuthenticationSetupEntry(
    runtime: WireNavigation3Runtime,
    route: MultiFactorAuthenticationSetupRoute,
    onExit: () -> Unit,
) {
    val viewModel: MultiFactorAuthenticationSetupViewModel = settingsViewModel(
        viewModelStoreOwner = wireViewModelStoreOwner(WireViewModelOwner.Flow(route.flowId)),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentEntryId = runtime.navigator.currentRoute?.entryId
    fun next(step: MultiFactorAuthenticationSetupStep) {
        if (runtime.navigator.currentRoute?.entryId != route.entryId) return
        // Verification always goes back to the linking screen, including after manual-key entry.
        if (route.step == MultiFactorAuthenticationSetupStep.MANUAL_KEY && step == MultiFactorAuthenticationSetupStep.VERIFY_CODE) {
            runtime.navigator.goBack()
        }
        runtime.navigator.navigate(WireNavigationCommand(route.copy(step = step, entryId = WireNavEntryId.random())))
    }
    LaunchedEffect(currentEntryId, state.nextStep, state.completion, state.enrollment) {
        if (currentEntryId != route.entryId) return@LaunchedEffect
        val completion = state.completion
        if (route.step != MultiFactorAuthenticationSetupStep.CONFIRM_PASSWORD && state.enrollment == null) {
            // Process recreation restores routes, never enrollment secrets. Restart confirmation.
            runtime.navigator.replaceBackStack(setupRootBackStack(runtime.navigator.routes, route.flowId))
        } else if (completion != null) {
            runtime.navigator.replaceBackStack(setupRootBackStack(runtime.navigator.routes, route.flowId))
            runtime.completeCurrentAndPop(MultiFactorAuthenticationSetupResultType, WireNavResult.Value(completion))
        } else {
            state.nextStep?.let { step ->
                viewModel.navigationHandled()
                next(step)
            }
        }
    }
    MultiFactorAuthenticationSetupScreen(
        step = route.step,
        state = state,
        viewModel = viewModel,
        onBack = {
            if (viewModel.state.value.canNavigateBackFrom(route.step)) {
                viewModel.cancelPendingOperation()
                if (!runtime.navigator.goBack()) onExit()
            }
        },
        onNext = ::next,
    )
}

/** Retain the requester and first setup entry so result completion can pop back to the same overview. */
internal fun setupRootBackStack(routes: List<WireRoute>, flowId: String): List<WireRoute> {
    val rootIndex = routes.indexOfFirst { it.flowId == flowId }
    require(rootIndex >= 0)
    val root = routes[rootIndex] as MultiFactorAuthenticationSetupRoute
    return routes.take(rootIndex) + root.copy(step = MultiFactorAuthenticationSetupStep.CONFIRM_PASSWORD)
}
