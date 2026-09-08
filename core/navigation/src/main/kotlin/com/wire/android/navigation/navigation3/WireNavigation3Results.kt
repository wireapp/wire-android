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

import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireNavResult
import com.wire.navigation.WireNavResultCoordinator
import com.wire.navigation.WireNavResultContract
import com.wire.navigation.WireNavResultRegistry
import com.wire.navigation.WireNavResultRegistrySnapshot
import com.wire.navigation.WireNavResultRequest
import com.wire.navigation.WireNavResultRequestId
import com.wire.navigation.decodeWireNavResultRegistrySnapshot
import com.wire.navigation.encode
import kotlinx.serialization.KSerializer

/**
 * Android persistence information for one KMP result contract.
 *
 * The serializer stays at the runtime boundary; only the contract id and encoded registry
 * snapshot enter saved instance state.
 */
class WireNavigation3ResultType<T>(
    val contract: WireNavResultContract<T>,
    internal val serializer: KSerializer<T>,
)

internal class WireNavigation3ResultStore(
    resultTypes: List<WireNavigation3ResultType<*>>,
    restored: Map<String, String> = emptyMap(),
) {
    private val channels = resultTypes.associate { type ->
        type.contract.id.value to type.toChannel(restored[type.contract.id.value])
    }

    init {
        require(channels.size == resultTypes.size) {
            "Navigation result contract ids must be unique"
        }
    }

    fun <T> register(
        type: WireNavigation3ResultType<T>,
        request: WireNavResultRequest,
    ): Boolean = channel(type).registry.register(request)

    fun <T> discardPending(
        type: WireNavigation3ResultType<T>,
        requestId: WireNavResultRequestId,
    ): Boolean = channel(type).registry.discardPending(requestId)

    fun <T> uniquePendingRequestForTarget(
        type: WireNavigation3ResultType<T>,
        targetEntryId: WireNavEntryId,
    ): WireNavResultRequestId? =
        channel(type).registry.uniquePendingRequestForTarget(targetEntryId)

    fun <T> consume(
        type: WireNavigation3ResultType<T>,
        requestId: WireNavResultRequestId,
        requesterEntryId: WireNavEntryId,
    ): WireNavResult<T>? = channel(type).registry.consume(requestId, requesterEntryId)

    fun <T> completeAndPop(
        type: WireNavigation3ResultType<T>,
        requestId: WireNavResultRequestId,
        result: WireNavResult<T>,
        removeTarget: (WireNavEntryId) -> Boolean,
    ): WireNavigation3Completion {
        val registry = channel(type).registry
        val targetEntryId = registry.snapshot().records
            .firstOrNull { it.request.id == requestId }
            ?.request
            ?.targetEntryId
        val completed = WireNavResultCoordinator(registry, removeTarget)
            .completeAndPop(requestId, result)
        return WireNavigation3Completion(completed, targetEntryId)
    }

    fun onEntryRemoved(entryId: WireNavEntryId, remainingEntryIds: Set<WireNavEntryId>) {
        channels.values.forEach { it.onEntryRemoved(entryId, remainingEntryIds) }
    }

    fun prune(activeEntryIds: Set<WireNavEntryId>) {
        channels.values.forEach { it.prune(activeEntryIds) }
    }

    fun encode(): Map<String, String> = channels.mapValues { (_, channel) -> channel.encode() }

    @Suppress("UNCHECKED_CAST")
    private fun <T> channel(type: WireNavigation3ResultType<T>): TypedChannel<T> {
        val channel = channels[type.contract.id.value]
            ?: error("Navigation result contract '${type.contract.id.value}' is not registered")
        require(channel.type === type) {
            "Use the result type instance registered in rememberWireNavigation3Runtime"
        }
        return channel as TypedChannel<T>
    }

    private interface Channel {
        val type: WireNavigation3ResultType<*>
        val contract: WireNavResultContract<*>
        fun onEntryRemoved(entryId: WireNavEntryId, remainingEntryIds: Set<WireNavEntryId>)
        fun prune(activeEntryIds: Set<WireNavEntryId>)
        fun encode(): String
    }

    private class TypedChannel<T>(
        override val type: WireNavigation3ResultType<T>,
        override val contract: WireNavResultContract<T>,
        private val serializer: KSerializer<T>,
        restored: WireNavResultRegistrySnapshot<T>,
    ) : Channel {
        val registry = WireNavResultRegistry(contract, restored)

        override fun onEntryRemoved(
            entryId: WireNavEntryId,
            remainingEntryIds: Set<WireNavEntryId>,
        ) = registry.onEntryRemoved(entryId, remainingEntryIds)

        override fun prune(activeEntryIds: Set<WireNavEntryId>) = registry.prune(activeEntryIds)

        override fun encode(): String = registry.snapshot().encode(serializer)
    }

    private fun <T> WireNavigation3ResultType<T>.toChannel(
        encoded: String?,
    ): Channel = TypedChannel(
        type = this,
        contract = contract,
        serializer = serializer,
        restored = encoded?.let { decodeWireNavResultRegistrySnapshot(it, serializer) }
            ?: WireNavResultRegistrySnapshot(),
    )
}

internal data class WireNavigation3Completion(
    val completed: Boolean,
    val targetEntryId: WireNavEntryId?,
)
