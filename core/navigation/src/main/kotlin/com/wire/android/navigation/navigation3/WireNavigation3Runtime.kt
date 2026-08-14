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

package com.wire.android.navigation.navigation3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import com.wire.navigation.WireBackStackChange
import com.wire.navigation.WireNavResult
import com.wire.navigation.WireNavResultRequest
import com.wire.navigation.WireNavResultRequestId
import com.wire.navigation.WireNavigationCommand
import com.wire.navigation.WireNavigationController
import com.wire.navigation.WireRoute
import java.util.UUID

/**
 * Saveable Navigation 3 state together with Wire's only back-stack mutation API.
 */
@Stable
class WireNavigation3Runtime internal constructor(
    val backStack: NavBackStack<NavKey>,
    private val resultStore: WireNavigation3ResultStore,
    canNavigate: (WireNavigationCommand) -> Boolean,
    private val requestIdFactory: () -> WireNavResultRequestId,
    private val onNavigationChanged: (
        previous: List<WireRoute>,
        current: List<WireRoute>,
        change: WireBackStackChange,
    ) -> Unit = { _, _, _ -> },
) {
    val navigator = WireNavigationController(
        backStack = backStack,
        canNavigate = canNavigate,
        onBackStackChanged = ::onBackStackChanged,
    )

    /**
     * Pushes a fresh entry and binds its result to the concrete requester entry.
     */
    fun <T> navigateForResult(
        destination: WireRoute,
        resultType: WireNavigation3ResultType<T>,
    ): WireNavResultRequestId? {
        val requesterEntryId = navigator.currentRoute?.entryId ?: return null
        require(destination.entryId !in activeEntryIds()) {
            "A route passed to navigateForResult must have a fresh entry id"
        }
        val requestId = requestIdFactory()
        val request = WireNavResultRequest(
            id = requestId,
            contractId = resultType.contract.id,
            requesterEntryId = requesterEntryId,
            targetEntryId = destination.entryId,
        )
        check(resultStore.register(resultType, request)) {
            "The navigation result request id factory produced a duplicate id"
        }
        val accepted = runCatching {
            navigator.navigate(
                WireNavigationCommand(destination = destination, launchSingleTop = false)
            )
        }.getOrElse { failure ->
            check(resultStore.discardPending(resultType, requestId))
            throw failure
        }
        return if (accepted) {
            requestId
        } else {
            check(resultStore.discardPending(resultType, requestId))
            null
        }
    }

    /**
     * Completes a pending request and removes its current target as one serialized operation.
     */
    fun <T> completeAndPop(
        requestId: WireNavResultRequestId,
        resultType: WireNavigation3ResultType<T>,
        result: WireNavResult<T>,
    ): Boolean {
        val completion = resultStore.completeAndPop(
            type = resultType,
            requestId = requestId,
            result = result,
            removeTarget = { targetEntryId ->
                val current = navigator.currentRoute
                if (current?.entryId != targetEntryId || backStack.size <= 1) {
                    false
                } else {
                    backStack.removeLast()
                    true
                }
            },
        )
        if (completion.completed) {
            val remaining = activeEntryIds()
            // The completing channel already holds its terminal value; reconciliation preserves it
            // while canceling any other pending request targeting the same removed entry.
            completion.targetEntryId?.let { resultStore.onEntryRemoved(it, remaining) }
        }
        return completion.completed
    }

    /**
     * Completes the unique pending result request targeting the current entry and removes it.
     *
     * Request ids belong to the navigation relationship and therefore are not embedded in route
     * arguments. Missing or ambiguous relationships are rejected without changing the stack.
     */
    fun <T> completeCurrentAndPop(
        resultType: WireNavigation3ResultType<T>,
        result: WireNavResult<T>,
    ): Boolean {
        val requestId = navigator.currentRoute?.entryId?.let { targetEntryId ->
            resultStore.uniquePendingRequestForTarget(
                type = resultType,
                targetEntryId = targetEntryId,
            )
        }
        return requestId?.let { completeAndPop(it, resultType, result) } ?: false
    }

    /**
     * Consumes a result only when called by the exact entry that initiated the request.
     */
    fun <T> consumeResult(
        requestId: WireNavResultRequestId,
        resultType: WireNavigation3ResultType<T>,
    ): WireNavResult<T>? {
        val requesterEntryId = navigator.currentRoute?.entryId ?: return null
        return resultStore.consume(resultType, requestId, requesterEntryId)
    }

    private fun onBackStackChanged(
        previous: List<WireRoute>,
        current: List<WireRoute>,
        change: WireBackStackChange,
    ) {
        onNavigationChanged(previous, current, change)
        val active = current.mapTo(mutableSetOf()) { it.entryId }
        if (change == WireBackStackChange.BACK) {
            previous.asSequence()
                .map { it.entryId }
                .filterNot(active::contains)
                .forEach { resultStore.onEntryRemoved(it, active) }
        } else {
            resultStore.prune(active)
        }
    }

    private fun activeEntryIds() = navigator.routes.mapTo(mutableSetOf()) { it.entryId }
}

/**
 * Creates navigation state that survives configuration changes and process recreation.
 *
 * Every concrete [WireRoute] placed on this stack must be annotated with
 * `kotlinx.serialization.Serializable`.
 */
@Composable
fun rememberWireNavigation3Runtime(
    startRoute: WireRoute,
    resultTypes: List<WireNavigation3ResultType<*>> = emptyList(),
    canNavigate: (WireNavigationCommand) -> Boolean = { true },
    onNavigationChanged: (
        previous: List<WireRoute>,
        current: List<WireRoute>,
        change: WireBackStackChange,
    ) -> Unit = { _, _, _ -> },
): WireNavigation3Runtime {
    val backStack = rememberNavBackStack(startRoute)
    val currentCanNavigate by rememberUpdatedState(canNavigate)
    val resultTypeIds = resultTypes.map { it.contract.id.value }
    val resultStore = rememberSaveable(
        resultTypeIds,
        saver = listSaver(
            save = { store ->
                store.encode().flatMap { (contractId, snapshot) -> listOf(contractId, snapshot) }
            },
            restore = { values ->
                require(values.size % 2 == 0) {
                    "Saved navigation result state contains an incomplete channel"
                }
                val restored = values.chunked(2).associate { (contractId, snapshot) ->
                    contractId to snapshot
                }
                WireNavigation3ResultStore(resultTypes, restored)
            },
        ),
    ) {
        WireNavigation3ResultStore(resultTypes)
    }
    return remember(backStack, resultStore) {
        WireNavigation3Runtime(
            backStack = backStack,
            resultStore = resultStore,
            canNavigate = { command -> currentCanNavigate(command) },
            requestIdFactory = { WireNavResultRequestId(UUID.randomUUID().toString()) },
            onNavigationChanged = onNavigationChanged,
        )
    }
}
