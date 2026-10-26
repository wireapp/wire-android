/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.wire.android.ui

import com.wire.android.di.metro.AppSessionViewModelGraph
import com.wire.android.navigation.runtime.SessionGraphStoreViewModel
import com.wire.android.navigation.runtime.SessionGraphStoreViewModel.Availability
import com.wire.android.navigation.runtime.SessionGraphStoreViewModel.Lifecycle
import com.wire.android.navigation.runtime.SessionGraphUnavailableException
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.LogoutCallback
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionGraphReactivationTest {
    private val dispatcher = StandardTestDispatcher()
    private val userA = UserId("a", "wire.test")
    private val userB = UserId("b", "wire.test")

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun inactiveAccountLogoutDisposesOnlyThatAccountAndAllowsFreshGraphAfterConfirmation() = runTest {
        val loader = mockk<WireSessionImageLoader>(relaxed = true)
        val oldA = mockk<AppSessionViewModelGraph> { every { wireSessionImageLoader } returns loader }
        val newA = mockk<AppSessionViewModelGraph>()
        val graphB = mockk<AppSessionViewModelGraph>()
        val graphsA = ArrayDeque(listOf(oldA, newA))
        lateinit var logout: LogoutCallback
        val store = SessionGraphStoreViewModel(
            createSessionGraph = { if (it == userA) graphsA.removeFirst() else graphB },
            registerLogoutCallback = { logout = it },
        )
        assertSame(oldA, store.graphFor(userA))
        assertSame(graphB, store.graphFor(userB))
        logout(userA, LogoutReason.REMOVED_CLIENT)
        assertEquals(Lifecycle.INVALIDATING, store.lifecycle(userA))
        assertSame(graphB, store.graphFor(userB))
        verify(exactly = 0) { loader.shutdown() }
        lateinit var ownerCleared: () -> Unit
        val reactivation = async {
            reactivateSessionGraph(store, userA, { ownerCleared = it }, { true }, { true })
        }
        runCurrent()
        assertEquals(Availability.DEFERRED, store.markAvailable(userA, store.invalidationVersion(userA)))
        assertThrows(SessionGraphUnavailableException::class.java) { store.graphFor(userA) }
        verify(exactly = 0) { loader.shutdown() }
        ownerCleared()
        // The clear observer may fire before other ViewModels have finished onCleared().
        verify(exactly = 0) { loader.shutdown() }
        runCurrent()
        verify(exactly = 1) { loader.shutdown() }
        assertTrue(reactivation.await())
        assertSame(newA, store.graphFor(userA))
        assertNotSame(oldA, store.graphFor(userA))
        assertSame(graphB, store.graphFor(userB))
    }

    @Test
    fun accountWithoutAGraphStillCompletesTeardown() = runTest {
        var created = 0
        val store = SessionGraphStoreViewModel(createSessionGraph = {
            created++
            mockk()
        })
        store.markInvalidating(userA)
        assertTrue(reactivateSessionGraph(store, userA, { it() }, { true }, { true }))
        assertEquals(0, created)
        store.graphFor(userA)
        assertEquals(1, created)
    }

    @Test
    fun repeatedLogoutAndRecreatedHostCannotDuplicatePendingTeardown() = runTest {
        val loader = mockk<WireSessionImageLoader>(relaxed = true)
        val graph = mockk<AppSessionViewModelGraph> { every { wireSessionImageLoader } returns loader }
        val store = SessionGraphStoreViewModel(createSessionGraph = { graph })
        store.graphFor(userA)
        store.markInvalidating(userA)
        lateinit var ownerCleared: () -> Unit
        val first = store.removeAfterOwnerCleared(userA) { ownerCleared = it }
        val oldHost = async { first.await() }
        runCurrent()
        oldHost.cancel()
        store.markInvalidating(userA)
        val recreatedHost = store.removeAfterOwnerCleared(userA) { error("Owner clear must not be requested twice") }
        assertSame(first, recreatedHost)
        assertFalse(first.isCancelled)
        ownerCleared()
        ownerCleared()
        runCurrent()
        recreatedHost.await()
        assertEquals(Lifecycle.REMOVED, store.lifecycle(userA))
        verify(exactly = 1) { loader.shutdown() }
    }

    @Test
    fun logoutDuringSuspendedConfirmationRejectsStaleResult() = runTest {
        val store = SessionGraphStoreViewModel(createSessionGraph = { mockk() })
        store.markRemoved(userA)
        val confirmation = CompletableDeferred<Boolean>()
        var checks = 0
        val result = async {
            reactivateSessionGraph(store, userA, { it() }, {
                checks++
                if (checks == 1) confirmation.await() else false
            }, { true })
        }
        runCurrent()
        store.markInvalidating(userA)
        confirmation.complete(true)
        assertFalse(result.await())
        assertEquals(2, checks)
        assertEquals(Lifecycle.REMOVED, store.lifecycle(userA))
        assertThrows(SessionGraphUnavailableException::class.java) { store.graphFor(userA) }
    }

    @Test
    fun changedRouteOrInvalidSessionDoesNotReopenTombstone() = runTest {
        val store = SessionGraphStoreViewModel(createSessionGraph = { mockk() })
        store.markRemoved(userA)
        val confirmation = CompletableDeferred<Boolean>()
        var isCurrent = true
        val reactivation = async { reactivateSessionGraph(store, userA, { it() }, { confirmation.await() }, { isCurrent }) }
        runCurrent()
        isCurrent = false
        confirmation.complete(true)
        assertFalse(reactivation.await())
        assertFalse(reactivateSessionGraph(store, userA, { it() }, { false }, { true }))
        assertEquals(Lifecycle.REMOVED, store.lifecycle(userA))
    }

    @Test
    fun explicitTeardownWaitsForOwnerAndRejectsConcurrentAvailability() = runTest {
        val events = mutableListOf<String>()
        val loader = mockk<WireSessionImageLoader> { every { shutdown() } answers { events += "graph" } }
        val graph = mockk<AppSessionViewModelGraph> { every { wireSessionImageLoader } returns loader }
        val store = SessionGraphStoreViewModel(createSessionGraph = { graph })
        store.graphFor(userA)
        lateinit var ownerCleared: () -> Unit
        val removal = teardownWireActivitySession(
            com.wire.navigation.WireSessionId(userA.value, userA.domain),
            store,
        ) { _, onCleared -> ownerCleared = onCleared }
        assertEquals(Availability.DEFERRED, store.markAvailable(userA, store.invalidationVersion(userA)))
        assertFalse(removal.isCompleted)
        ownerCleared()
        events += "viewModels"
        assertEquals(listOf("viewModels"), events)
        removal.await()
        assertEquals(listOf("viewModels", "graph"), events)
        assertEquals(Lifecycle.REMOVED, store.lifecycle(userA))
    }
}
