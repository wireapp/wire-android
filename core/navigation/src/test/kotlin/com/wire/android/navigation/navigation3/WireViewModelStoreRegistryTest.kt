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

import androidx.lifecycle.viewmodel.ViewModelStoreProvider
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireSessionId
import com.wire.navigation.WireViewModelOwner
import com.wire.navigation.stableKey
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class WireViewModelStoreRegistryTest {

    @Test
    fun givenSameStableOwnerKey_whenStoreIsAcquiredRepeatedly_thenStoreIsReused() {
        val provider = ViewModelStoreProvider(parentStore = null)

        val first = provider.getOrCreate("flow:login")
        val second = provider.getOrCreate("flow:login")

        assertSame(first, second)
    }

    @Test
    fun givenDifferentOwnerKinds_whenStoresAreAcquired_thenStoresAreIsolated() {
        val provider = ViewModelStoreProvider(parentStore = null)

        val flow = provider.getOrCreate("flow:shared")
        val session = provider.getOrCreate("session:shared")
        val application = provider.getOrCreate("application")

        assertNotSame(flow, session)
        assertNotSame(flow, application)
        assertNotSame(session, application)
    }

    @Test
    fun givenOwnerIsCleared_whenItIsAcquiredAgain_thenFreshStoreIsCreated() {
        val provider = ViewModelStoreProvider(parentStore = null)
        val original = provider.getOrCreate("flow:registration")

        provider.clearKey("flow:registration")

        assertNotSame(original, provider.getOrCreate("flow:registration"))
    }

    @Test
    fun givenOwnerIsStillDisplayed_whenClearIsRequested_thenStoreWaitsForExitAnimation() {
        val provider = ViewModelStoreProvider(parentStore = null)
        val original = provider.getOrCreate("flow:registration")
        val displayedReference = provider.acquireToken("flow:registration")
        var clearedCallbacks = 0
        val registry = WireViewModelStoreRegistry(provider) { clearedCallbacks++ }

        registry.clear(WireViewModelOwner.Flow("registration"))

        assertSame(original, provider.getOrCreate("flow:registration"))
        assertEquals(0, clearedCallbacks)

        displayedReference.close()

        assertNotSame(original, provider.getOrCreate("flow:registration"))
        assertEquals(1, clearedCallbacks)
    }

    @Test
    fun givenSessionIsCleared_whenOtherOwnersExist_thenOnlyThatSessionStoreIsRecreated() {
        val session = WireSessionId("user", "wire.example")
        val provider = ViewModelStoreProvider(parentStore = null)
        val registry = WireViewModelStoreRegistry(provider)
        val sessionOwner = WireViewModelOwner.Session(session)
        val otherOwner = WireViewModelOwner.Flow("login")
        val originalSession = registry.viewModelStoreFor(sessionOwner)
        val originalFlow = registry.viewModelStoreFor(otherOwner)

        registry.clearSession(session)

        assertNotSame(originalSession, registry.viewModelStoreFor(sessionOwner))
        assertSame(originalFlow, registry.viewModelStoreFor(otherOwner))
    }

    @Test
    fun givenApplicationOwnerIsCleared_whenItIsAcquiredAgain_thenFreshStoreIsCreated() {
        val provider = ViewModelStoreProvider(parentStore = null)
        val registry = WireViewModelStoreRegistry(provider)
        val original = registry.viewModelStoreFor(WireViewModelOwner.Application)

        registry.clearApplication()

        assertNotSame(original, registry.viewModelStoreFor(WireViewModelOwner.Application))
    }

    @Test
    fun givenLastFlowEntryIsPopped_whenExitAnimationIsRunning_thenClearedCallbackWaits() {
        val flowOwner = WireViewModelOwner.Flow("login")
        val provider = ViewModelStoreProvider(parentStore = null)
        val cleared = mutableListOf<String>()
        val registry = WireViewModelStoreRegistry(provider, cleared::add)
        registry.registerEntry(contentKey = "login", owners = setOf(flowOwner))
        val animationReference = provider.acquireToken(flowOwner.stableKey())

        registry.onEntryPopped("login")

        assertEquals(emptyList<String>(), cleared)

        animationReference.close()

        assertEquals(listOf(flowOwner.stableKey()), cleared)
    }

    @Test
    fun givenOwnerKinds_whenStableKeysAreCreated_thenIdentityIsDeterministic() {
        val session = WireSessionId("user", "wire.example")

        assertEquals(
            "entry:entry-id",
            WireViewModelOwner.Entry(WireNavEntryId("entry-id")).stableKey(),
        )
        assertEquals("flow:login", WireViewModelOwner.Flow("login").stableKey())
        assertEquals(
            "session:user@wire.example",
            WireViewModelOwner.Session(session).stableKey(),
        )
        assertEquals("application", WireViewModelOwner.Application.stableKey())
    }

    @Test
    fun givenAuthFlowAndTwoAccounts_whenFlowEndsAndOneSessionIsRemoved_thenOnlyDeclaredOwnersAreCleared() {
        val firstSession = WireSessionId("first-user", "wire.example")
        val secondSession = WireSessionId("second-user", "wire.example")
        val flowOwner = WireViewModelOwner.Flow("add-second-account")
        val firstSessionOwner = WireViewModelOwner.Session(firstSession)
        val secondSessionOwner = WireViewModelOwner.Session(secondSession)
        val provider = ViewModelStoreProvider(parentStore = null)
        val cleared = mutableListOf<String>()
        val registry = WireViewModelStoreRegistry(provider, cleared::add)

        registry.registerEntry(
            contentKey = "login",
            owners = setOf(flowOwner, WireViewModelOwner.Application),
        )
        registry.registerEntry(
            contentKey = "too-many-devices",
            owners = setOf(flowOwner, secondSessionOwner, WireViewModelOwner.Application),
        )
        registry.registerEntry(
            contentKey = "first-account-home",
            owners = setOf(firstSessionOwner, WireViewModelOwner.Application),
        )
        val firstStore = registry.viewModelStoreFor(firstSessionOwner)
        val secondStore = registry.viewModelStoreFor(secondSessionOwner)
        val applicationStore = registry.viewModelStoreFor(WireViewModelOwner.Application)

        registry.onEntryPopped("login")
        assertEquals(emptyList<String>(), cleared)
        registry.onEntryPopped("too-many-devices")
        assertEquals(listOf(flowOwner.stableKey()), cleared)

        registry.clearSession(secondSession)

        assertEquals(
            listOf(flowOwner.stableKey(), secondSessionOwner.stableKey()),
            cleared,
        )
        assertSame(firstStore, registry.viewModelStoreFor(firstSessionOwner))
        assertNotSame(secondStore, registry.viewModelStoreFor(secondSessionOwner))
        assertSame(applicationStore, registry.viewModelStoreFor(WireViewModelOwner.Application))
    }
}
