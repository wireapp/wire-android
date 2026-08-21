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
package com.wire.android.session

import com.wire.android.ui.MIGRATION_LONG_RUNNING_MESSAGE_DELAY
import com.wire.android.ui.MIGRATION_SCREEN_MINIMUM_VISIBILITY
import com.wire.android.ui.MIGRATION_SCREEN_REVEAL_DELAY
import com.wire.android.ui.MigrationScreenPhase
import com.wire.android.ui.MigrationScreenVisibility
import com.wire.android.ui.UserSessionPreparationUiFailure
import com.wire.android.ui.UserSessionPreparationUiState
import com.wire.android.ui.migrationScreenPhase
import com.wire.android.ui.preparationScreenRevealDelay
import com.wire.android.ui.toUiFailure
import com.wire.android.ui.toUiStates
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.PrepareUserSessionResult
import com.wire.kalium.logic.UserSessionPreparationFailure
import com.wire.kalium.logic.UserSessionPreparationState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.UserSessionScope
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class UserSessionPreparationGateTest {

    @Test
    fun givenKaliumReturnsReadySession_whenPreparing_thenReturnedScopeIsReused() = runTest {
        val coreLogic = mockk<CoreLogic>()
        val sessionScope = mockk<UserSessionScope>()
        coEvery { coreLogic.prepareUserSession(USER_ID) } returns success(sessionScope)

        val result = UserSessionPreparationGate(coreLogic).prepare(USER_ID)

        assertSame(sessionScope, (result as AppUserSessionPreparationResult.Ready).sessionScope)
    }

    @Test
    fun givenConcurrentAppCallers_whenPreparing_thenBothAwaitKaliumSharedResult() = runTest {
        val coreLogic = mockk<CoreLogic>()
        val sessionScope = mockk<UserSessionScope>()
        val kaliumResult = CompletableDeferred<PrepareUserSessionResult>()
        coEvery { coreLogic.prepareUserSession(USER_ID) } coAnswers { kaliumResult.await() }
        val gate = UserSessionPreparationGate(coreLogic)

        val foreground = async { gate.prepare(USER_ID) }
        val background = async { gate.prepare(USER_ID) }
        runCurrent()
        coVerify(exactly = 2) { coreLogic.prepareUserSession(USER_ID) }

        kaliumResult.complete(success(sessionScope))

        assertSame(
            sessionScope,
            (foreground.await() as AppUserSessionPreparationResult.Ready).sessionScope,
        )
        assertSame(
            sessionScope,
            (background.await() as AppUserSessionPreparationResult.Ready).sessionScope,
        )
    }

    @Test
    fun givenPublicPreparationFailures_whenMapping_thenRetryabilityIsExplicit() {
        val failures: List<Pair<UserSessionPreparationFailure, Boolean>> = listOf(
            mockk<UserSessionPreparationFailure.InsufficientStorage>() to true,
            mockk<UserSessionPreparationFailure.TemporarilyUnavailable>() to true,
            mockk<UserSessionPreparationFailure.ApplicationUpdateRequired>() to false,
            mockk<UserSessionPreparationFailure.SupportRequired>() to false,
        )

        failures.forEach { (reason, expectedCanRetry) ->
            val result = failure(reason).toAppResult() as AppUserSessionPreparationResult.Failed
            assertEquals(reason, result.reason)
            if (expectedCanRetry) assertTrue(result.canRetry) else assertFalse(result.canRetry)
        }
    }

    @Test
    fun givenPublicPreparationFailures_whenMappingForForeground_thenEveryActionableStateIsPreserved() {
        val mappings: List<Pair<UserSessionPreparationFailure, UserSessionPreparationUiFailure>> = listOf(
            mockk<UserSessionPreparationFailure.InsufficientStorage>() to
                    UserSessionPreparationUiFailure.InsufficientStorage,
            mockk<UserSessionPreparationFailure.TemporarilyUnavailable>() to
                    UserSessionPreparationUiFailure.TemporarilyUnavailable,
            mockk<UserSessionPreparationFailure.ApplicationUpdateRequired>() to
                    UserSessionPreparationUiFailure.ApplicationUpdateRequired,
            mockk<UserSessionPreparationFailure.SupportRequired>() to UserSessionPreparationUiFailure.SupportRequired,
        )

        mappings.forEach { (failure, expected) ->
            assertEquals(expected, failure.toUiFailure())
        }
    }

    @Test
    fun givenFastPreparationStates_whenChoosingVisibility_thenPreparationScreenStaysBehindSystemSplash() {
        val hiddenStates = listOf(
            UserSessionPreparationUiState.ResolvingSession,
            UserSessionPreparationUiState.OpeningDatabase,
            UserSessionPreparationUiState.Ready,
        )

        hiddenStates.forEach { state ->
            assertEquals(null, state.preparationScreenRevealDelay())
        }
    }

    @Test
    fun givenMigrationState_whenChoosingVisibility_thenPreparationScreenIsDebounced() {
        assertEquals(
            MIGRATION_SCREEN_REVEAL_DELAY,
            UserSessionPreparationUiState.MigratingDatabase.preparationScreenRevealDelay(),
        )
    }

    @Test
    fun givenFailureState_whenChoosingVisibility_thenPreparationScreenIsRevealedImmediately() {
        val state = UserSessionPreparationUiState.Failed(UserSessionPreparationUiFailure.SupportRequired)

        assertEquals(Duration.ZERO, state.preparationScreenRevealDelay())
    }

    @Test
    fun givenVisibleMigration_whenChoosingCopy_thenLongRunningMessageStartsAtItsDelay() {
        assertEquals(MigrationScreenPhase.Updating, migrationScreenPhase(Duration.ZERO))
        assertEquals(
            MigrationScreenPhase.Updating,
            migrationScreenPhase(MIGRATION_LONG_RUNNING_MESSAGE_DELAY - 1.milliseconds),
        )
        assertEquals(
            MigrationScreenPhase.StillUpdating,
            migrationScreenPhase(MIGRATION_LONG_RUNNING_MESSAGE_DELAY),
        )
    }

    /** Preserves a short migration state after observation starts while the main collector is busy. */
    @Test
    fun givenStatesChangeWhileTheCollectorIsBusy_whenMappingForForeground_thenMigrationIsStillDelivered() = runTest {
        val states = MutableStateFlow<UserSessionPreparationState>(UserSessionPreparationState.NotStarted)
        val observed = mutableListOf<UserSessionPreparationUiState>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
            states.toUiStates().collect { state ->
                observed += state
                delay(BUSY_COLLECTOR_DELAY) // stands in for a main thread stuck on the first frame
            }
        }
        runCurrent()

        states.value = UserSessionPreparationState.OpeningDatabase
        states.value = UserSessionPreparationState.MigratingDatabase
        states.value = UserSessionPreparationState.Ready
        advanceTimeBy(BUSY_COLLECTOR_DELAY * OBSERVED_STATE_COUNT)
        collector.cancel()

        assertEquals(
            listOf(
                UserSessionPreparationUiState.ResolvingSession,
                UserSessionPreparationUiState.OpeningDatabase,
                UserSessionPreparationUiState.MigratingDatabase,
                UserSessionPreparationUiState.Ready,
            ),
            observed,
        )
    }

    @Test
    fun givenMigrationScreenWasNeverRevealed_whenLeavingPreparation_thenNothingIsWaitedFor() {
        val visibility = MigrationScreenVisibility(elapsedRealtimeMillis = { 0L })

        assertEquals(Duration.ZERO, visibility.remainingVisibility())
    }

    @Test
    fun givenMigrationFinishedRightAfterReveal_whenLeavingPreparation_thenRemainderOfMinimumIsWaitedFor() {
        var now = 1_000L
        val visibility = MigrationScreenVisibility(elapsedRealtimeMillis = { now })

        visibility.onRevealed()
        now += 200L

        assertEquals(MIGRATION_SCREEN_MINIMUM_VISIBILITY - 200.milliseconds, visibility.remainingVisibility())
    }

    @Test
    fun givenMigrationOutlivedTheMinimum_whenLeavingPreparation_thenNothingIsWaitedFor() {
        var now = 1_000L
        val visibility = MigrationScreenVisibility(elapsedRealtimeMillis = { now })

        visibility.onRevealed()
        now += MIGRATION_SCREEN_MINIMUM_VISIBILITY.inWholeMilliseconds + 1L

        assertEquals(Duration.ZERO, visibility.remainingVisibility())
    }

    @Test
    fun givenScreenAlreadyRevealed_whenRevealedAgain_thenMinimumStillCountsFromFirstReveal() {
        var now = 1_000L
        val visibility = MigrationScreenVisibility(elapsedRealtimeMillis = { now })

        visibility.onRevealed()
        now += 400L
        visibility.onRevealed()

        assertEquals(MIGRATION_SCREEN_MINIMUM_VISIBILITY - 400.milliseconds, visibility.remainingVisibility())
    }

    private fun success(sessionScope: UserSessionScope): PrepareUserSessionResult.Success =
        mockk<PrepareUserSessionResult.Success>().also {
            every { it.sessionScope } returns sessionScope
        }

    private fun failure(reason: UserSessionPreparationFailure): PrepareUserSessionResult.Failure =
        mockk<PrepareUserSessionResult.Failure>().also {
            every { it.failure } returns reason
        }

    private companion object {
        val USER_ID = UserId("user", "wire.test")
        const val BUSY_COLLECTOR_DELAY = 1_000L
        const val OBSERVED_STATE_COUNT = 4
    }
}
