/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
package com.wire.android.ui.home.appLock

import app.cash.turbine.test
import com.wire.android.feature.AppLockConfig
import com.wire.android.feature.ObserveAppLockConfigUseCase
import com.wire.android.util.CurrentScreenManager
import com.wire.kalium.logic.feature.applock.AppLockTeamFeatureConfigObserverImpl.Companion.DEFAULT_TIMEOUT
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

class LockCodeTimeManagerTest {

    private val dispatcher = StandardTestDispatcher()

    private fun AppLockConfig.timeoutInMillis(): Long = this.timeout.inWholeMilliseconds

    private fun testInitialStart(appLockConfig: AppLockConfig, expected: Boolean) =
        runTest(dispatcher) {
            // given
            val (arrangement, manager) = Arrangement(dispatcher)
                .withAppLockConfig(appLockConfig)
                .withIsAppVisible(false)
                .arrange()
            advanceUntilIdle()
            // when
            arrangement.withIsAppVisible(true)
            advanceUntilIdle()
            // then
            assertEquals(expected, manager.observeAppLock().first())
        }

    @Test
    fun givenLockEnabled_whenAppInitiallyOpened_thenLocked() =
        testInitialStart(AppLockConfig.Enabled(DEFAULT_TIMEOUT), true)

    @Test
    fun givenLockDisabled_whenAppInitiallyOpened_thenNotLocked() =
        testInitialStart(AppLockConfig.Disabled(DEFAULT_TIMEOUT), false)

    private fun testStop(appLockConfig: AppLockConfig, delayAfterStop: Long, expected: Boolean) =
        runTest(dispatcher) {
            // given
            val (arrangement, manager) = Arrangement(dispatcher)
                .withAppLockConfig(appLockConfig)
                .withIsAppVisible(true)
                .arrange()
            manager.appUnlocked()
            advanceUntilIdle()
            // when
            arrangement.withIsAppVisible(false)
            advanceTimeBy(delayAfterStop)
            // then
            assertEquals(expected, manager.observeAppLock().first())
        }

    @Test
    fun givenLockEnabledAndAppOpenedUnlocked_whenAppClosedAndWaitedMoreThanTimeout_thenLocked() =
        testStop(
            AppLockConfig.Enabled(DEFAULT_TIMEOUT),
            AppLockConfig.Enabled(DEFAULT_TIMEOUT).timeoutInMillis() + 100L,
            true
        )

    @Test
    fun givenLockEnabledAndAppOpenedUnlocked_whenAppClosedAndWaitedLessThanTimeout_thenNotLocked() =
        testStop(
            AppLockConfig.Enabled(DEFAULT_TIMEOUT),
            AppLockConfig.Enabled(DEFAULT_TIMEOUT).timeoutInMillis() - 100L,
            false
        )

    @Test
    fun givenLockDisabledAndAppOpenedUnlocked_whenAppClosedAndWaitedMoreThanTimeout_thenNotLocked() =
        testStop(
            AppLockConfig.Disabled(DEFAULT_TIMEOUT),
            AppLockConfig.Disabled(DEFAULT_TIMEOUT).timeoutInMillis() + 100L,
            false
        )

    @Test
    fun givenLockDisabledAndAppOpenedUnlocked_whenAppClosedAndWaitedLessThanTimeout_thenNotLocked() =
        testStop(
            AppLockConfig.Disabled(DEFAULT_TIMEOUT),
            AppLockConfig.Disabled(DEFAULT_TIMEOUT).timeoutInMillis() - 100L,
            false
        )

    @Test
    fun givenLockEnabledAndAppOpenedUnlocked_whenAppClosedAnd_thenAfterTimeoutShouldChangeFromNotLockedToLocked() =
        runTest(dispatcher) {
            // given
            val (arrangement, manager) = Arrangement(dispatcher)
                .withAppLockConfig(AppLockConfig.Enabled(timeout = 1000.seconds))
                .withIsAppVisible(true)
                .arrange()
            manager.appUnlocked()
            advanceUntilIdle()
            // when-then
            manager.observeAppLock().test {
                arrangement.withIsAppVisible(false)
                assertEquals(false, awaitItem())
                assertEquals(true, awaitItem())
            }
        }

    private fun testStopAndStart(
        appLockConfig: AppLockConfig,
        startDelay: Long,
        expected: Boolean
    ) = runTest(dispatcher) {
        // given
        val (arrangement, manager) = Arrangement(dispatcher)
            .withAppLockConfig(appLockConfig)
            .withIsAppVisible(true)
            .arrange()
        manager.appUnlocked()
        advanceUntilIdle()
        // when
        arrangement.withIsAppVisible(false)
        advanceTimeBy(startDelay)
        arrangement.withIsAppVisible(true)
        // then
        assertEquals(expected, manager.observeAppLock().first())
    }

    @Test
    fun givenLockEnabledAndAppOpenedUnlocked_whenAppClosedAndOpenedAgainBeforeLockTimeout_thenNotLocked() =
        testStopAndStart(
            AppLockConfig.Enabled(DEFAULT_TIMEOUT),
            AppLockConfig.Enabled(DEFAULT_TIMEOUT).timeoutInMillis() - 100L,
            false
        )

    @Test
    fun givenLockEnabledAndAppOpenedUnlocked_whenAppClosedAndOpenedAgainAfterLockTimeout_thenLocked() =
        testStopAndStart(
            AppLockConfig.Enabled(DEFAULT_TIMEOUT),
            AppLockConfig.Enabled(DEFAULT_TIMEOUT).timeoutInMillis() + 100L,
            true
        )

    @Test
    fun givenLockDisabledAndAppOpenedUnlocked_whenAppClosedAndOpenedAgainBeforeLockTimeout_thenNotLocked() =
        testStopAndStart(
            AppLockConfig.Disabled(DEFAULT_TIMEOUT),
            AppLockConfig.Disabled(DEFAULT_TIMEOUT).timeoutInMillis() - 100L,
            false
        )

    @Test
    fun givenLockDisabledAndAppOpenedUnlocked_whenAppClosedAndOpenedAgainAfterLockTimeout_thenNotLocked() =
        testStopAndStart(
            AppLockConfig.Disabled(DEFAULT_TIMEOUT),
            AppLockConfig.Disabled(DEFAULT_TIMEOUT).timeoutInMillis() + 100L,
            false
        )

    @Test
    fun givenLockEnabledAndAppOpenedLocked_whenAppClosedAndOpenedBeforeLockTimeout_thenShouldStillBeLocked() =
        runTest(dispatcher) {
            // given
            val (arrangement, manager) = Arrangement(dispatcher)
                .withAppLockConfig(AppLockConfig.Enabled(DEFAULT_TIMEOUT))
                .withIsAppVisible(false)
                .arrange()
            advanceUntilIdle()
            // when
            advanceTimeBy(AppLockConfig.Enabled(DEFAULT_TIMEOUT).timeoutInMillis() - 100L)
            arrangement.withIsAppVisible(true)
            // then
            assertEquals(true, manager.observeAppLock().first())
        }

    @Test
    fun givenLockEnabledAndAppOpenedLocked_whenAppIsUnlocked_thenNotLocked() = runTest(dispatcher) {
        // given
        val (arrangement, manager) = Arrangement(dispatcher)
            .withAppLockConfig(AppLockConfig.Enabled(DEFAULT_TIMEOUT))
            .withIsAppVisible(true)
            .arrange()
        advanceUntilIdle()
        // when
        manager.appUnlocked()
        advanceUntilIdle()
        // then
        assertEquals(false, manager.observeAppLock().first())
    }

    class Arrangement(dispatcher: TestDispatcher) {

        @MockK
        private lateinit var currentScreenManager: CurrentScreenManager

        @MockK
        private lateinit var observeAppLockConfigUseCase: ObserveAppLockConfigUseCase

        private val lockCodeTimeManager by lazy {
            LockCodeTimeManager(
                CoroutineScope(dispatcher),
                currentScreenManager,
                observeAppLockConfigUseCase,
            )
        }

        private val isAppVisibleStateFlow = MutableStateFlow(false)
        private val appLockConfigStateFlow =
            MutableStateFlow<AppLockConfig>(AppLockConfig.Disabled(DEFAULT_TIMEOUT))

        fun arrange() = this to lockCodeTimeManager

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        fun withIsAppVisible(value: Boolean): Arrangement = apply {
            isAppVisibleStateFlow.value = value
            every { currentScreenManager.isAppVisibleFlow() } returns isAppVisibleStateFlow
        }

        fun withAppLockConfig(value: AppLockConfig): Arrangement = apply {
            appLockConfigStateFlow.value = value
            every { observeAppLockConfigUseCase() } returns appLockConfigStateFlow
        }
    }
}
