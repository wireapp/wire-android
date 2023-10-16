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
package com.wire.android.feature

import com.wire.android.datastore.GlobalDataStore
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ObserveAppLockConfigUseCaseTest {

    // TODO: include checking if any logged account does not enforce app-lock
    @Test
    fun givenPasscodeIsSet_whenObservingAppLockConfig_thenReturnEnabled() = runTest {
        val (_, useCase) = Arrangement()
            .withAppLockPasscodeSet(true)
            .arrange()

        val result = useCase.invoke().firstOrNull()

        assert(result is AppLockConfig.Enabled)
    }

    @Test
    fun givenPasscodeIsNotSet_whenObservingAppLockConfig_thenReturnDisabled() = runTest {
        val (_, useCase) = Arrangement()
            .withAppLockPasscodeSet(false)
            .arrange()

        val result = useCase.invoke().firstOrNull()

        assert(result is AppLockConfig.Disabled)
    }

    inner class Arrangement {
        @MockK
        lateinit var globalDataStore: GlobalDataStore
        val useCase by lazy { ObserveAppLockConfigUseCase(globalDataStore) }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        fun withAppLockPasscodeSet(value: Boolean) = apply {
            every { globalDataStore.isAppLockPasscodeSetFlow() } returns flowOf(value)
        }

        fun arrange() = this to useCase
    }
}
