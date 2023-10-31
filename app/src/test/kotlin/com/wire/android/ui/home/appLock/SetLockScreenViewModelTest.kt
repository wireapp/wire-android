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

import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.applock.passcode.setAppLockPasscode
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.feature.AppLockConfig
import com.wire.android.feature.ObserveAppLockConfigUseCase
import com.wire.kalium.logic.feature.applock.AppLockTeamFeatureConfigObserverImpl
import com.wire.kalium.logic.feature.auth.ValidatePasswordResult
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
class SetLockScreenViewModelTest {

    @Test
    fun `given new password input, when valid,then should update state`() {
        val (arrangement, viewModel) = Arrangement()
            .withValidPassword()
            .arrange()

        viewModel.onPasswordChanged(TextFieldValue("password"))

        assert(viewModel.state.password.text == "password")
        assert(viewModel.state.passwordValidation.isValid)

        verify(exactly = 1) { arrangement.validatePassword("password") }
    }

    @Test
    fun `given new password input, when invalid,then should update state`() {
        val (arrangement, viewModel) = Arrangement()
            .withInvalidPassword()
            .arrange()

        viewModel.onPasswordChanged(TextFieldValue("password"))

        assert(viewModel.state.password.text == "password")
        assert(!viewModel.state.passwordValidation.isValid)

        verify(exactly = 1) { arrangement.validatePassword("password") }
    }

    private class Arrangement {
        @MockK
        lateinit var validatePassword: ValidatePasswordUseCase
        @MockK
        lateinit var globalDataStore: GlobalDataStore
        @MockK
        private lateinit var observeAppLockConfigUseCase: ObserveAppLockConfigUseCase

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            coEvery { globalDataStore.setAppLockPasscode(any()) } returns Unit
            coEvery { observeAppLockConfigUseCase() } returns flowOf(
                AppLockConfig.Disabled(AppLockTeamFeatureConfigObserverImpl.DEFAULT_TIMEOUT)
            )
        }

        fun withValidPassword() = apply {
            every { validatePassword(any()) } returns ValidatePasswordResult.Valid
        }

        fun withInvalidPassword() = apply {
            every { validatePassword(any()) } returns ValidatePasswordResult.Invalid()
        }

        private val viewModel = SetLockScreenViewModel(
            validatePassword,
            globalDataStore,
            TestDispatcherProvider(),
            observeAppLockConfigUseCase
        )

        fun arrange() = this to viewModel
    }
}
