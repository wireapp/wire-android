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
internal fun MultiFactorAuthenticationAddAuthenticatorAppEntry(
    runtime: WireNavigation3Runtime,
    route: MultiFactorAuthenticationAddAuthenticatorAppRoute,
    onExit: () -> Unit,
) {
    val viewModel: MultiFactorAuthenticationAddAuthenticatorAppViewModel = settingsViewModel(
        viewModelStoreOwner = wireViewModelStoreOwner(WireViewModelOwner.Flow(route.flowId)),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentEntryId = runtime.navigator.currentRoute?.entryId
    fun next(step: MultiFactorAuthenticationSetupStep) {
        navigateAddAuthenticatorStep(runtime, route, step)
    }
    fun finish(result: AuthenticatorAppAddedResult) {
        if (runtime.navigator.currentRoute?.entryId != route.entryId) return
        runtime.navigator.replaceBackStack(addAuthenticatorRootBackStack(runtime.navigator.routes, route.flowId))
        if (!runtime.completeCurrentAndPop(AuthenticatorAppAddedResultType, WireNavResult.Value(result))) {
            if (!runtime.navigator.goBack()) onExit()
        }
    }
    LaunchedEffect(currentEntryId, state.enrollment, state.nextStep, state.completion) {
        if (currentEntryId != route.entryId) return@LaunchedEffect
        val completion = state.completion
        if (route.step != MultiFactorAuthenticationSetupStep.CONFIRM_PASSWORD && state.enrollment == null) {
            runtime.navigator.replaceBackStack(addAuthenticatorRootBackStack(runtime.navigator.routes, route.flowId))
        } else if (completion != null) {
            finish(completion)
        } else {
            state.nextStep?.let {
                viewModel.navigationHandled()
                next(it)
            }
        }
    }
    AddAuthenticatorAppScreen(
        step = route.step,
        state = state,
        viewModel = viewModel,
        onNext = ::next,
        onBack = {
            val completion = viewModel.state.value.completion
            if (completion != null) {
                finish(completion)
            } else {
                viewModel.cancelPendingOperation()
                if (!runtime.navigator.goBack()) onExit()
            }
        },
    )
}

private fun navigateAddAuthenticatorStep(
    runtime: WireNavigation3Runtime,
    route: MultiFactorAuthenticationAddAuthenticatorAppRoute,
    step: MultiFactorAuthenticationSetupStep,
) {
    if (runtime.navigator.currentRoute?.entryId != route.entryId) return
    if (route.step == MultiFactorAuthenticationSetupStep.MANUAL_KEY && step == MultiFactorAuthenticationSetupStep.VERIFY_CODE) {
        runtime.navigator.goBack()
    }
    runtime.navigator.navigate(WireNavigationCommand(route.copy(step = step, entryId = WireNavEntryId.random())))
}

@Composable
private fun AddAuthenticatorAppScreen(
    step: MultiFactorAuthenticationSetupStep,
    state: MultiFactorAuthenticationAddAuthenticatorAppState,
    viewModel: MultiFactorAuthenticationAddAuthenticatorAppViewModel,
    onNext: (MultiFactorAuthenticationSetupStep) -> Unit,
    onBack: () -> Unit,
) {
    val linkActions = rememberAuthenticatorLinkActions(state.enrollment) {
        onNext(MultiFactorAuthenticationSetupStep.VERIFY_CODE)
    }
    MultiFactorAuthenticationSetupContent(
        step = step,
        state = MultiFactorAuthenticationSetupState(enrollment = state.enrollment, isBusy = state.isBusy, error = state.error),
        password = viewModel.password,
        code = viewModel.code,
        name = viewModel.name,
        isAddingAuthenticator = true,
        onBack = onBack,
        onContinue = {
            when (step) {
                MultiFactorAuthenticationSetupStep.CONFIRM_PASSWORD -> viewModel.confirmPassword()
                MultiFactorAuthenticationSetupStep.LINK_AUTHENTICATOR,
                MultiFactorAuthenticationSetupStep.MANUAL_KEY -> onNext(MultiFactorAuthenticationSetupStep.VERIFY_CODE)
                MultiFactorAuthenticationSetupStep.VERIFY_CODE -> viewModel.verifyAuthenticationCode()
                MultiFactorAuthenticationSetupStep.NAME_APP -> viewModel.addApp()
                MultiFactorAuthenticationSetupStep.RECOVERY_CODES -> Unit
            }
        },
        onOpenAuthenticator = linkActions.open,
        onManualKey = { onNext(MultiFactorAuthenticationSetupStep.MANUAL_KEY) },
        onCopyKey = linkActions.copyKey,
        onCopyCodes = {},
        onDownloadCodes = {},
        onCodesSaved = {},
    )
}

internal fun addAuthenticatorRootBackStack(routes: List<WireRoute>, flowId: String): List<WireRoute> {
    val rootIndex = routes.indexOfFirst { it.flowId == flowId }
    require(rootIndex >= 0)
    val root = routes[rootIndex] as MultiFactorAuthenticationAddAuthenticatorAppRoute
    return routes.take(rootIndex) + root.copy(step = MultiFactorAuthenticationSetupStep.CONFIRM_PASSWORD)
}
