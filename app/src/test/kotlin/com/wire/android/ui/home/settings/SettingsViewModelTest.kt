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

package com.wire.android.ui.home.settings

import app.cash.turbine.test
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.datastore.GlobalDataStore
import com.wire.kalium.logic.feature.featureConfig.ObserveIsAppLockEditableUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
class SettingsViewModelTest {

    @Test
    fun givenSupportEmail_whenReportBugClicked_thenShareWithRecipient() = runTest {
        val (_, viewModel) = Arrangement().arrange()

        viewModel.reportBugClickAction.test {
            viewModel.onReportBugClicked("support@example.com")

            assertEquals(ReportBugClickAction.Share("support@example.com"), awaitItem())
        }
    }

    @Test
    fun givenMissingSupportEmail_whenReportBugClicked_thenConfirmSharingWithoutRecipient() = runTest {
        val (_, viewModel) = Arrangement().arrange()

        viewModel.reportBugClickAction.test {
            viewModel.onReportBugClicked("")

            assertEquals(ReportBugClickAction.ConfirmSharingWithoutRecipient, awaitItem())
        }
    }

    @Test
    fun givenBlankSupportEmail_whenReportBugClicked_thenConfirmSharingWithoutRecipient() = runTest {
        val (_, viewModel) = Arrangement().arrange()

        viewModel.reportBugClickAction.test {
            viewModel.onReportBugClicked("  ")

            assertEquals(ReportBugClickAction.ConfirmSharingWithoutRecipient, awaitItem())
        }
    }

    private class Arrangement {
        @MockK
        lateinit var globalDataStore: GlobalDataStore

        @MockK
        lateinit var observeIsAppLockEditable: ObserveIsAppLockEditableUseCase

        @MockK
        lateinit var getSelf: ObserveSelfUserUseCase

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { globalDataStore.isAppLockPasscodeSetFlow() } returns flowOf(false)
            coEvery { observeIsAppLockEditable() } returns flowOf(false)
            coEvery { getSelf() } returns emptyFlow()
            coEvery { globalDataStore.getBackendSupportEmail(any()) } returns null
        }

        fun arrange() = this to SettingsViewModel(
            globalDataStore = globalDataStore,
            observeIsAppLockEditable = observeIsAppLockEditable,
            getSelf = getSelf,
            dispatchers = TestDispatcherProvider()
        )
    }
}
