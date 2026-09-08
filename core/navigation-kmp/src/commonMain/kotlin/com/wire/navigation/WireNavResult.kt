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

import kotlin.jvm.JvmInline
import kotlin.random.Random
import kotlinx.serialization.Serializable

private const val ENTRY_ID_BYTE_COUNT = 16
private const val HEX_RADIX = 16
private const val HEX_BYTE_WIDTH = 2

/**
 * Opaque identity of one concrete entry on the back stack.
 *
 * This must be unique for every entry instance, including two entries created from the same
 * [WireRoute]. A route id is consequently not a valid entry id.
 */
@Serializable
@JvmInline
value class WireNavEntryId(val value: String) {
    init {
        require(value.isNotBlank()) { "A navigation entry id cannot be blank" }
    }

    companion object {
        /**
         * Creates an opaque entry identity once, before a route enters the saveable back stack.
         */
        fun random(random: Random = Random.Default): WireNavEntryId {
            val bytes = ByteArray(ENTRY_ID_BYTE_COUNT)
            random.nextBytes(bytes)
            return WireNavEntryId(
                bytes.joinToString(separator = "") { byte ->
                    byte.toUByte().toString(radix = HEX_RADIX).padStart(HEX_BYTE_WIDTH, '0')
                }
            )
        }
    }
}

/**
 * Opaque identity of one result request.
 *
 * The identity is supplied by the runtime so it can be generated once and restored together with
 * the back stack.
 */
@Serializable
@JvmInline
value class WireNavResultRequestId(val value: String) {
    init {
        require(value.isNotBlank()) { "A navigation result request id cannot be blank" }
    }
}

/**
 * Saveable identity of a typed result contract.
 */
@Serializable
@JvmInline
value class WireNavResultContractId(val value: String) {
    init {
        require(value.isNotBlank()) { "A navigation result contract id cannot be blank" }
    }
}

/**
 * Runtime type token for a navigation result channel.
 *
 * Only [id] enters saved state. Serializers remain a concern of the persistence boundary, which
 * allows the common model to support multiple heterogeneous contracts without storing serializer
 * objects or platform-specific type information.
 */
class WireNavResultContract<T>(val id: WireNavResultContractId)

/**
 * The terminal outcome of a typed navigation result request.
 *
 * [Value] intentionally accepts nullable values. This keeps `Value(null)` distinct from both
 * [Canceled] and a request that has not completed yet.
 */
@Serializable
sealed interface WireNavResult<out T> {
    @Serializable
    data class Value<T>(val value: T) : WireNavResult<T>

    @Serializable
    data object Canceled : WireNavResult<Nothing>
}

/**
 * Stable addressing for a result traveling from [targetEntryId] back to [requesterEntryId].
 */
@Serializable
data class WireNavResultRequest(
    val id: WireNavResultRequestId,
    val contractId: WireNavResultContractId,
    val requesterEntryId: WireNavEntryId,
    val targetEntryId: WireNavEntryId,
) {
    init {
        require(requesterEntryId != targetEntryId) {
            "Requester and target must be different navigation entries"
        }
    }
}

/**
 * Saveable state of a request. A completed result remains here until it is consumed exactly once.
 */
@Serializable
sealed interface WireNavResultState<out T> {
    @Serializable
    data object Pending : WireNavResultState<Nothing>

    @Serializable
    data class Completed<T>(val result: WireNavResult<T>) : WireNavResultState<T>
}

@Serializable
data class WireNavResultRecord<T>(
    val request: WireNavResultRequest,
    val state: WireNavResultState<T> = WireNavResultState.Pending,
)
