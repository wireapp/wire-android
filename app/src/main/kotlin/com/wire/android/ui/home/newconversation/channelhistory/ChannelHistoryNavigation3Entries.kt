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

package com.wire.android.ui.home.newconversation.channelhistory

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.wire.android.navigation.navigation3.WireEntryProviderInstaller
import com.wire.android.navigation.navigation3.WireNavigation3ResultType
import com.wire.android.navigation.navigation3.WireNavigation3Runtime
import com.wire.android.navigation.navigation3.wireEntry
import com.wire.android.navigation.navigation3.wireViewModelStoreOwner
import com.wire.android.ui.home.newConversationViewModel
import com.wire.navigation.WireNavResult
import com.wire.navigation.WireNavResultRequestId
import com.wire.navigation.WireViewModelOwner

/**
 * The Android persistence boundary for the KMP result contract.
 *
 * Keep this as one stable instance: the runtime deliberately rejects an unregistered look-alike
 * contract so a serializer cannot be changed accidentally during state restoration.
 */
internal val ChannelHistoryCustomResultType = WireNavigation3ResultType(
    contract = ChannelHistoryCustomResultContract,
    serializer = ChannelHistoryCustomResult.serializer(),
)

/**
 * App-owned pilot contribution consumed by the future root Navigation 3 host.
 *
 * Keeping result types and entry providers together prevents the host from installing entries
 * whose result channel was not registered in [WireNavigation3Runtime].
 */
internal object ChannelHistoryNavigation3Pilot {
    val resultTypes: List<WireNavigation3ResultType<*>> =
        listOf(ChannelHistoryCustomResultType)

    fun entryProviderInstallers(
        runtime: WireNavigation3Runtime,
    ): List<WireEntryProviderInstaller> =
        listOf(channelHistoryNavigation3Entries(runtime))
}

/**
 * Navigation 3 entries for the channel-history slice of new-conversation.
 */
internal fun channelHistoryNavigation3Entries(
    runtime: WireNavigation3Runtime,
): WireEntryProviderInstaller = {
    wireEntry<ChannelHistoryRoute> { route ->
        ChannelHistoryNavigation3Entry(route, runtime)
    }
    wireEntry<ChannelHistoryCustomRoute> { route ->
        ChannelHistoryCustomNavigation3Entry(route, runtime)
    }
}

@Composable
private fun ChannelHistoryNavigation3Entry(
    route: ChannelHistoryRoute,
    runtime: WireNavigation3Runtime,
) {
    val flowOwner = wireViewModelStoreOwner(WireViewModelOwner.Flow(route.flowId))
    val viewModel = newConversationViewModel(viewModelStoreOwner = flowOwner)
    var pendingRequestIdValue by rememberSaveable(route.entryId.value) {
        mutableStateOf<String?>(null)
    }

    val currentEntryId = runtime.navigator.currentRoute?.entryId
    LaunchedEffect(pendingRequestIdValue, currentEntryId) {
        if (currentEntryId != route.entryId) return@LaunchedEffect
        val requestId = pendingRequestIdValue?.let(::WireNavResultRequestId)
            ?: return@LaunchedEffect
        when (val result = runtime.consumeResult(requestId, ChannelHistoryCustomResultType)) {
            is WireNavResult.Value -> {
                viewModel.setChannelHistoryType(result.value.customType.toLegacy())
                pendingRequestIdValue = null
            }

            WireNavResult.Canceled -> pendingRequestIdValue = null
            null -> Unit
        }
    }

    ChannelHistoryScreenContent(
        selectedHistoryOption = viewModel.newGroupState.channelHistoryType,
        onHistoryOptionSelected = viewModel::setChannelHistoryType,
        onOpenCustomChooser = {
            val requestId = runtime.navigateForResult(
                destination = ChannelHistoryCustomRoute(
                    sessionId = route.sessionId,
                    flowId = route.flowId,
                    currentType = viewModel.newGroupState.channelHistoryType.toSelection(),
                ),
                resultType = ChannelHistoryCustomResultType,
            )
            pendingRequestIdValue = requestId?.value
        },
        onBackPressed = runtime.navigator::goBack,
    )
}

@Composable
private fun ChannelHistoryCustomNavigation3Entry(
    route: ChannelHistoryCustomRoute,
    runtime: WireNavigation3Runtime,
) {
    ChannelHistoryCustomRouteScreen(
        currentType = route.currentType.toLegacy(),
        onNavigateBack = { selected ->
            val result = selected
                ?.toSelection()
                ?.let(::ChannelHistoryCustomResult)
                ?.let { WireNavResult.Value(it) }
                ?: WireNavResult.Canceled
            if (!runtime.completeCurrentAndPop(ChannelHistoryCustomResultType, result)) {
                runtime.navigator.goBack()
            }
        },
    )
}
