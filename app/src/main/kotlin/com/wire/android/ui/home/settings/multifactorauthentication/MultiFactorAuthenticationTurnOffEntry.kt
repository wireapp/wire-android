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
import com.wire.android.ui.home.settings.MultiFactorAuthenticationRoute
import com.wire.android.ui.home.settings.settingsViewModel
import com.wire.navigation.WireNavResult
import com.wire.navigation.WireNavResultRequestId

@Composable
internal fun MultiFactorAuthenticationTurnOffEntry(
    runtime: WireNavigation3Runtime,
    route: MultiFactorAuthenticationTurnOffRoute,
    onExit: () -> Unit,
) {
    val viewModel: MultiFactorAuthenticationTurnOffViewModel = settingsViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentEntryId = runtime.navigator.currentRoute?.entryId
    fun finish() {
        if (runtime.navigator.currentRoute?.entryId != route.entryId) return
        if (!runtime.completeCurrentAndPop(
                MultiFactorAuthenticationTurnOffResultType,
                WireNavResult.Value(MultiFactorAuthenticationTurnOffResult),
            )
        ) {
            if (!runtime.navigator.goBack()) onExit()
        }
    }
    LaunchedEffect(state.isCompleted, currentEntryId) {
        if (state.isCompleted && currentEntryId == route.entryId) finish()
    }
    MultiFactorAuthenticationTurnOffScreen(
        state = state,
        password = viewModel.password,
        code = viewModel.code,
        onBack = {
            if (runtime.navigator.currentRoute?.entryId == route.entryId) {
                if (viewModel.state.value.isCompleted) {
                    finish()
                } else {
                    viewModel.cancel()
                    if (!runtime.navigator.goBack()) onExit()
                }
            }
        },
        onTurnOff = viewModel::turnOff,
    )
}

@Composable
internal fun ConsumeMultiFactorAuthenticationTurnOffResult(
    runtime: WireNavigation3Runtime,
    route: MultiFactorAuthenticationRoute,
    requestIdValue: String?,
    onConsumed: () -> Unit,
    onSuccess: () -> Unit,
) {
    val currentEntryId = runtime.navigator.currentRoute?.entryId
    LaunchedEffect(requestIdValue, currentEntryId) {
        if (currentEntryId != route.entryId) return@LaunchedEffect
        val requestId = requestIdValue?.let(::WireNavResultRequestId) ?: return@LaunchedEffect
        when (runtime.consumeResult(requestId, MultiFactorAuthenticationTurnOffResultType)) {
            is WireNavResult.Value -> {
                onConsumed()
                onSuccess()
            }
            WireNavResult.Canceled -> onConsumed()
            null -> Unit
        }
    }
}
