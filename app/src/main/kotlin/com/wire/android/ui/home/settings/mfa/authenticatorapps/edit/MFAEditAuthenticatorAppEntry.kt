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

package com.wire.android.ui.home.settings.mfa.authenticatorapps.edit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wire.android.navigation.navigation3.WireNavigation3ResultType
import com.wire.android.navigation.navigation3.WireNavigation3Runtime
import com.wire.android.navigation.navigation3.wireViewModelStoreOwner
import com.wire.android.ui.home.settings.mfa.MFAOverview
import com.wire.android.ui.home.settings.mfa.authenticatorapps.remove.MFARemoveAuthenticatorAppRoute
import com.wire.android.ui.home.settings.mfa.overview.MFAOverviewState
import com.wire.android.ui.home.settings.mfa.overview.MFAOverviewViewModel
import com.wire.android.ui.home.settings.settingsViewModel
import com.wire.navigation.WireNavResult
import com.wire.navigation.WireNavigationCommand
import com.wire.navigation.WireViewModelOwner

internal val AuthenticatorAppRenameResultType = WireNavigation3ResultType(
    contract = AuthenticatorAppRenameResultContract,
    serializer = AuthenticatorAppRenameResult.serializer(),
)

@Composable
internal fun MFAEditAuthenticatorAppEntry(
    runtime: WireNavigation3Runtime,
    route: MFAEditAuthenticatorAppRoute,
    onExit: () -> Unit,
) {
    val areaViewModel: MFAOverviewViewModel = settingsViewModel(
        viewModelStoreOwner = wireViewModelStoreOwner(WireViewModelOwner.Flow(route.flowId)),
    )
    val editViewModel: MFAEditAuthenticatorAppViewModel = settingsViewModel()
    val overviewState by areaViewModel.state.collectAsStateWithLifecycle()
    val state by editViewModel.state.collectAsStateWithLifecycle()
    val loaded = overviewState as? MFAOverviewState.Loaded
    val overview = loaded?.overview as? MFAOverview.Enabled
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
    MFAEditAuthenticatorAppScreen(
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
                    WireNavigationCommand(MFARemoveAuthenticatorAppRoute(route.sessionId, route.flowId, route.appId))
                )
            }
        },
    )
}
