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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wire.android.navigation.navigation3.WireNavigation3Runtime
import com.wire.android.navigation.navigation3.wireViewModelStoreOwner
import com.wire.android.ui.home.settings.settingsViewModel
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireNavResult
import com.wire.navigation.WireNavigationCommand
import com.wire.navigation.WireRoute
import com.wire.navigation.WireViewModelOwner

@Composable
internal fun MultiFactorAuthenticationRecoveryCodesEntry(
    runtime: WireNavigation3Runtime,
    route: MultiFactorAuthenticationRecoveryCodesRoute,
    onExit: () -> Unit,
) {
    val viewModel: MultiFactorAuthenticationRecoveryCodesViewModel = settingsViewModel(
        viewModelStoreOwner = wireViewModelStoreOwner(WireViewModelOwner.Flow(route.flowId)),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentEntryId = runtime.navigator.currentRoute?.entryId
    fun finish(count: Int) {
        runtime.navigator.replaceBackStack(recoveryCodesRootBackStack(runtime.navigator.routes, route.flowId))
        if (!runtime.completeCurrentAndPop(
                MultiFactorAuthenticationRecoveryCodesResultType,
                WireNavResult.Value(MultiFactorAuthenticationRecoveryCodesResult(count)),
            )
        ) {
            if (!runtime.navigator.goBack()) onExit()
        }
    }
    LaunchedEffect(currentEntryId, state.nextStep) {
        if (currentEntryId != route.entryId) return@LaunchedEffect
        if (route.step == MultiFactorAuthenticationRecoveryCodesStep.NEW_CODES && state.codes.isEmpty()) {
            // Process death loses the displayed codes. Preserve their generated count without regenerating them.
            finish(checkNotNull(route.generatedCount))
        } else if (route.step == MultiFactorAuthenticationRecoveryCodesStep.SUMMARY && !state.passwordConfirmed) {
            runtime.navigator.replaceBackStack(recoveryCodesRootBackStack(runtime.navigator.routes, route.flowId))
        } else {
            state.nextStep?.let { step ->
                viewModel.navigationHandled()
                runtime.navigator.navigate(
                    WireNavigationCommand(
                        route.copy(
                            step = step,
                            generatedCount = state.codes.size.takeIf { it > 0 },
                            entryId = WireNavEntryId.random(),
                        )
                    )
                )
            }
        }
    }
    MultiFactorAuthenticationRecoveryCodesScreen(
        route = route,
        state = state,
        viewModel = viewModel,
        onBack = {
            handleRecoveryCodesBack(viewModel, finish = ::finish) {
                if (!runtime.navigator.goBack()) onExit()
            }
        },
        onDone = {
            val current = viewModel.state.value
            if (current.codesSaved && current.codes.isNotEmpty()) finish(current.codes.size)
        },
    )
}

private fun handleRecoveryCodesBack(
    viewModel: MultiFactorAuthenticationRecoveryCodesViewModel,
    finish: (Int) -> Unit,
    goBack: () -> Unit,
) {
    val current = viewModel.state.value
    if (!current.canNavigateBack) return
    if (current.codes.isNotEmpty()) {
        finish(current.codes.size)
    } else {
        viewModel.cancelPendingOperation()
        goBack()
    }
}

internal fun recoveryCodesRootBackStack(routes: List<WireRoute>, flowId: String): List<WireRoute> {
    val rootIndex = routes.indexOfFirst { it.flowId == flowId }
    require(rootIndex >= 0)
    return routes.take(rootIndex + 1)
}
