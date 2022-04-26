package com.wire.android.ui.authentication.devices.register

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationItemDestinationsRoutes
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.data.client.Client
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.client.RegisterClientUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith


@OptIn(ExperimentalMaterialApi::class, ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class RegisterDeviceViewModelTest {

    @MockK
    private lateinit var navigationManager: NavigationManager
    @MockK
    private lateinit var validatePasswordUseCase: ValidatePasswordUseCase
    @MockK
    private lateinit var registerClientUseCase: RegisterClientUseCase
    @MockK
    private lateinit var client: Client

    private lateinit var registerDeviceViewModel: RegisterDeviceViewModel

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        registerDeviceViewModel = RegisterDeviceViewModel(navigationManager, validatePasswordUseCase, registerClientUseCase)
    }

    @Test
    fun `given empty string, when entering the password to register, then button is disabled`() {
        coEvery { validatePasswordUseCase.invoke(String.EMPTY) } returns false
        registerDeviceViewModel.onPasswordChange(TextFieldValue(String.EMPTY))
        registerDeviceViewModel.state.continueEnabled shouldBeEqualTo false
        registerDeviceViewModel.state.loading shouldBeEqualTo false
    }

    @Test
    fun `given non-empty string, when entering the password to register, then button is disabled`() {
        coEvery { validatePasswordUseCase.invoke("abc") } returns true
        registerDeviceViewModel.onPasswordChange(TextFieldValue("abc"))
        registerDeviceViewModel.state.continueEnabled shouldBeEqualTo true
        registerDeviceViewModel.state.loading shouldBeEqualTo false
    }

    @Test
    fun `given button is clicked, when registering the client, then show loading`() {
        coEvery { validatePasswordUseCase.invoke(any()) } returns true
        coEvery { registerClientUseCase.invoke(any(), any(), any()) } returns RegisterClientResult.Success(client)

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
        coEvery { validatePasswordUseCase.invoke(any()) } returns true
        coEvery { registerClientUseCase.invoke(any(), any(), any()) } returns RegisterClientResult.Success(client)
        coEvery { navigationManager.navigate(any()) } returns Unit
        registerDeviceViewModel.onPasswordChange(TextFieldValue(password))

        runTest { registerDeviceViewModel.onContinue() }

        coVerify(exactly = 1) { validatePasswordUseCase.invoke(password) }
        coVerify(exactly = 1) { registerClientUseCase.invoke(password, any(), any()) }
        coVerify(exactly = 1) {
            navigationManager.navigate(NavigationCommand(NavigationItemDestinationsRoutes.HOME, BackStackMode.CLEAR_WHOLE))
        }
    }

    @Test
    fun `given button is clicked, when request returns TooManyClients Error, then navigateToRemoveDevicesScreen is called`() {
        val scheduler = TestCoroutineScheduler()
        val password = "abc"
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        coEvery { validatePasswordUseCase.invoke(any()) } returns true
        coEvery { registerClientUseCase.invoke(any(), any(), any()) } returns RegisterClientResult.Failure.TooManyClients
        coEvery { navigationManager.navigate(any()) } returns Unit
        registerDeviceViewModel.onPasswordChange(TextFieldValue(password))

        runTest { registerDeviceViewModel.onContinue() }

        coVerify(exactly = 1) { validatePasswordUseCase.invoke(password) }
        coVerify(exactly = 1) { registerClientUseCase.invoke(password, any(), any()) }
        coVerify(exactly = 1) {
            navigationManager.navigate(NavigationCommand(NavigationItem.RemoveDevices.getRouteWithArgs(), BackStackMode.CLEAR_WHOLE))
        }
    }

    @Test
    fun `given button is clicked, when password is invalid, then UsernameInvalidError is passed`() {
        coEvery { validatePasswordUseCase.invoke(any()) } returns false
        coEvery { registerClientUseCase.invoke(any(), any(), any()) } returns RegisterClientResult.Failure.InvalidCredentials

        runTest { registerDeviceViewModel.onContinue() }

        registerDeviceViewModel.state.error shouldBeInstanceOf RegisterDeviceError.InvalidCredentialsError::class
    }

    @Test
    fun `given button is clicked, when request returns Generic error, then GenericError is passed`() {
        val networkFailure = NetworkFailure.NoNetworkConnection(null)
        coEvery { validatePasswordUseCase.invoke(any()) } returns true
        coEvery { registerClientUseCase.invoke(any(), any(), any()) } returns
                RegisterClientResult.Failure.Generic(networkFailure)

        runTest { registerDeviceViewModel.onContinue() }

        registerDeviceViewModel.state.error shouldBeInstanceOf RegisterDeviceError.GenericError::class
        val error = registerDeviceViewModel.state.error as RegisterDeviceError.GenericError
        error.coreFailure shouldBe networkFailure
    }

    @Test
    fun `given dialog is dismissed, when state error is DialogError, then hide error`() {
        coEvery { validatePasswordUseCase.invoke(any()) } returns true
        coEvery { registerClientUseCase.invoke(any(), any(), any()) } returns
                RegisterClientResult.Failure.Generic(NetworkFailure.NoNetworkConnection(null))

        runTest { registerDeviceViewModel.onContinue() }

        registerDeviceViewModel.state.error shouldBeInstanceOf RegisterDeviceError.GenericError::class
        registerDeviceViewModel.onErrorDismiss()
        registerDeviceViewModel.state.error shouldBe RegisterDeviceError.None
    }
}
