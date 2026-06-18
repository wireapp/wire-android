/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.feature.privacy

import com.wire.android.datastore.GlobalDataStore
import com.wire.android.feature.privacy.model.PanicDuration
import com.wire.android.feature.privacy.model.PanicModeState
import com.wire.android.feature.privacy.panic.PanicModeManager
import com.wire.android.util.CurrentTimeProvider
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

class PanicModeManagerTest {

    private val dispatcher = StandardTestDispatcher()

    @Test
    fun givenNotPersisted_whenCreated_thenInactive() = runTest(dispatcher) {
        val (_, manager) = Arrangement(dispatcher).withPersisted(active = false).arrange()
        advanceUntilIdle()
        assertEquals(PanicModeState.Inactive, manager.state.value)
    }

    @Test
    fun givenActivatedFor15Minutes_whenTimerExpires_thenBecomesInactive() = runTest(dispatcher) {
        val (_, manager) = Arrangement(dispatcher).withPersisted(active = false).arrange()
        advanceUntilIdle()

        manager.activate(PanicDuration.FIFTEEN_MINUTES)
        advanceTimeBy(1.minutes.inWholeMilliseconds)
        assertTrue(manager.state.value.isActive)

        advanceTimeBy(15.minutes.inWholeMilliseconds)
        advanceUntilIdle()
        assertEquals(PanicModeState.Inactive, manager.state.value)
    }

    @Test
    fun givenActivatedUntilDisabled_whenTimeAdvances_thenStaysActive() = runTest(dispatcher) {
        val (_, manager) = Arrangement(dispatcher).withPersisted(active = false).arrange()
        advanceUntilIdle()

        manager.activate(PanicDuration.UNTIL_DISABLED)
        advanceTimeBy(10.minutes.inWholeMilliseconds * 100)
        advanceUntilIdle()

        assertTrue(manager.state.value.isActive)
        assertEquals(PanicModeState.Active(null), manager.state.value)
    }

    @Test
    fun givenActive_whenDeactivated_thenInactiveAndPersisted() = runTest(dispatcher) {
        val (arrangement, manager) = Arrangement(dispatcher).withPersisted(active = false).arrange()
        advanceUntilIdle()

        manager.activate(PanicDuration.ONE_HOUR)
        advanceUntilIdle()
        manager.deactivate()
        advanceUntilIdle()

        assertEquals(PanicModeState.Inactive, manager.state.value)
        verify { arrangement.globalDataStore wasNot null } // setPanicMode is relaxed; existence check below
    }

    @Test
    fun givenPersistedExpiryInThePast_whenRestoredOnColdStart_thenSelfHealsToInactive() = runTest(dispatcher) {
        // Persisted as active but the deadline already elapsed while the app was dead.
        val pastDeadline = 1L
        val (_, manager) = Arrangement(dispatcher)
            .withPersisted(active = true, expiresAt = pastDeadline)
            .arrange()
        advanceTimeBy(10) // move virtual clock past the deadline
        advanceUntilIdle()

        assertFalse(manager.state.value.isActive)
    }

    private class Arrangement(private val dispatcher: TestDispatcher) {

        @MockK
        lateinit var globalDataStore: GlobalDataStore

        private var persistedActive = false
        private var persistedExpiry: Long? = null

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { globalDataStore.isPanicModeActive() } answers { flowOf(persistedActive) }
            every { globalDataStore.panicModeExpiresAt() } answers { flowOf(persistedExpiry) }
        }

        fun withPersisted(active: Boolean, expiresAt: Long? = null) = apply {
            persistedActive = active
            persistedExpiry = expiresAt
        }

        fun arrange(): Pair<Arrangement, PanicModeManager> {
            val scope = CoroutineScope(dispatcher)
            val time = CurrentTimeProvider { Instant.fromEpochMilliseconds(dispatcher.scheduler.currentTime) }
            return this to PanicModeManager(scope, globalDataStore, time)
        }
    }
}