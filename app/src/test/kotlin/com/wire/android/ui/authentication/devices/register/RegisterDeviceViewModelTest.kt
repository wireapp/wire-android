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

package com.wire.android.ui.authentication.devices.register

import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.mockUri
import com.wire.android.datastore.UserDataStore
import com.wire.android.framework.TestClient
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.feature.client.GetOrRegisterClientUseCase
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.user.IsPasswordRequiredUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class RegisterDeviceViewModelTest {

    @MockK
    private lateinit var registerClientUseCase: GetOrRegisterClientUseCase

    @MockK
    private lateinit var isPasswordRequiredUseCase: IsPasswordRequiredUseCase

    @MockK
    lateinit var userDataStore: UserDataStore

    private lateinit var registerDeviceViewModel: RegisterDeviceViewModel

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        mockUri()
        coEvery { isPasswordRequiredUseCase() } returns IsPasswordRequiredUseCase.Result.Success(true)
        every { userDataStore.initialSyncCompleted } returns flowOf(true)
        registerDeviceViewModel = RegisterDeviceViewModel(
            registerClientUseCase,
            isPasswordRequiredUseCase,
            userDataStore
        )
    }

    @Test
    fun `given empty string, when entering the password to register, then button is disabled`() {
        registerDeviceViewModel.onPasswordChange(TextFieldValue(String.EMPTY))
        registerDeviceViewModel.state.continueEnabled shouldBeEqualTo false
        registerDeviceViewModel.state.flowState shouldBeInstanceOf RegisterDeviceFlowState.Default::class
    }

    @Test
    fun `given non-empty string, when entering the password to register, then button is disabled`() {
        registerDeviceViewModel.onPasswordChange(TextFieldValue("abc"))
        registerDeviceViewModel.state.continueEnabled shouldBeEqualTo true
        registerDeviceViewModel.state.flowState shouldBeInstanceOf RegisterDeviceFlowState.Default::class
    }

    @Test
    fun `given button is clicked, when request returns Success, then navigateToHomeScreen is called`() = runTest {
        val password = "abc"
        coEvery {
            registerClientUseCase(
                any()
            )
        } returns RegisterClientResult.Success(CLIENT)

        registerDeviceViewModel.onPasswordChange(TextFieldValue(password))

        registerDeviceViewModel.onContinue()
        advanceUntilIdle()
        coVerify(exactly = 1) {
            registerClientUseCase(any())
        }
        registerDeviceViewModel.state.flowState shouldBeInstanceOf RegisterDeviceFlowState.Success::class
    }

    @Test
    fun `given button is clicked, when request returns TooManyClients Error, then navigateToRemoveDevicesScreen is called`() = runTest {
        val password = "abc"
        coEvery {
            registerClientUseCase(any())
        } returns RegisterClientResult.Failure.TooManyClients
        registerDeviceViewModel.onPasswordChange(TextFieldValue(password))

        registerDeviceViewModel.onContinue()
        advanceUntilIdle()
        coVerify(exactly = 1) {
            registerClientUseCase(any())
        }
        registerDeviceViewModel.state.flowState shouldBeInstanceOf RegisterDeviceFlowState.TooManyDevices::class
    }

    @Test
    fun `given button is clicked, when password is invalid, then UsernameInvalidError is passed`() = runTest {
        coEvery {
            registerClientUseCase(any())
        } returns RegisterClientResult.Failure.InvalidCredentials.InvalidPassword

        registerDeviceViewModel.onContinue()
        advanceUntilIdle()
        registerDeviceViewModel.state.flowState shouldBeInstanceOf RegisterDeviceFlowState.Error::class
        registerDeviceViewModel.state.flowState shouldBeInstanceOf RegisterDeviceFlowState.Error::class
    }

    @Test
    fun `given button is clicked, when request returns Generic error, then GenericError is passed`() = runTest {
        val networkFailure = NetworkFailure.NoNetworkConnection(null)
        coEvery { registerClientUseCase(any()) } returns
                RegisterClientResult.Failure.Generic(networkFailure)

        registerDeviceViewModel.onContinue()
        advanceUntilIdle()

        registerDeviceViewModel.state.flowState shouldBeInstanceOf RegisterDeviceFlowState.Error.GenericError::class
        val error = registerDeviceViewModel.state.flowState as RegisterDeviceFlowState.Error.GenericError
        error.coreFailure shouldBe networkFailure
    }

    @Test
    fun `given dialog is dismissed, when state error is DialogError, then hide error`() = runTest {
        val networkFailure = NetworkFailure.NoNetworkConnection(null)
        coEvery { registerClientUseCase(any()) } returns
                RegisterClientResult.Failure.Generic(networkFailure)

        registerDeviceViewModel.onContinue()
        advanceUntilIdle()
        registerDeviceViewModel.state.flowState shouldBeInstanceOf RegisterDeviceFlowState.Error.GenericError::class
        registerDeviceViewModel.onErrorDismiss()
        registerDeviceViewModel.state.flowState shouldBe RegisterDeviceFlowState.Default
    }

    companion object {
        val CLIENT = TestClient.CLIENT
    }
}
