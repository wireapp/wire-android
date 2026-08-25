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

package com.wire.navigation

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Serializable snapshot used to restore outstanding requests after recreation.
 */
@Serializable
data class WireNavResultRegistrySnapshot<T>(
    val records: List<WireNavResultRecord<T>> = emptyList(),
)

/**
 * Encodes one typed channel without leaking JSON into the platform runtime.
 */
fun <T> WireNavResultRegistrySnapshot<T>.encode(
    valueSerializer: KSerializer<T>,
): String = Json.encodeToString(
    serializer = WireNavResultRegistrySnapshot.serializer(valueSerializer),
    value = this,
)

/**
 * Restores one typed channel at the platform persistence boundary.
 */
fun <T> decodeWireNavResultRegistrySnapshot(
    encoded: String,
    valueSerializer: KSerializer<T>,
): WireNavResultRegistrySnapshot<T> = Json.decodeFromString(
    deserializer = WireNavResultRegistrySnapshot.serializer(valueSerializer),
    string = encoded,
)

/**
 * Platform-independent state machine for one typed result channel.
 *
 * Mutation is intentionally centralized here. [complete] performs one Pending -> Completed
 * transition and rejects every later producer, while [consume] removes a completed record before
 * returning it. The owning navigation runtime must serialize calls to this registry on its state
 * thread, just like mutations of the application-owned back stack.
 */
class WireNavResultRegistry<T>(
    val contract: WireNavResultContract<T>,
    restored: WireNavResultRegistrySnapshot<T> = WireNavResultRegistrySnapshot(),
) {
    private val records = restored.records.associateByTo(mutableMapOf()) { it.request.id }

    init {
        require(records.size == restored.records.size) {
            "A restored navigation result snapshot contains duplicate request ids"
        }
        require(records.values.all { it.request.contractId == contract.id }) {
            "A restored navigation result snapshot contains a different result contract"
        }
    }

    fun snapshot(): WireNavResultRegistrySnapshot<T> =
        WireNavResultRegistrySnapshot(records.values.toList())

    /**
     * Registers a request before its target entry is shown.
     *
     * Reusing a request id is rejected, even if the entries happen to be identical.
     */
    fun register(request: WireNavResultRequest): Boolean {
        require(request.contractId == contract.id) {
            "A navigation result request belongs to a different result contract"
        }
        if (request.id in records) return false
        records[request.id] = WireNavResultRecord(request)
        return true
    }

    /**
     * Atomically claims completion of a pending request.
     *
     * Returns false for an unknown or already completed request, ensuring that only the first
     * result producer wins.
     */
    fun complete(requestId: WireNavResultRequestId, result: WireNavResult<T>): Boolean {
        val record = records[requestId]
        return if (record?.state is WireNavResultState.Pending) {
            records[requestId] = record.copy(state = WireNavResultState.Completed(result))
            true
        } else {
            false
        }
    }

    /**
     * Rolls back a request that could not create its target entry.
     *
     * Only a still-pending request can be discarded; terminal results remain consumable.
     */
    fun discardPending(requestId: WireNavResultRequestId): Boolean {
        val record = records[requestId]
        return if (record?.state is WireNavResultState.Pending) {
            records.remove(requestId)
            true
        } else {
            false
        }
    }

    /**
     * Returns a completed result to its requester at most once.
     *
     * The requester identity is checked so another copy of the same route cannot consume it.
     */
    fun consume(
        requestId: WireNavResultRequestId,
        requesterEntryId: WireNavEntryId,
    ): WireNavResult<T>? {
        val record = records[requestId]
        val completed = record?.state as? WireNavResultState.Completed
        val isRequester = record?.request?.requesterEntryId == requesterEntryId
        return completed?.takeIf { isRequester }?.also {
            records.remove(requestId)
        }?.result
    }

    /**
     * Handles an ordinary pop of [entryId].
     *
     * Requests owned by the removed requester are discarded. A pending request targeting the
     * removed entry completes as [WireNavResult.Canceled], provided its requester still exists.
     */
    fun onEntryRemoved(entryId: WireNavEntryId, remainingEntryIds: Set<WireNavEntryId>) {
        records.entries.removeAll { (_, record) ->
            record.request.requesterEntryId == entryId ||
                record.request.requesterEntryId !in remainingEntryIds
        }
        records.keys.toList().forEach { requestId ->
            val record = records.getValue(requestId)
            if (
                record.request.targetEntryId == entryId &&
                record.state is WireNavResultState.Pending
            ) {
                records[requestId] = record.copy(
                    state = WireNavResultState.Completed<T>(WireNavResult.Canceled),
                )
            }
        }
    }

    /**
     * Prunes stale requests after a non-linear stack replacement or session switch.
     *
     * Unlike [onEntryRemoved], this does not synthesize cancellation: neither side can safely
     * observe it once its restored entry relationship has been broken.
     */
    fun prune(activeEntryIds: Set<WireNavEntryId>) {
        records.entries.removeAll { (_, record) ->
            record.request.requesterEntryId !in activeEntryIds ||
                record.request.targetEntryId !in activeEntryIds
        }
    }

    /**
     * Drops all outstanding cross-entry state when the navigation session changes.
     */
    fun clear() {
        records.clear()
    }

    /**
     * Resolves the single pending request targeting [targetEntryId].
     *
     * The destination does not carry a request id as a navigation argument. A fresh target entry
     * normally has exactly one producer relationship; returning `null` for both missing and
     * ambiguous relationships keeps a target from completing the wrong caller.
     */
    fun uniquePendingRequestForTarget(
        targetEntryId: WireNavEntryId,
    ): WireNavResultRequestId? {
        val matches = records.values.asSequence()
            .filter { record ->
                record.request.targetEntryId == targetEntryId &&
                    record.state is WireNavResultState.Pending
            }
            .map { it.request.id }
            .take(2)
            .toList()
        return matches.singleOrNull()
    }

    internal fun pendingTarget(requestId: WireNavResultRequestId): WireNavEntryId? {
        val record = records[requestId] ?: return null
        return if (record.state is WireNavResultState.Pending) {
            record.request.targetEntryId
        } else {
            null
        }
    }
}

/**
 * Navigation-boundary operation that keeps producing a result and removing its target together.
 *
 * The runtime must invoke this coordinator on the same serialized state thread as its back-stack
 * mutations. [removeTarget] must only remove the supplied entry and must not independently notify
 * the registry; reconciliation happens after this operation returns.
 */
class WireNavResultCoordinator<T>(
    private val registry: WireNavResultRegistry<T>,
    private val removeTarget: (WireNavEntryId) -> Boolean,
) {
    fun completeAndPop(
        requestId: WireNavResultRequestId,
        result: WireNavResult<T>,
    ): Boolean {
        val targetEntryId = registry.pendingTarget(requestId)
        return if (targetEntryId != null && removeTarget(targetEntryId)) {
            check(registry.complete(requestId, result)) {
                "Navigation result state changed during a serialized complete-and-pop operation"
            }
            true
        } else {
            false
        }
    }
}
