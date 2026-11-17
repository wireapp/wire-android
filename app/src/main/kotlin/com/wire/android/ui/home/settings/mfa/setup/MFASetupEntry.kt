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

package com.wire.android.ui.home.settings.mfa.setup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wire.android.navigation.navigation3.WireNavigation3ResultType
import com.wire.android.navigation.navigation3.WireNavigation3Runtime
import com.wire.android.navigation.navigation3.wireViewModelStoreOwner
import com.wire.android.ui.home.settings.settingsViewModel
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireNavResult
import com.wire.navigation.WireNavigationCommand
import com.wire.navigation.WireRoute
import com.wire.navigation.WireViewModelOwner

internal val MFASetupResultType = WireNavigation3ResultType(
    contract = MFASetupResultContract,
    serializer = MFASetupResult.serializer(),
)

@Composable
internal fun MFASetupEntry(
    runtime: WireNavigation3Runtime,
    route: MFASetupRoute,
    onExit: () -> Unit,
) {
    val viewModel: MFASetupViewModel = settingsViewModel(
        viewModelStoreOwner = wireViewModelStoreOwner(WireViewModelOwner.Flow(route.flowId)),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentEntryId = runtime.navigator.currentRoute?.entryId
    fun next(step: MFASetupStep) {
        if (runtime.navigator.currentRoute?.entryId != route.entryId) return
        // Verification always goes back to the linking screen, including after manual-key entry.
        if (route.step == MFASetupStep.MANUAL_KEY && step == MFASetupStep.VERIFY_CODE) {
            runtime.navigator.goBack()
        }
        runtime.navigator.navigate(WireNavigationCommand(route.copy(step = step, entryId = WireNavEntryId.random())))
    }
    LaunchedEffect(currentEntryId, state.nextStep, state.completion, state.enrollment) {
        if (currentEntryId != route.entryId) return@LaunchedEffect
        val completion = state.completion
        if (route.step != MFASetupStep.CONFIRM_PASSWORD && state.enrollment == null) {
            // Process recreation restores routes, never enrollment secrets. Restart confirmation.
            runtime.navigator.replaceBackStack(setupRootBackStack(runtime.navigator.routes, route.flowId))
        } else if (completion != null) {
            runtime.navigator.replaceBackStack(setupRootBackStack(runtime.navigator.routes, route.flowId))
            runtime.completeCurrentAndPop(MFASetupResultType, WireNavResult.Value(completion))
        } else {
            state.nextStep?.let { step ->
                viewModel.navigationHandled()
                next(step)
            }
        }
    }
    MFASetupScreen(
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
    val root = routes[rootIndex] as MFASetupRoute
    return routes.take(rootIndex) + root.copy(step = MFASetupStep.CONFIRM_PASSWORD)
}
