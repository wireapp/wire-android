/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */
package com.wire.android.ui.authentication.initialsync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InitialSyncViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `completion is emitted only after gateway has completed`() = runTest(dispatcher) {
        val viewModel = InitialSyncViewModel { InitialSyncGatewayResult.Completed(shouldMoveToBackground = true) }

        assertEquals(InitialSyncState.Loading, viewModel.state)
        advanceUntilIdle()
        assertEquals(InitialSyncState.Completed(shouldMoveToBackground = true), viewModel.state)
    }

    @Test
    fun `an unavailable observer exposes a retryable state and retry starts a new attempt`() = runTest(dispatcher) {
        var calls = 0
        val viewModel = InitialSyncViewModel {
            calls++
            if (calls == 1) InitialSyncGatewayResult.Unavailable else InitialSyncGatewayResult.Completed(false)
        }

        advanceUntilIdle()
        assertEquals(InitialSyncState.Unavailable, viewModel.state)
        viewModel.retry()
        assertEquals(InitialSyncState.Loading, viewModel.state)
        advanceUntilIdle()
        assertEquals(InitialSyncState.Completed(false), viewModel.state)
        assertEquals(2, calls)
    }
}
