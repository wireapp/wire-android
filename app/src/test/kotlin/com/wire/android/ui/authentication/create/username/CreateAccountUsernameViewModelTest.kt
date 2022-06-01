package com.wire.android.ui.authentication.create.username

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.mockUri
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItemDestinationsRoutes
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.feature.user.SetUserHandleUseCase
import com.wire.kalium.logic.feature.auth.ValidateUserHandleUseCase
import com.wire.kalium.logic.feature.user.SetUserHandleResult
import com.wire.kalium.logic.feature.auth.ValidateUserHandleResult
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
class CreateAccountUsernameViewModelTest {

    @MockK private lateinit var navigationManager: NavigationManager
    @MockK private lateinit var validateUserHandleUseCase: ValidateUserHandleUseCase
    @MockK private lateinit var setUserHandleUseCase: SetUserHandleUseCase

    private lateinit var createAccountUsernameViewModel: CreateAccountUsernameViewModel

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        mockUri()
        createAccountUsernameViewModel = CreateAccountUsernameViewModel(navigationManager, validateUserHandleUseCase, setUserHandleUseCase)
    }

    @Test
    fun `given empty string, when entering username, then button is disabled`() {
        coEvery { validateUserHandleUseCase.invoke(String.EMPTY) } returns ValidateUserHandleResult.Invalid.TooShort(String.EMPTY)
        createAccountUsernameViewModel.onUsernameChange(TextFieldValue(String.EMPTY))
        createAccountUsernameViewModel.state.continueEnabled shouldBeEqualTo false
        createAccountUsernameViewModel.state.loading shouldBeEqualTo false
    }

    @Test
    fun `given non-empty string, when entering username, then button is disabled`() {
        coEvery { validateUserHandleUseCase.invoke("abc") } returns ValidateUserHandleResult.Valid("abc")
        createAccountUsernameViewModel.onUsernameChange(TextFieldValue("abc"))
        createAccountUsernameViewModel.state.continueEnabled shouldBeEqualTo true
        createAccountUsernameViewModel.state.loading shouldBeEqualTo false
    }

    @Test
    fun `given forbidden character, when entering username, then forbidden character is ignored`() {
        coEvery { validateUserHandleUseCase.invoke("a1_") } returns ValidateUserHandleResult.Valid("a1_")
        coEvery { validateUserHandleUseCase.invoke("a1_$") } returns ValidateUserHandleResult.Invalid.InvalidCharacters("a1_")
        createAccountUsernameViewModel.onUsernameChange(TextFieldValue("a1_"))
        createAccountUsernameViewModel.state.username.text shouldBeEqualTo "a1_"
        createAccountUsernameViewModel.onUsernameChange(TextFieldValue("a1_$"))
        createAccountUsernameViewModel.state.username.text shouldBeEqualTo "a1_"
    }

    @Test
    fun `given button is clicked, when setting the username, then show loading`() {
        val scheduler = TestCoroutineScheduler()
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        coEvery { validateUserHandleUseCase.invoke(any()) } returns ValidateUserHandleResult.Valid("abc")
        coEvery { setUserHandleUseCase.invoke(any()) } returns SetUserHandleResult.Success

        createAccountUsernameViewModel.onUsernameChange(TextFieldValue("abc"))
        createAccountUsernameViewModel.state.continueEnabled shouldBeEqualTo true
        createAccountUsernameViewModel.state.loading shouldBeEqualTo false
        createAccountUsernameViewModel.onContinue()
        createAccountUsernameViewModel.state.continueEnabled shouldBeEqualTo false
        createAccountUsernameViewModel.state.loading shouldBeEqualTo true
        scheduler.advanceUntilIdle()
        createAccountUsernameViewModel.state.continueEnabled shouldBeEqualTo true
        createAccountUsernameViewModel.state.loading shouldBeEqualTo false
    }

    @Test
    fun `given button is clicked, when request returns Success, then navigateToConvScreen is called`() {
        val scheduler = TestCoroutineScheduler()
        val username = "abc"
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        coEvery { validateUserHandleUseCase.invoke(any()) } returns ValidateUserHandleResult.Valid(username)
        coEvery { setUserHandleUseCase.invoke(any()) } returns SetUserHandleResult.Success
        coEvery { navigationManager.navigate(any()) } returns Unit
        createAccountUsernameViewModel.onUsernameChange(TextFieldValue(username))

        runTest { createAccountUsernameViewModel.onContinue() }

        // FIXME: change to 1 once the viewModel is fixed
        coVerify(exactly = 2) { validateUserHandleUseCase.invoke(username) }
        coVerify(exactly = 1) { setUserHandleUseCase.invoke(username) }
        coVerify(exactly = 1) {
            navigationManager.navigate(NavigationCommand(NavigationItemDestinationsRoutes.HOME, BackStackMode.CLEAR_WHOLE))
        }
    }

    @Test
    fun `given button is clicked, when username is invalid, then UsernameInvalidError is passed`() {
        coEvery { validateUserHandleUseCase.invoke(any()) } returns ValidateUserHandleResult.Invalid.TooShort("a")
        coEvery { setUserHandleUseCase.invoke(any()) } returns SetUserHandleResult.Failure.InvalidHandle

        runTest { createAccountUsernameViewModel.onContinue() }

        createAccountUsernameViewModel.state.error shouldBeInstanceOf
                CreateAccountUsernameViewState.UsernameError.TextFieldError.UsernameInvalidError::class
    }

    @Test
    fun `given button is clicked, when request returns HandleExists error, then UsernameTakenError is passed`() {
        coEvery { validateUserHandleUseCase.invoke(any()) } returns ValidateUserHandleResult.Valid("abc")
        coEvery { setUserHandleUseCase.invoke(any()) } returns SetUserHandleResult.Failure.HandleExists

        runTest { createAccountUsernameViewModel.onContinue() }

        createAccountUsernameViewModel.state.error shouldBeInstanceOf
                CreateAccountUsernameViewState.UsernameError.TextFieldError.UsernameTakenError::class
    }

    @Test
    fun `given button is clicked, when request returns Generic error, then GenericError is passed`() {
        val networkFailure = NetworkFailure.NoNetworkConnection(null)
        coEvery { validateUserHandleUseCase.invoke(any()) } returns ValidateUserHandleResult.Valid("abc")
        coEvery { setUserHandleUseCase.invoke(any()) } returns SetUserHandleResult.Failure.Generic(networkFailure)

        runTest { createAccountUsernameViewModel.onContinue() }

        createAccountUsernameViewModel.state.error shouldBeInstanceOf
                CreateAccountUsernameViewState.UsernameError.DialogError.GenericError::class
        val error = createAccountUsernameViewModel.state.error as CreateAccountUsernameViewState.UsernameError.DialogError.GenericError
        error.coreFailure shouldBe networkFailure
    }

    @Test
    fun `given dialog is dismissed, when state error is DialogError, then hide error`() {
        coEvery { validateUserHandleUseCase.invoke(any()) } returns ValidateUserHandleResult.Valid("abc")
        coEvery { setUserHandleUseCase.invoke(any()) } returns SetUserHandleResult.Failure.Generic(NetworkFailure.NoNetworkConnection(null))

        runTest { createAccountUsernameViewModel.onContinue() }

        createAccountUsernameViewModel.state.error shouldBeInstanceOf
                CreateAccountUsernameViewState.UsernameError.DialogError.GenericError::class
        createAccountUsernameViewModel.onErrorDismiss()
        createAccountUsernameViewModel.state.error shouldBe CreateAccountUsernameViewState.UsernameError.None
    }
}

