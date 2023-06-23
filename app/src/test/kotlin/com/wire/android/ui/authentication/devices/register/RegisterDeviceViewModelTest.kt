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
 *
 *
 */

package com.wire.android.ui.authentication.devices.register

import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.mockUri
import com.wire.android.datastore.UserDataStore
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationItemDestinationsRoutes
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.data.client.Client
import com.wire.kalium.logic.data.client.ClientType
import com.wire.kalium.logic.data.client.DeviceType
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.client.GetOrRegisterClientUseCase
import com.wire.kalium.logic.feature.user.IsPasswordRequiredUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
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
    private lateinit var navigationManager: NavigationManager

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
        registerDeviceViewModel =
            RegisterDeviceViewModel(
                navigationManager,
                registerClientUseCase,
                isPasswordRequiredUseCase,
                userDataStore
            )
    }

    @Test
    fun `given empty string, when entering the password to register, then button is disabled`() {
        registerDeviceViewModel.onPasswordChange(TextFieldValue(String.EMPTY))
        registerDeviceViewModel.state.continueEnabled shouldBeEqualTo false
        registerDeviceViewModel.state.loading shouldBeEqualTo false
    }

    @Test
    fun `given non-empty string, when entering the password to register, then button is disabled`() {
        registerDeviceViewModel.onPasswordChange(TextFieldValue("abc"))
        registerDeviceViewModel.state.continueEnabled shouldBeEqualTo true
        registerDeviceViewModel.state.loading shouldBeEqualTo false
    }

    @Test
    fun `given button is clicked, when registering the client, then show loading`() {
        coEvery {
            registerClientUseCase(any())
        } returns RegisterClientResult.Success(CLIENT)
        coEvery { navigationManager.navigate(any()) } returns Unit

        registerDeviceViewModel.onPasswordChange(TextFieldValue("abc"))
        registerDeviceViewModel.state.continueEnabled shouldBeEqualTo true
        registerDeviceViewModel.state.loading shouldBeEqualTo false
        registerDeviceViewModel.onContinue()
        registerDeviceViewModel.state.continueEnabled shouldBeEqualTo false
        registerDeviceViewModel.state.loading shouldBeEqualTo true
    }

    @Test
    fun `given button is clicked, when request returns Success, then navigateToHomeScreen is called`() {
        val scheduler = TestCoroutineScheduler()
        val password = "abc"
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        coEvery {
            registerClientUseCase(
                any()
            )
        } returns RegisterClientResult.Success(CLIENT)

        coEvery { navigationManager.navigate(any()) } returns Unit
        registerDeviceViewModel.onPasswordChange(TextFieldValue(password))

        runTest { registerDeviceViewModel.onContinue() }

        coVerify(exactly = 1) {
            registerClientUseCase(any())
        }
        coVerify(exactly = 1) {
            navigationManager.navigate(NavigationCommand(NavigationItemDestinationsRoutes.HOME, BackStackMode.CLEAR_WHOLE))
        }
    }

    @Test
    fun `given button is clicked, when request returns TooManyClients Error, then navigateToRemoveDevicesScreen is called`() {
        val scheduler = TestCoroutineScheduler()
        val password = "abc"
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        coEvery {
            registerClientUseCase(any())
        } returns RegisterClientResult.Failure.TooManyClients
        coEvery { navigationManager.navigate(any()) } returns Unit
        registerDeviceViewModel.onPasswordChange(TextFieldValue(password))

        runTest { registerDeviceViewModel.onContinue() }

        coVerify(exactly = 1) {
            registerClientUseCase(any())
        }
        coVerify(exactly = 1) {
            navigationManager.navigate(NavigationCommand(NavigationItem.RemoveDevices.getRouteWithArgs(), BackStackMode.CLEAR_WHOLE))
        }
    }

    @Test
    fun `given button is clicked, when password is invalid, then UsernameInvalidError is passed`() {
        coEvery {
            registerClientUseCase(any())
        } returns RegisterClientResult.Failure.InvalidCredentials.InvalidPassword
        coEvery { navigationManager.navigate(any()) } returns Unit

        runTest { registerDeviceViewModel.onContinue() }

        registerDeviceViewModel.state.error shouldBeInstanceOf RegisterDeviceError.InvalidCredentialsError::class
    }

    @Test
    fun `given button is clicked, when request returns Generic error, then GenericError is passed`() {
        val networkFailure = NetworkFailure.NoNetworkConnection(null)
        coEvery { registerClientUseCase(any()) } returns
                RegisterClientResult.Failure.Generic(networkFailure)

        runTest { registerDeviceViewModel.onContinue() }

        registerDeviceViewModel.state.error shouldBeInstanceOf RegisterDeviceError.GenericError::class
        val error = registerDeviceViewModel.state.error as RegisterDeviceError.GenericError
        error.coreFailure shouldBe networkFailure
    }

    @Test
    fun `given dialog is dismissed, when state error is DialogError, then hide error`() {
        val networkFailure = NetworkFailure.NoNetworkConnection(null)
        coEvery { registerClientUseCase(any()) } returns
                RegisterClientResult.Failure.Generic(networkFailure)

        runTest { registerDeviceViewModel.onContinue() }

        registerDeviceViewModel.state.error shouldBeInstanceOf RegisterDeviceError.GenericError::class
        registerDeviceViewModel.onErrorDismiss()
        registerDeviceViewModel.state.error shouldBe RegisterDeviceError.None
    }

    companion object {
        val CLIENT_ID = ClientId("test")
        val CLIENT = Client(
            CLIENT_ID, ClientType.Permanent, Instant.DISTANT_FUTURE, false,
            isValid = true, DeviceType.Desktop, "label", null
        )
    }
}
