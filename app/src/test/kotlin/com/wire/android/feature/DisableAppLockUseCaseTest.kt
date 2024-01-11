/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package com.wire.android.feature

import com.wire.android.datastore.GlobalDataStore
import com.wire.kalium.logic.feature.featureConfig.ObserveIsAppLockEditableUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class DisableAppLockUseCaseTest {

    @Test
    fun `given app lock is editable when disable app lock is called then clear app lock passcode`() = runTest {
        val (arrangement, useCase) = Arrangement()
            .withAppLockEditable(true)
            .withClearAppLockPasscode()
            .arrange()

        useCase()

        coVerify(exactly = 1) { arrangement.dataStore.clearAppLockPasscode() }
    }

    @Test
    fun `given app lock is not editable when disable app lock is called then do not clear app lock passcode`() = runTest {
        val (arrangement, useCase) = Arrangement()
            .withAppLockEditable(false)
            .arrange()

        useCase()

        coVerify(exactly = 0) { arrangement.dataStore.clearAppLockPasscode() }
    }
    private class Arrangement {

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        @MockK
        lateinit var dataStore: GlobalDataStore

        @MockK
        lateinit var observeIsAppLockEditableUseCase: ObserveIsAppLockEditableUseCase

        private val useCase = DisableAppLockUseCase(
            dataStore,
            observeIsAppLockEditableUseCase
        )

        fun withAppLockEditable(result: Boolean) = apply {
            coEvery { observeIsAppLockEditableUseCase() } returns flowOf(result)
        }

        fun withClearAppLockPasscode() = apply {
            coEvery { dataStore.clearAppLockPasscode() } returns Unit
        }

        fun arrange() = Pair(this, useCase)
    }
}
