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

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireNavResult
import com.wire.navigation.WireNavResultContract
import com.wire.navigation.WireNavResultContractId
import com.wire.navigation.WireNavResultRequestId
import com.wire.navigation.WireRoute
import com.wire.navigation.availableSharedViewModelOwners
import com.wire.navigation.entryViewModelOwner
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WireNavigation3RuntimeTest {
    private val resultType = WireNavigation3ResultType(
        contract = WireNavResultContract<String>(WireNavResultContractId("string")),
        serializer = String.serializer(),
    )

    @Test
    fun givenEqualRoutes_whenBuildingEntries_thenEachInstanceHasIndependentStableContentKey() {
        val first = route("details", entryId = "first")
        val second = route("details", entryId = "second")
        val provider = entryProvider<NavKey> {
            wireEntry<TestRoute> { }
        }

        assertEquals("first", provider(first).contentKey)
        assertEquals("second", provider(second).contentKey)
    }

    @Test
    fun givenCustomMetadataConflictsWithWireMetadata_whenBuildingEntry_thenWirePoliciesWin() {
        val route = TestRoute(
            routeId = "details",
            entryId = WireNavEntryId("details"),
            flowId = "real-flow",
        )
        val presentation = WireEntryPresentation.PopUp
        val transitionKey = NavDisplay.transitionSpec { null }.keys.single()
        val ownerMetadata = WireViewModelStoreNavEntryDecorator.owners(
            entryOwner = route.entryViewModelOwner(),
            sharedOwners = route.availableSharedViewModelOwners(),
        )
        val provider = entryProvider<NavKey> {
            wireEntry<TestRoute>(
                presentation = presentation,
                metadata = mapOf(
                    "feature-metadata" to "preserved",
                    transitionKey to "feature-transition",
                ) + ownerMetadata.keys.associateWith { "spoofed-owner" },
            ) { }
        }

        val metadata = provider(route).metadata

        assertEquals("preserved", metadata["feature-metadata"])
        assertSame(presentation.navEntryMetadata[transitionKey], metadata[transitionKey])
        ownerMetadata.forEach { (key, value) ->
            assertEquals(value, metadata[key])
        }
    }

    @Test
    fun givenPendingRequest_whenTargetGoesBackNormally_thenRequesterConsumesCancellation() {
        val runtime = runtime(route("home", entryId = "home"))
        val requestId = runtime.navigateForResult(
            destination = route("picker", entryId = "picker"),
            resultType = resultType,
        )

        assertTrue(runtime.navigator.goBack())
        assertEquals(
            WireNavResult.Canceled,
            runtime.consumeResult(requireNotNull(requestId), resultType),
        )
    }

    @Test
    fun givenPendingRequest_whenCompletingAndPopping_thenValueAndPopHappenExactlyOnce() {
        val runtime = runtime(route("home", entryId = "home"))
        val requestId = requireNotNull(
            runtime.navigateForResult(route("picker", entryId = "picker"), resultType)
        )

        assertTrue(runtime.completeAndPop(requestId, resultType, WireNavResult.Value("selected")))
        assertEquals(listOf("home"), runtime.navigator.routes.map { it.entryId.value })
        assertFalse(runtime.completeAndPop(requestId, resultType, WireNavResult.Value("duplicate")))
        assertEquals(WireNavResult.Value("selected"), runtime.consumeResult(requestId, resultType))
        assertNull(runtime.consumeResult(requestId, resultType))
    }

    @Test
    fun givenUniquePendingRequestForCurrentTarget_whenCompletingCurrent_thenValueIsDelivered() {
        val runtime = runtime(route("home", entryId = "home"))
        val requestId = requireNotNull(
            runtime.navigateForResult(route("picker", entryId = "picker"), resultType)
        )

        assertTrue(
            runtime.completeCurrentAndPop(
                resultType = resultType,
                result = WireNavResult.Value("selected"),
            )
        )

        assertEquals(listOf("home"), runtime.navigator.routes.map { it.entryId.value })
        assertEquals(WireNavResult.Value("selected"), runtime.consumeResult(requestId, resultType))
    }

    @Test
    fun givenCurrentEntryHasNoPendingRequest_whenCompletingCurrent_thenStackIsUnchanged() {
        val runtime = runtime(route("home", entryId = "home"))

        assertFalse(
            runtime.completeCurrentAndPop(
                resultType = resultType,
                result = WireNavResult.Value("unexpected"),
            )
        )

        assertEquals(listOf("home"), runtime.navigator.routes.map { it.entryId.value })
    }

    @Test
    fun givenSavedCompletedResult_whenRuntimeIsRestored_thenRequesterConsumesItOnce() {
        val requester = route("home", entryId = "home")
        val originalStore = WireNavigation3ResultStore(listOf(resultType))
        val runtime = runtime(requester, resultStore = originalStore)
        val requestId = requireNotNull(
            runtime.navigateForResult(route("picker", entryId = "picker"), resultType)
        )
        assertTrue(runtime.completeAndPop(requestId, resultType, WireNavResult.Value("restored")))

        val restoredStore = WireNavigation3ResultStore(
            resultTypes = listOf(resultType),
            restored = originalStore.encode(),
        )
        val restoredRuntime = runtime(requester, resultStore = restoredStore)

        assertEquals(
            WireNavResult.Value("restored"),
            restoredRuntime.consumeResult(requestId, resultType),
        )
        assertNull(restoredRuntime.consumeResult(requestId, resultType))
    }

    @Test
    fun givenSavedNullableValue_whenRuntimeIsRestored_thenNullValueIsNotCancellation() {
        val nullableType = WireNavigation3ResultType(
            contract = WireNavResultContract<String?>(WireNavResultContractId("nullable-string")),
            serializer = String.serializer().nullable,
        )
        val requester = route("home", entryId = "home")
        val originalStore = WireNavigation3ResultStore(listOf(nullableType))
        val runtime = runtime(
            requester,
            resultStore = originalStore,
        )
        val requestId = requireNotNull(
            runtime.navigateForResult(route("picker", entryId = "picker"), nullableType)
        )
        assertTrue(runtime.completeAndPop(requestId, nullableType, WireNavResult.Value(null)))

        val restoredRuntime = runtime(
            requester,
            resultStore = WireNavigation3ResultStore(
                resultTypes = listOf(nullableType),
                restored = originalStore.encode(),
            ),
        )

        assertEquals(WireNavResult.Value(null), restoredRuntime.consumeResult(requestId, nullableType))
    }

    @Test
    fun givenPendingRequest_whenStackIsReplaced_thenBrokenRelationshipIsPruned() {
        val runtime = runtime(route("home", entryId = "home"))
        val requestId = requireNotNull(
            runtime.navigateForResult(route("picker", entryId = "picker"), resultType)
        )

        runtime.navigator.replaceBackStack(listOf(route("login", entryId = "login")))

        assertFalse(runtime.completeAndPop(requestId, resultType, WireNavResult.Value("stale")))
        assertNull(runtime.consumeResult(requestId, resultType))
    }

    @Test
    fun givenNavigationIsRejected_whenRequestingResult_thenNoRequestOrTargetIsCreated() {
        val runtime = runtime(
            route("home", entryId = "home"),
            canNavigate = { false },
        )

        val requestId = runtime.navigateForResult(
            route("picker", entryId = "picker"),
            resultType,
        )

        assertNull(requestId)
        assertEquals(listOf("home"), runtime.navigator.routes.map { it.entryId.value })
    }

    private fun runtime(
        vararg routes: TestRoute,
        resultStore: WireNavigation3ResultStore = WireNavigation3ResultStore(listOf(resultType)),
        canNavigate: (com.wire.navigation.WireNavigationCommand) -> Boolean = { true },
    ): WireNavigation3Runtime {
        var nextRequest = 0
        return WireNavigation3Runtime(
            backStack = NavBackStack(*routes.map { it as NavKey }.toTypedArray()),
            resultStore = resultStore,
            canNavigate = canNavigate,
            requestIdFactory = { WireNavResultRequestId("request-${nextRequest++}") },
        )
    }

    private fun route(routeId: String, entryId: String) = TestRoute(
        routeId = routeId,
        entryId = WireNavEntryId(entryId),
    )

    @Serializable
    private data class TestRoute(
        override val routeId: String,
        override val entryId: WireNavEntryId,
        override val flowId: String? = null,
    ) : WireRoute
}
