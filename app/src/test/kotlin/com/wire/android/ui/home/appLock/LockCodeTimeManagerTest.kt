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

import com.wire.android.datastore.GlobalDataStore
import com.wire.android.feature.AppLockConfig
import com.wire.android.feature.ObserveAppLockConfigUseCase
import com.wire.android.util.CurrentScreenManager
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test

class LockCodeTimeManagerTest {

    private val dispatcher = StandardTestDispatcher()

    private fun testStopAndStart(appLockConfig: AppLockConfig, delay: Long, expected: Boolean) =
        runTest(dispatcher) {
            val (arrangement, manager) = Arrangement(dispatcher)
                .withAppLockConfig(appLockConfig)
                .withIsAppVisible(true)
                .arrange()
            advanceUntilIdle()
            arrangement.withIsAppVisible(false)
            advanceTimeBy(delay)
            arrangement.withIsAppVisible(true)
            advanceUntilIdle()
            val result = manager.shouldLock().first()
            assertEquals(expected, result)
        }

    private fun AppLockConfig.timeoutInMillis(): Long = this.timeout.inWholeMilliseconds

    @Test
    fun givenLockEnabledAndAppOpen_whenAppClosedAndOpenedAgainBeforeLockTimeout_thenDoNotRequirePasscode() =
        testStopAndStart(AppLockConfig.Enabled, AppLockConfig.Enabled.timeoutInMillis() - 100L, false)

    @Test
    fun givenLockEnabledAndAppOpen_whenAppClosedAndOpenedAgainAfterLockTimeout_thenRequirePasscode() =
        testStopAndStart(AppLockConfig.Enabled, AppLockConfig.Enabled.timeoutInMillis() + 100L, true)

    @Test
    fun givenLockDisabledAndAppOpen_whenAppClosedAndOpenedAgainBeforeLockTimeout_thenDoNotRequirePasscode() =
        testStopAndStart(AppLockConfig.Disabled, AppLockConfig.Disabled.timeoutInMillis() - 100L, false)

    @Test
    fun givenLockDisabledAndAppOpen_whenAppClosedAndOpenedAgainAfterLockTimeout_thenDoNotRequirePasscode() =
        testStopAndStart(AppLockConfig.Disabled, AppLockConfig.Disabled.timeoutInMillis() + 100L, false)

    private fun testStart(appLockConfig: AppLockConfig, withInitialTimestamp: Boolean, delay: Long, expected: Boolean) =
        runTest(dispatcher) {
            val (arrangement, manager) = Arrangement(dispatcher)
                .withInitialAppLockTimestamp(if (withInitialTimestamp) dispatcher.scheduler.currentTime else -1)
                .withAppLockConfig(appLockConfig)
                .withIsAppVisible(false)
                .arrange()
            advanceUntilIdle()
            advanceTimeBy(delay)
            arrangement.withIsAppVisible(true)
            advanceUntilIdle()
            val result = manager.shouldLock().first()
            assertEquals(expected, result)
        }

    @Test
    fun givenLockEnabledAndNoInitialTimestamp_whenAppOpenedBeforeLockTimeout_thenDoNotRequirePasscode() =
        testStart(AppLockConfig.Enabled, false, AppLockConfig.Enabled.timeoutInMillis() - 100, false)

    @Test
    fun givenLockEnabledAndNoInitialTimestamp_whenAppOpenedAfterLockTimeout_thenDoNotRequirePasscode() =
        testStart(AppLockConfig.Enabled, false, AppLockConfig.Enabled.timeoutInMillis() + 100, false)

    @Test
    fun givenLockEnabledAndInitialTimestamp_whenAppOpenedBeforeLockTimeout_thenDoNotRequirePasscode() =
        testStart(AppLockConfig.Enabled, true, AppLockConfig.Enabled.timeoutInMillis() - 100, false)

    @Test
    fun givenLockEnabledAndInitialTimestamp_whenAppOpenedAfterLockTimeout_thenRequirePasscode() =
        testStart(AppLockConfig.Enabled, true, AppLockConfig.Enabled.timeoutInMillis() + 100, true)

    @Test
    fun givenLockDisabledAndNoInitialTimestamp_whenAppOpenedBeforeLockTimeout_thenDoNotRequirePasscode() =
        testStart(AppLockConfig.Disabled, false, AppLockConfig.Disabled.timeoutInMillis() - 100, false)

    @Test
    fun givenLockDisabledAndNoInitialTimestamp_whenAppOpenedAfterLockTimeout_thenDoNotRequirePasscode() =
        testStart(AppLockConfig.Disabled, false, AppLockConfig.Disabled.timeoutInMillis() + 100, false)

    @Test
    fun givenLockDisabledAndInitialTimestamp_whenAppOpenedBeforeLockTimeout_thenDoNotRequirePasscode() =
        testStart(AppLockConfig.Disabled, true, AppLockConfig.Disabled.timeoutInMillis() - 100, false)

    @Test
    fun givenLockDisabledAndInitialTimestamp_whenAppOpenedAfterLockTimeout_thenDoNotRequirePasscode() =
        testStart(AppLockConfig.Disabled, true, AppLockConfig.Disabled.timeoutInMillis() + 100, false)

    class Arrangement(dispatcher: TestDispatcher) {

        @MockK
        private lateinit var currentScreenManager: CurrentScreenManager

        @MockK
        private lateinit var observeAppLockConfigUseCase: ObserveAppLockConfigUseCase

        @MockK
        private lateinit var globalDataStore: GlobalDataStore

        private val lockCodeTimeManager by lazy {
            LockCodeTimeManager(
                CoroutineScope(dispatcher),
                currentScreenManager,
                observeAppLockConfigUseCase,
                globalDataStore,
                dispatcher.scheduler::currentTime
            )
        }

        private val isAppVisibleStateFlow = MutableStateFlow(false)
        private val appLockConfigStateFlow = MutableStateFlow<AppLockConfig>(AppLockConfig.Disabled)

        fun arrange() = this to lockCodeTimeManager

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            withInitialAppLockTimestamp(-1L)
            coEvery { globalDataStore.setAppLockTimestamp(any()) } returns Unit
        }

        fun withInitialAppLockTimestamp(value: Long = -1L): Arrangement = apply {
            every { globalDataStore.getAppLockTimestampFlow() } returns flowOf(value)
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
