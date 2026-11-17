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

package com.wire.android.ui.home.settings.mfa.overview

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wire.android.config.CoroutineTestExtension
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.common.functional.Either
import com.wire.kalium.logic.feature.mfa.AuthenticatorApp
import com.wire.kalium.logic.feature.mfa.MFAOverview
import com.wire.kalium.logic.feature.mfa.ObserveMFAOverviewUseCase
import com.wire.kalium.logic.feature.mfa.RecoveryCodesStatus
import com.wire.kalium.logic.feature.mfa.RefreshMFAOverviewUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class MFAOverviewViewModelTest {
    private val owner = ViewModelStore()
    private val enabled = MFAOverview.Enabled(
        listOf(AuthenticatorApp("phone", "Android phone", LocalDate(2026, 10, 14))),
        RecoveryCodesStatus(10, 10),
    )

    @AfterEach
    fun tearDown() {
        owner.clear()
    }

    @Test
    fun `no cached data remains loading until Kalium publishes an overview`() = runTest {
        val overview = MutableStateFlow<MFAOverview?>(null)
        val response = CompletableDeferred<Unit>()
        val viewModel = createViewModel(overview) {
            response.await()
            overview.value = MFAOverview.Disabled
            Either.Right(Unit)
        }
        runCurrent()
        assertEquals(MFAOverviewState.Loading, viewModel.state.value)
        response.complete(Unit)
        runCurrent()
        assertEquals(MFAOverviewState.Loaded(MFAOverview.Disabled), viewModel.state.value)
    }

    @Test
    fun `initial refresh failure is replaced with content after retry succeeds`() = runTest {
        val overview = MutableStateFlow<MFAOverview?>(null)
        var fail = true
        val viewModel = createViewModel(overview) {
            if (fail) {
                Either.Left(CoreFailure.Unknown(null))
            } else {
                overview.value = enabled
                Either.Right(Unit)
            }
        }
        runCurrent()
        assertEquals(MFAOverviewState.Error, viewModel.state.value)
        fail = false
        viewModel.refresh()
        runCurrent()
        assertEquals(MFAOverviewState.Loaded(enabled), viewModel.state.value)
    }

    @Test
    fun `Kalium mutations update existing and reopened screens without navigation callbacks`() = runTest {
        val overview = MutableStateFlow<MFAOverview?>(enabled)
        val viewModel = createViewModel(overview)
        runCurrent()
        val renamed = enabled.copy(authenticatorApps = enabled.authenticatorApps.map { it.copy(name = "New name") })
        overview.value = renamed
        runCurrent()
        assertEquals(MFAOverviewState.Loaded(renamed), viewModel.state.value)
        owner.clear()
        val reopened = createViewModel(overview)
        runCurrent()
        assertEquals(MFAOverviewState.Loaded(renamed), reopened.state.value)
        overview.value = MFAOverview.Disabled
        runCurrent()
        assertEquals(MFAOverviewState.Loaded(MFAOverview.Disabled), reopened.state.value)
    }

    @Test
    fun `cached content remains visible while refreshing and after a failure`() = runTest {
        val overview = MutableStateFlow<MFAOverview?>(enabled)
        val response = CompletableDeferred<Either<CoreFailure, Unit>>()
        val viewModel = createViewModel(overview) { response.await() }
        runCurrent()
        assertEquals(MFAOverviewState.Loaded(enabled, isRefreshing = true), viewModel.state.value)
        response.complete(Either.Left(CoreFailure.Unknown(null)))
        runCurrent()
        assertEquals(MFAOverviewState.Loaded(enabled, hasRefreshError = true), viewModel.state.value)
    }

    @Test
    fun `clearing the flow owner cancels refresh without publishing a presentation error`() = runTest {
        var cancelled = false
        val response = CompletableDeferred<Unit>()
        val viewModel = createViewModel(MutableStateFlow(enabled)) {
            try {
                response.await()
                Either.Right(Unit)
            } finally {
                cancelled = true
            }
        }
        runCurrent()
        owner.clear()
        runCurrent()
        assertTrue(cancelled)
        assertFalse((viewModel.state.value as MFAOverviewState.Loaded).hasRefreshError)
    }

    private fun createViewModel(
        overview: MutableStateFlow<MFAOverview?>,
        refresh: suspend () -> Either<CoreFailure, Unit> = { Either.Right(Unit) },
    ): MFAOverviewViewModel {
        val observe = mockk<ObserveMFAOverviewUseCase>()
        val refreshUseCase = mockk<RefreshMFAOverviewUseCase>()
        every { observe() } returns overview
        coEvery { refreshUseCase() } coAnswers { refresh() }
        return ViewModelProvider.create(
            owner,
            viewModelFactory { initializer { MFAOverviewViewModel(observe, refreshUseCase) } },
        )[MFAOverviewViewModel::class]
    }
}
