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

package com.wire.android.navigation.runtime

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wire.android.di.metro.AppSessionViewModelGraph
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.LogoutCallback
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SessionGraphStoreViewModelTest {

    @Test
    fun givenSameUser_whenResolvingGraphMultipleTimes_thenSameGraphIsReturned() {
        val expectedGraph = mockk<AppSessionViewModelGraph>()
        var creationCount = 0
        val store = SessionGraphStoreViewModel(
            createSessionGraph = {
                creationCount++
                expectedGraph
            }
        )
        val userId = UserId("user-one", "wire.test")

        val firstGraph = store.graphFor(userId)
        val secondGraph = store.graphFor(userId)

        assertSame(expectedGraph, firstGraph)
        assertSame(firstGraph, secondGraph)
        assertEquals(1, creationCount)
    }

    @Test
    fun givenDifferentUsers_whenResolvingGraphs_thenEachUserHasItsOwnGraph() {
        val firstGraph = mockk<AppSessionViewModelGraph>(relaxed = true)
        val secondGraph = mockk<AppSessionViewModelGraph>(relaxed = true)
        val graphs = listOf(firstGraph, secondGraph).iterator()
        val store = SessionGraphStoreViewModel(createSessionGraph = { graphs.next() })

        val resolvedFirstGraph = store.graphFor(UserId("user-one", "wire.test"))
        val resolvedSecondGraph = store.graphFor(UserId("user-two", "wire.test"))

        assertSame(firstGraph, resolvedFirstGraph)
        assertSame(secondGraph, resolvedSecondGraph)
    }

    @Test
    fun givenActiveCallGraph_whenReleased_thenItIsDisposedWithoutSessionTombstone() {
        val userId = UserId("user-one", "wire.test")
        val firstLoader = mockk<WireSessionImageLoader>(relaxed = true)
        val secondLoader = mockk<WireSessionImageLoader>(relaxed = true)
        val firstGraph = mockk<AppSessionViewModelGraph>(relaxed = true) {
            every { wireSessionImageLoader } returns firstLoader
        }
        val secondGraph = mockk<AppSessionViewModelGraph>(relaxed = true) {
            every { wireSessionImageLoader } returns secondLoader
        }
        val graphs = listOf(firstGraph, secondGraph).iterator()
        val store = SessionGraphStoreViewModel(createSessionGraph = { graphs.next() })

        assertSame(firstGraph, store.graphFor(userId))
        store.release(userId)

        assertEquals(null, store.lifecycle(userId))
        verify(exactly = 1) { firstLoader.shutdown() }
        assertSame(secondGraph, store.graphFor(userId))
        verify(exactly = 0) { secondLoader.shutdown() }
    }

    @Test
    fun givenInvalidatingGraph_whenReleased_thenLogoutTombstoneIsPreserved() {
        val userId = UserId("user-one", "wire.test")
        val loader = mockk<WireSessionImageLoader>(relaxed = true)
        val graph = mockk<AppSessionViewModelGraph>(relaxed = true) {
            every { wireSessionImageLoader } returns loader
        }
        val store = SessionGraphStoreViewModel(createSessionGraph = { graph })
        store.graphFor(userId)
        store.markInvalidating(userId)

        store.release(userId)

        assertEquals(SessionGraphStoreViewModel.Lifecycle.REMOVED, store.lifecycle(userId))
        assertThrows(SessionGraphUnavailableException::class.java) { store.graphFor(userId) }
        verify(exactly = 1) { loader.shutdown() }
    }

    @Test
    fun givenSessionIsLoggedOut_whenStaleRouteResolvesSameUser_thenGraphIsNotRecreated() = runTest {
        val userId = UserId("user-one", "wire.test")
        val firstGraph = mockk<AppSessionViewModelGraph>(relaxed = true)
        var creationCount = 0
        var logoutCallback: LogoutCallback? = null
        val store = SessionGraphStoreViewModel(
            createSessionGraph = {
                creationCount++
                firstGraph
            },
            registerLogoutCallback = { logoutCallback = it },
        )

        val resolvedBeforeLogout = store.graphFor(userId)
        requireNotNull(logoutCallback).invoke(userId, LogoutReason.SELF_HARD_LOGOUT)

        assertSame(firstGraph, resolvedBeforeLogout)
        assertEquals(SessionGraphStoreViewModel.Lifecycle.INVALIDATING, store.lifecycle(userId))
        assertThrows(SessionGraphUnavailableException::class.java) { store.graphFor(userId) }
        assertEquals(1, creationCount)
    }

    @Test
    fun givenSessionIsInvalidating_whenStaleRouteResolvesSameUser_thenGraphIsNotRecreated() {
        val userId = UserId("user-one", "wire.test")
        val graph = mockk<AppSessionViewModelGraph>(relaxed = true)
        val imageLoader = mockk<WireSessionImageLoader>(relaxed = true)
        every { graph.wireSessionImageLoader } returns imageLoader
        var creationCount = 0
        val store = SessionGraphStoreViewModel(
            createSessionGraph = {
                creationCount++
                graph
            }
        )
        store.graphFor(userId)

        store.markInvalidating(userId)

        assertEquals(SessionGraphStoreViewModel.Lifecycle.INVALIDATING, store.lifecycle(userId))
        assertThrows(SessionGraphUnavailableException::class.java) { store.graphFor(userId) }
        assertEquals(1, creationCount)
        verify(exactly = 0) { imageLoader.shutdown() }

        store.markRemoved(userId)

        verify(exactly = 1) { imageLoader.shutdown() }
    }

    @Test
    fun givenRemovedSessionIsConfirmedAvailable_whenResolvingAgain_thenFreshGraphIsCreated() {
        val userId = UserId("user-one", "wire.test")
        val firstGraph = mockk<AppSessionViewModelGraph>(relaxed = true)
        val secondGraph = mockk<AppSessionViewModelGraph>(relaxed = true)
        val graphs = listOf(firstGraph, secondGraph).iterator()
        val store = SessionGraphStoreViewModel(createSessionGraph = { graphs.next() })
        val resolvedBeforeRemoval = store.graphFor(userId)

        store.markRemoved(userId)
        store.markAvailable(userId)
        val resolvedAfterReactivation = store.graphFor(userId)

        assertSame(firstGraph, resolvedBeforeRemoval)
        assertSame(secondGraph, resolvedAfterReactivation)
        assertNotSame(resolvedBeforeRemoval, resolvedAfterReactivation)
        assertEquals(SessionGraphStoreViewModel.Lifecycle.ACTIVE, store.lifecycle(userId))
    }

    @Test
    fun givenInvalidatingSession_whenMarkedAvailableBeforeRemoval_thenOldGenerationCannotReopen() {
        val userId = UserId("user-one", "wire.test")
        val graph = mockk<AppSessionViewModelGraph>(relaxed = true)
        val store = SessionGraphStoreViewModel(createSessionGraph = { graph })
        store.graphFor(userId)
        store.markInvalidating(userId)

        assertThrows(IllegalStateException::class.java) { store.markAvailable(userId) }
        assertEquals(SessionGraphStoreViewModel.Lifecycle.INVALIDATING, store.lifecycle(userId))
        assertThrows(SessionGraphUnavailableException::class.java) { store.graphFor(userId) }
    }

    @Test
    fun givenGraphCreationInProgress_whenLogoutInvalidatesSession_thenCreatedGraphIsDiscarded() {
        val userId = UserId("user-one", "wire.test")
        val creationStarted = CountDownLatch(1)
        val allowCreationToFinish = CountDownLatch(1)
        val loader = mockk<WireSessionImageLoader>(relaxed = true)
        val graph = mockk<AppSessionViewModelGraph>(relaxed = true) {
            every { wireSessionImageLoader } returns loader
        }
        val store = SessionGraphStoreViewModel(
            createSessionGraph = {
                creationStarted.countDown()
                check(allowCreationToFinish.await(5, TimeUnit.SECONDS))
                graph
            }
        )
        val executor = Executors.newSingleThreadExecutor()

        try {
            val resolution = executor.submit<AppSessionViewModelGraph> { store.graphFor(userId) }
            check(creationStarted.await(5, TimeUnit.SECONDS))

            store.markInvalidating(userId)
            allowCreationToFinish.countDown()

            assertThrows(Exception::class.java) { resolution.get(5, TimeUnit.SECONDS) }
            assertEquals(SessionGraphStoreViewModel.Lifecycle.INVALIDATING, store.lifecycle(userId))
            verify(exactly = 1) { loader.shutdown() }
        } finally {
            allowCreationToFinish.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun givenCreatingGraphIsReleased_whenLogoutThenInvalidates_thenTombstoneIsPreserved() {
        val userId = UserId("user-one", "wire.test")
        val creationStarted = CountDownLatch(1)
        val allowCreationToFinish = CountDownLatch(1)
        val loader = mockk<WireSessionImageLoader>(relaxed = true)
        val graph = mockk<AppSessionViewModelGraph>(relaxed = true) {
            every { wireSessionImageLoader } returns loader
        }
        val store = SessionGraphStoreViewModel(
            createSessionGraph = {
                creationStarted.countDown()
                check(allowCreationToFinish.await(5, TimeUnit.SECONDS))
                graph
            }
        )
        val executor = Executors.newSingleThreadExecutor()

        try {
            val resolution = executor.submit<AppSessionViewModelGraph> { store.graphFor(userId) }
            check(creationStarted.await(5, TimeUnit.SECONDS))

            store.release(userId)
            store.markInvalidating(userId)
            allowCreationToFinish.countDown()

            assertThrows(Exception::class.java) { resolution.get(5, TimeUnit.SECONDS) }
            assertEquals(SessionGraphStoreViewModel.Lifecycle.INVALIDATING, store.lifecycle(userId))
            assertThrows(SessionGraphUnavailableException::class.java) { store.graphFor(userId) }
            verify(exactly = 1) { loader.shutdown() }
        } finally {
            allowCreationToFinish.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun givenConcurrentResolutionForSameSession_whenGraphIsCreated_thenBothCallersShareOneGraph() {
        val userId = UserId("user-one", "wire.test")
        val creationStarted = CountDownLatch(1)
        val allowCreationToFinish = CountDownLatch(1)
        val graph = mockk<AppSessionViewModelGraph>(relaxed = true)
        var creationCount = 0
        val store = SessionGraphStoreViewModel(
            createSessionGraph = {
                creationCount++
                creationStarted.countDown()
                check(allowCreationToFinish.await(5, TimeUnit.SECONDS))
                graph
            }
        )
        val executor = Executors.newFixedThreadPool(2)

        try {
            val first = executor.submit<AppSessionViewModelGraph> { store.graphFor(userId) }
            check(creationStarted.await(5, TimeUnit.SECONDS))
            val second = executor.submit<AppSessionViewModelGraph> { store.graphFor(userId) }
            allowCreationToFinish.countDown()

            assertSame(graph, first.get(5, TimeUnit.SECONDS))
            assertSame(graph, second.get(5, TimeUnit.SECONDS))
            assertEquals(1, creationCount)
        } finally {
            allowCreationToFinish.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun givenGraphFactoryFails_whenResolvingAgain_thenCreationCanRetry() {
        val userId = UserId("user-one", "wire.test")
        val graph = mockk<AppSessionViewModelGraph>(relaxed = true)
        var creationCount = 0
        val store = SessionGraphStoreViewModel(
            createSessionGraph = {
                creationCount++
                if (creationCount == 1) error("creation failed") else graph
            }
        )

        assertThrows(IllegalStateException::class.java) { store.graphFor(userId) }

        assertSame(graph, store.graphFor(userId))
        assertEquals(2, creationCount)
        assertEquals(SessionGraphStoreViewModel.Lifecycle.ACTIVE, store.lifecycle(userId))
    }

    @Test
    fun givenSessionRemovedBeforeFirstResolution_whenStaleRouteResolves_thenGraphIsNotCreated() {
        val userId = UserId("user-one", "wire.test")
        var creationCount = 0
        val store = SessionGraphStoreViewModel(
            createSessionGraph = {
                creationCount++
                mockk()
            }
        )

        store.markRemoved(userId)

        assertThrows(SessionGraphUnavailableException::class.java) { store.graphFor(userId) }
        assertEquals(0, creationCount)
    }

    @Test
    fun givenDifferentSessionIsLoggedOut_whenResolvingExistingUser_thenGraphIsRetained() = runTest {
        val retainedUserId = UserId("user-one", "wire.test")
        val loggedOutUserId = UserId("user-two", "wire.test")
        val expectedGraph = mockk<AppSessionViewModelGraph>()
        var logoutCallback: LogoutCallback? = null
        var creationCount = 0
        val store = SessionGraphStoreViewModel(
            createSessionGraph = {
                creationCount++
                expectedGraph
            },
            registerLogoutCallback = { logoutCallback = it },
        )

        val resolvedBeforeLogout = store.graphFor(retainedUserId)
        requireNotNull(logoutCallback).invoke(loggedOutUserId, LogoutReason.SELF_HARD_LOGOUT)
        val resolvedAfterLogout = store.graphFor(retainedUserId)

        assertSame(resolvedBeforeLogout, resolvedAfterLogout)
        assertEquals(1, creationCount)
    }

    @Test
    fun givenStoreOwnerIsCleared_whenLogoutCallbackWasRegistered_thenCallbackIsUnregistered() {
        var registeredCallback: LogoutCallback? = null
        var unregisteredCallback: LogoutCallback? = null
        val factory = viewModelFactory {
            initializer {
                SessionGraphStoreViewModel(
                    createSessionGraph = { mockk() },
                    registerLogoutCallback = { registeredCallback = it },
                    unregisterLogoutCallback = { unregisteredCallback = it },
                )
            }
        }
        val viewModelStore = ViewModelStore()

        ViewModelProvider.create(viewModelStore, factory)[SessionGraphStoreViewModel::class]
        viewModelStore.clear()

        assertSame(registeredCallback, unregisteredCallback)
    }

    @Test
    fun givenStoreOwnerIsCleared_whenResolvingAgain_thenSessionGraphCacheIsRecreated() {
        val userId = UserId("user-one", "wire.test")
        val firstGraph = mockk<AppSessionViewModelGraph>(relaxed = true)
        val secondGraph = mockk<AppSessionViewModelGraph>(relaxed = true)
        val graphs = listOf(firstGraph, secondGraph).iterator()
        val factory = viewModelFactory {
            initializer {
                SessionGraphStoreViewModel(createSessionGraph = { graphs.next() })
            }
        }
        val viewModelStore = ViewModelStore()
        val firstStore = ViewModelProvider.create(viewModelStore, factory)[SessionGraphStoreViewModel::class]

        val resolvedFirstGraph = firstStore.graphFor(userId)
        viewModelStore.clear()
        val secondStore = ViewModelProvider.create(viewModelStore, factory)[SessionGraphStoreViewModel::class]
        val resolvedSecondGraph = secondStore.graphFor(userId)

        assertSame(firstGraph, resolvedFirstGraph)
        assertSame(secondGraph, resolvedSecondGraph)
    }

    @Test
    fun givenSeveralGraphs_whenStoreIsClearedAndOneShutdownFails_thenEveryGraphIsDisposedOnce() {
        val firstUser = UserId("user-one", "wire.test")
        val secondUser = UserId("user-two", "wire.test")
        val firstLoader = mockk<WireSessionImageLoader> {
            every { shutdown() } throws IllegalStateException("shutdown failed")
        }
        val secondLoader = mockk<WireSessionImageLoader>(relaxed = true)
        val firstGraph = mockk<AppSessionViewModelGraph> {
            every { wireSessionImageLoader } returns firstLoader
        }
        val secondGraph = mockk<AppSessionViewModelGraph> {
            every { wireSessionImageLoader } returns secondLoader
        }
        val graphs = listOf(firstGraph, secondGraph).iterator()
        val factory = viewModelFactory {
            initializer { SessionGraphStoreViewModel(createSessionGraph = { graphs.next() }) }
        }
        val viewModelStore = ViewModelStore()
        val store = ViewModelProvider.create(viewModelStore, factory)[SessionGraphStoreViewModel::class]
        store.graphFor(firstUser)
        store.graphFor(secondUser)

        viewModelStore.clear()

        verify(exactly = 1) { firstLoader.shutdown() }
        verify(exactly = 1) { secondLoader.shutdown() }
    }
}
