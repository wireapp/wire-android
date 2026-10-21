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
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WireNavResultRegistryTest {
    private val stringContract = WireNavResultContract<String>(WireNavResultContractId("string"))
    private val nullableStringContract =
        WireNavResultContract<String?>(WireNavResultContractId("nullable-string"))

    @Test
    fun givenTwoInstancesOfSameRoute_whenResultCompletes_thenOnlyRequestingEntryCanConsumeIt() {
        val registry = WireNavResultRegistry(stringContract)
        val request = request(requester = "same-route-entry-1", target = "same-route-entry-2")
        registry.register(request)
        registry.complete(request.id, WireNavResult.Value("result"))

        assertNull(registry.consume(request.id, entry("same-route-entry-2")))
        assertEquals(
            WireNavResult.Value("result"),
            registry.consume(request.id, entry("same-route-entry-1")),
        )
    }

    @Test
    fun givenNullableResult_whenCompletingWithNull_thenItIsDistinctFromCancellation() {
        val registry = WireNavResultRegistry(nullableStringContract)
        val request = request(contractId = nullableStringContract.id)
        registry.register(request)

        assertTrue(registry.complete(request.id, WireNavResult.Value(null)))

        val result = registry.consume(request.id, request.requesterEntryId)
        assertIs<WireNavResult.Value<String?>>(result)
        assertNull(result.value)
    }

    @Test
    fun givenPendingRequest_whenTwoProducersComplete_thenOnlyFirstCompletionWins() {
        val registry = WireNavResultRegistry(stringContract)
        val request = request()
        registry.register(request)

        assertTrue(registry.complete(request.id, WireNavResult.Value("first")))
        assertFalse(registry.complete(request.id, WireNavResult.Value("second")))
        assertEquals(
            WireNavResult.Value("first"),
            registry.consume(request.id, request.requesterEntryId),
        )
    }

    @Test
    fun givenCompletedRequest_whenConsumedTwice_thenSecondConsumptionReturnsNothing() {
        val registry = WireNavResultRegistry(stringContract)
        val request = request()
        registry.register(request)
        registry.complete(request.id, WireNavResult.Value("once"))

        assertEquals(
            WireNavResult.Value("once"),
            registry.consume(request.id, request.requesterEntryId),
        )
        assertNull(registry.consume(request.id, request.requesterEntryId))
    }

    @Test
    fun givenPendingRequest_whenTargetNavigationIsRejected_thenRequestCanBeRolledBack() {
        val registry = WireNavResultRegistry(stringContract)
        val request = request()
        registry.register(request)

        assertTrue(registry.discardPending(request.id))
        assertFalse(registry.discardPending(request.id))
        assertTrue(registry.snapshot().records.isEmpty())
    }

    @Test
    fun givenPendingRequest_whenTargetIsPoppedNormally_thenRequesterReceivesCancellation() {
        val registry = WireNavResultRegistry(stringContract)
        val request = request()
        registry.register(request)

        registry.onEntryRemoved(
            entryId = request.targetEntryId,
            remainingEntryIds = setOf(request.requesterEntryId),
        )

        assertEquals(
            WireNavResult.Canceled,
            registry.consume(request.id, request.requesterEntryId),
        )
    }

    @Test
    fun givenCompletedRequest_whenTargetIsPopped_thenExistingValueIsPreserved() {
        val registry = WireNavResultRegistry(stringContract)
        val request = request()
        registry.register(request)
        registry.complete(request.id, WireNavResult.Value("saved"))

        registry.onEntryRemoved(
            entryId = request.targetEntryId,
            remainingEntryIds = setOf(request.requesterEntryId),
        )

        assertEquals(
            WireNavResult.Value("saved"),
            registry.consume(request.id, request.requesterEntryId),
        )
    }

    @Test
    fun givenPendingRequest_whenRequesterIsPopped_thenRequestIsDiscarded() {
        val registry = WireNavResultRegistry(stringContract)
        val request = request()
        registry.register(request)

        registry.onEntryRemoved(
            entryId = request.requesterEntryId,
            remainingEntryIds = setOf(request.targetEntryId),
        )

        assertTrue(registry.snapshot().records.isEmpty())
    }

    @Test
    fun givenRequests_whenStackIsReplaced_thenRelationshipsOutsideNewStackArePruned() {
        val registry = WireNavResultRegistry(stringContract)
        val retained = request(id = "retained", requester = "a", target = "b")
        val staleRequester = request(id = "stale-requester", requester = "old", target = "b")
        val staleTarget = request(id = "stale-target", requester = "a", target = "old")
        listOf(retained, staleRequester, staleTarget).forEach(registry::register)

        registry.prune(setOf(entry("a"), entry("b")))

        assertEquals(listOf(retained.id), registry.snapshot().records.map { it.request.id })
    }

    @Test
    fun givenRequests_whenNavigationSessionChanges_thenAllRequestsAreCleared() {
        val registry = WireNavResultRegistry(stringContract)
        registry.register(request())

        registry.clear()

        assertTrue(registry.snapshot().records.isEmpty())
    }

    @Test
    fun givenPendingAndCompletedRequests_whenSnapshotIsSerialized_thenStatesRestoreExactly() {
        val pending = request(id = "pending", requester = "a", target = "b")
        val completed = request(id = "completed", requester = "a", target = "c")
        val registry = WireNavResultRegistry(stringContract)
        registry.register(pending)
        registry.register(completed)
        registry.complete(completed.id, WireNavResult.Value("value"))

        val encoded = Json.encodeToString(registry.snapshot())
        val restoredSnapshot =
            Json.decodeFromString<WireNavResultRegistrySnapshot<String>>(encoded)
        val restored = WireNavResultRegistry(stringContract, restoredSnapshot)

        assertIs<WireNavResultState.Pending>(restored.snapshot().records[0].state)
        assertEquals(
            WireNavResult.Value("value"),
            restored.consume(completed.id, completed.requesterEntryId),
        )
    }

    @Test
    fun givenLegacySavedResultVariants_whenDecoded_thenPayloadsAndDefaultsArePreserved() {
        val request = """{"id":"saved","contractId":"nullable-string","requesterEntryId":"home","targetEntryId":"picker"}"""
        val stateType = "com.wire.navigation.WireNavResultState"
        val resultType = "com.wire.navigation.WireNavResult"
        val fixtures = listOf<Pair<String, WireNavResultState<String?>>>(
            "" to WireNavResultState.Pending,
            ""","state":{"type":"$stateType.Pending"}""" to WireNavResultState.Pending,
            ""","state":{"type":"$stateType.Completed","result":{"type":"$resultType.Canceled"}}""" to
                WireNavResultState.Completed(WireNavResult.Canceled),
            ""","state":{"type":"$stateType.Completed","result":{"type":"$resultType.Value","value":"saved"}}""" to
                WireNavResultState.Completed(WireNavResult.Value("saved")),
            ""","state":{"type":"$stateType.Completed","result":{"type":"$resultType.Value","value":null}}""" to
                WireNavResultState.Completed(WireNavResult.Value(null)),
        )
        val serializer = String.serializer().nullable

        fixtures.forEach { (state, expected) ->
            val encoded = """{"records":[{"request":$request$state}]}"""
            val decoded = decodeWireNavResultRegistrySnapshot(encoded, serializer)

            assertEquals(expected, decoded.records.single().state)
            val canonical = if (expected == WireNavResultState.Pending) {
                """{"records":[{"request":$request}]}"""
            } else {
                encoded
            }
            assertEquals(Json.parseToJsonElement(canonical), Json.parseToJsonElement(decoded.encode(serializer)))
        }
    }

    @Test
    fun givenCustomNullablePayloadSerializer_whenRestoringSnapshot_thenTypedResultsAreConsumedOnce() {
        val serializer = object : KSerializer<Int> {
            override val descriptor = String.serializer().descriptor
            override fun serialize(encoder: Encoder, value: Int) = encoder.encodeString("payload-$value")
            override fun deserialize(decoder: Decoder): Int = decoder.decodeString().removePrefix("payload-").toInt()
        }.nullable
        val contract = WireNavResultContract<Int?>(WireNavResultContractId("custom"))
        val registry = WireNavResultRegistry(contract)
        val values = listOf(42, null)
        val requests = values.mapIndexed { index, value ->
            request(id = "custom-$index", contractId = contract.id).also {
                registry.register(it)
                registry.complete(it.id, WireNavResult.Value(value))
            }
        }
        val encoded = registry.snapshot().encode(serializer)
        assertTrue(encoded.contains("\"value\":\"payload-42\""))
        val restored = WireNavResultRegistry(contract, decodeWireNavResultRegistrySnapshot(encoded, serializer))

        requests.zip(values).forEach { (request, value) ->
            assertEquals(WireNavResult.Value(value), restored.consume(request.id, request.requesterEntryId))
            assertNull(restored.consume(request.id, request.requesterEntryId))
        }
    }

    @Test
    fun givenDuplicateRequestId_whenRegisteringAgain_thenSecondRegistrationIsRejected() {
        val registry = WireNavResultRegistry(stringContract)
        val first = request(id = "same", requester = "a", target = "b")
        val second = request(id = "same", requester = "c", target = "d")

        assertTrue(registry.register(first))
        assertFalse(registry.register(second))
        assertEquals(listOf(first), registry.snapshot().records.map { it.request })
    }

    @Test
    fun givenOnePendingRequestForTarget_whenResolvingByTarget_thenItsIdIsReturned() {
        val registry = WireNavResultRegistry(stringContract)
        val request = request(id = "unique")
        registry.register(request)

        assertEquals(
            request.id,
            registry.uniquePendingRequestForTarget(request.targetEntryId),
        )
    }

    @Test
    fun givenMultiplePendingRequestsForTarget_whenResolvingByTarget_thenNothingIsReturned() {
        val registry = WireNavResultRegistry(stringContract)
        registry.register(request(id = "first", requester = "first-requester"))
        registry.register(request(id = "second", requester = "second-requester"))

        assertNull(registry.uniquePendingRequestForTarget(entry("target-entry")))
    }

    @Test
    fun givenDifferentTypedContracts_whenRequestsCoexist_thenEachRegistryOwnsOnlyItsContract() {
        val integerContract = WireNavResultContract<Int>(WireNavResultContractId("integer"))
        val strings = WireNavResultRegistry(stringContract)
        val integers = WireNavResultRegistry(integerContract)
        val stringRequest = request(id = "string-request")
        val integerRequest = request(id = "integer-request", contractId = integerContract.id)

        assertTrue(strings.register(stringRequest))
        assertTrue(integers.register(integerRequest))
        assertTrue(strings.complete(stringRequest.id, WireNavResult.Value("typed")))
        assertTrue(integers.complete(integerRequest.id, WireNavResult.Value(42)))

        assertEquals(
            WireNavResult.Value("typed"),
            strings.consume(stringRequest.id, stringRequest.requesterEntryId),
        )
        assertEquals(
            WireNavResult.Value(42),
            integers.consume(integerRequest.id, integerRequest.requesterEntryId),
        )
    }

    @Test
    fun givenPendingRequest_whenCompletingAndPopping_thenBothMutationsUseOneCoordinatorOperation() {
        val registry = WireNavResultRegistry(stringContract)
        val request = request()
        registry.register(request)
        val removedEntries = mutableListOf<WireNavEntryId>()
        val coordinator = WireNavResultCoordinator(registry) {
            removedEntries += it
            true
        }

        assertTrue(coordinator.completeAndPop(request.id, WireNavResult.Value("done")))
        assertFalse(coordinator.completeAndPop(request.id, WireNavResult.Value("duplicate")))

        assertEquals(listOf(request.targetEntryId), removedEntries)
        assertEquals(
            WireNavResult.Value("done"),
            registry.consume(request.id, request.requesterEntryId),
        )
    }

    @Test
    fun givenTargetCannotBePopped_whenCompletingAndPopping_thenRequestRemainsPending() {
        val registry = WireNavResultRegistry(stringContract)
        val request = request()
        registry.register(request)
        val coordinator = WireNavResultCoordinator(registry, removeTarget = { false })

        assertFalse(coordinator.completeAndPop(request.id, WireNavResult.Value("not-delivered")))

        assertIs<WireNavResultState.Pending>(registry.snapshot().records.single().state)
    }

    private fun request(
        id: String = "request",
        requester: String = "requester-entry",
        target: String = "target-entry",
        contractId: WireNavResultContractId = stringContract.id,
    ) = WireNavResultRequest(
        id = WireNavResultRequestId(id),
        contractId = contractId,
        requesterEntryId = entry(requester),
        targetEntryId = entry(target),
    )

    private fun entry(value: String) = WireNavEntryId(value)
}
