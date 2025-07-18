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
package com.wire.android.ui.home.appLock.set

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.SnapshotExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.feature.AppLockConfig
import com.wire.android.feature.ObserveAppLockConfigUseCase
import com.wire.kalium.logic.feature.applock.MarkTeamAppLockStatusAsNotifiedUseCase
import com.wire.kalium.logic.feature.auth.ValidatePasswordResult
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import com.wire.kalium.logic.feature.featureConfig.ObserveIsAppLockEditableUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class, SnapshotExtension::class)
class SetLockScreenViewModelTest {

    @Test
    fun `given new password input, when valid,then should update state`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withValidPassword()
            .arrange()

        viewModel.passwordTextState.setTextAndPlaceCursorAtEnd("password")

        assertEquals("password", viewModel.passwordTextState.text.toString())
        assertEquals(true, viewModel.state.passwordValidation.isValid)

        verify(exactly = 1) { arrangement.validatePassword("password") }
    }

    @Test
    fun `given new password input, when invalid,then should update state`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withInvalidPassword()
            .arrange()

        viewModel.passwordTextState.setTextAndPlaceCursorAtEnd("password")

        assertEquals("password", viewModel.passwordTextState.text.toString())
        assertEquals(false, viewModel.state.passwordValidation.isValid)

        verify(exactly = 1) { arrangement.validatePassword("password") }
    }

    private class Arrangement {

        @MockK
        lateinit var validatePassword: ValidatePasswordUseCase

        @MockK
        lateinit var globalDataStore: GlobalDataStore

        @MockK
        private lateinit var observeAppLockConfig: ObserveAppLockConfigUseCase

        @MockK
        private lateinit var markTeamAppLockStatusAsNotified: MarkTeamAppLockStatusAsNotifiedUseCase

        @MockK
        private lateinit var observeIsAppLockEditableUseCase: ObserveIsAppLockEditableUseCase

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            coEvery { globalDataStore.setUserAppLock(any(), any()) } returns Unit
            coEvery { observeAppLockConfig() } returns flowOf(
                AppLockConfig.Disabled(ObserveAppLockConfigUseCase.DEFAULT_APP_LOCK_TIMEOUT)
            )

            coEvery { observeIsAppLockEditableUseCase() } returns flowOf(true)
        }

        fun withValidPassword() = apply {
            every { validatePassword(any()) } returns ValidatePasswordResult.Valid
            coEvery { validatePassword(any()) } returns ValidatePasswordResult.Valid
            every { validatePassword.invoke(any()) } returns ValidatePasswordResult.Valid
            coEvery { validatePassword.invoke(any()) } returns ValidatePasswordResult.Valid
        }

        fun withInvalidPassword() = apply {
            every { validatePassword(any()) } returns ValidatePasswordResult.Invalid()
        }

        fun withIsAppLockEditable(result: Boolean) = apply {
            coEvery { observeIsAppLockEditableUseCase() } returns flowOf(result)
        }

        private val viewModel by lazy {
            SetLockScreenViewModel(
                validatePassword,
                globalDataStore,
                TestDispatcherProvider(),
                observeAppLockConfig,
                observeIsAppLockEditableUseCase,
                markTeamAppLockStatusAsNotified
            )
        }

        fun arrange() = this to viewModel
    }
}
