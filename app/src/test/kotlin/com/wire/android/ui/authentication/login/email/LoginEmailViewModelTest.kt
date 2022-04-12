package com.wire.android.ui.authentication.login.email

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.di.ClientScopeProvider
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItemDestinationsRoutes
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.authentication.login.LoginEmailError
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.configuration.ServerConfig
import com.wire.kalium.logic.data.client.Client
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.AuthSession
import com.wire.kalium.logic.feature.auth.AuthenticationResult
import com.wire.kalium.logic.feature.auth.LoginUseCase
import com.wire.kalium.logic.feature.client.ClientScope
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.client.RegisterClientUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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
class LoginEmailViewModelTest {

    @MockK
    private lateinit var loginUseCase: LoginUseCase
    @MockK
    private lateinit var addAuthenticatedUserUseCase: AddAuthenticatedUserUseCase
    @MockK
    private lateinit var clientScopeProviderFactory: ClientScopeProvider.Factory
    @MockK
    private lateinit var clientScope: ClientScope
    @MockK
    private lateinit var registerClientUseCase: RegisterClientUseCase
    @MockK
    private lateinit var savedStateHandle: SavedStateHandle
    @MockK
    private lateinit var navigationManager: NavigationManager
    @MockK
    private lateinit var authSession: AuthSession
    @MockK
    private lateinit var serverConfig: ServerConfig
    @MockK
    private lateinit var client: Client

    private lateinit var loginViewModel: LoginEmailViewModel

    private val apiBaseUrl: String = "apiBaseUrl"
    private val userId: QualifiedID = QualifiedID("userId", "domain")

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        every { savedStateHandle.get<String>(any()) } returns ""
        every { savedStateHandle.set(any(), any<String>()) } returns Unit
        every { clientScopeProviderFactory.create(any()).clientScope } returns clientScope
        every { clientScope.register } returns registerClientUseCase
        every { serverConfig.apiBaseUrl } returns apiBaseUrl
        every { authSession.userId } returns userId
        loginViewModel = LoginEmailViewModel(
            loginUseCase,
            addAuthenticatedUserUseCase,
            clientScopeProviderFactory,
            savedStateHandle,
            navigationManager
        )
    }

    @Test
    fun `given empty strings, when entering credentials, then button is disabled`() {
        loginViewModel.onPasswordChange(TextFieldValue(String.EMPTY))
        loginViewModel.onUserIdentifierChange(TextFieldValue(String.EMPTY))
        loginViewModel.loginState.loginEnabled shouldBeEqualTo false
        loginViewModel.loginState.loading shouldBeEqualTo false
    }

    @Test
    fun `given non-empty strings, when entering credentials, then button is enabled`() {
        loginViewModel.onPasswordChange(TextFieldValue("abc"))
        loginViewModel.onUserIdentifierChange(TextFieldValue("abc"))
        loginViewModel.loginState.loginEnabled shouldBeEqualTo true
        loginViewModel.loginState.loading shouldBeEqualTo false
    }

    @Test
    fun `given button is clicked, when logging in, then show loading`() {
        val scheduler = TestCoroutineScheduler()
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        coEvery { loginUseCase.invoke(any(), any(), any(), any()) } returns AuthenticationResult.Failure.InvalidCredentials
        coEvery { addAuthenticatedUserUseCase.invoke(any(), any()) } returns AddAuthenticatedUserUseCase.Result.Success(userId)

        loginViewModel.onPasswordChange(TextFieldValue("abc"))
        loginViewModel.onUserIdentifierChange(TextFieldValue("abc"))
        loginViewModel.loginState.loginEnabled shouldBeEqualTo true
        loginViewModel.loginState.loading shouldBeEqualTo false
        loginViewModel.login(serverConfig)
        loginViewModel.loginState.loginEnabled shouldBeEqualTo false
        loginViewModel.loginState.loading shouldBeEqualTo true
        scheduler.advanceUntilIdle()
        loginViewModel.loginState.loginEnabled shouldBeEqualTo true
        loginViewModel.loginState.loading shouldBeEqualTo false
    }

    @Test
    fun `given button is clicked, when login returns Success, then navigateToConvScreen is called`() {
        val scheduler = TestCoroutineScheduler()
        val password = "abc"
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        coEvery { loginUseCase.invoke(any(), any(), any(), any()) } returns AuthenticationResult.Success(authSession)
        coEvery { addAuthenticatedUserUseCase.invoke(any(), any()) } returns AddAuthenticatedUserUseCase.Result.Success(userId)
        coEvery { navigationManager.navigate(any()) } returns Unit
        coEvery { registerClientUseCase.invoke(any(), any(), any()) } returns RegisterClientResult.Success(client)
        loginViewModel.onPasswordChange(TextFieldValue(password))
        runTest { loginViewModel.login(serverConfig) }
        coVerify(exactly = 1) { registerClientUseCase.invoke(password, null) }
        coVerify(exactly = 1) {
            navigationManager.navigate(NavigationCommand(NavigationItemDestinationsRoutes.HOME, BackStackMode.CLEAR_WHOLE))
        }
    }

    @Test
    fun `given button is clicked, when login returns InvalidUserIdentifier error, then InvalidUserIdentifierError is passed`() {
        coEvery { loginUseCase.invoke(any(), any(), any(), any()) } returns AuthenticationResult.Failure.InvalidUserIdentifier
        runTest { loginViewModel.login(serverConfig) }
        loginViewModel.loginState.loginEmailError shouldBeInstanceOf LoginEmailError.TextFieldError.InvalidUserIdentifierError::class
    }

    @Test
    fun `given button is clicked, when login returns InvalidCredentials error, then InvalidCredentialsError is passed`() {
        coEvery { loginUseCase.invoke(any(), any(), any(), any()) } returns AuthenticationResult.Failure.InvalidCredentials
        runTest { loginViewModel.login(serverConfig) }
        loginViewModel.loginState.loginEmailError shouldBeInstanceOf LoginEmailError.DialogError.InvalidCredentialsError::class
    }

    @Test
    fun `given button is clicked, when login returns Generic error, then GenericError is passed`() {
        coEvery { loginUseCase.invoke(any(), any(), any(), any()) } returns
                AuthenticationResult.Failure.Generic(NetworkFailure.NoNetworkConnection)
        runTest { loginViewModel.login(serverConfig) }
        loginViewModel.loginState.loginEmailError shouldBeInstanceOf LoginEmailError.DialogError.GenericError::class
        (loginViewModel.loginState.loginEmailError as LoginEmailError.DialogError.GenericError).coreFailure shouldBe
                NetworkFailure.NoNetworkConnection
    }

    @Test
    fun `given dialog is dismissed, when login returns DialogError, then hide error`() {
        coEvery { loginUseCase.invoke(any(), any(), any(), any()) } returns AuthenticationResult.Failure.InvalidCredentials
        runTest { loginViewModel.login(serverConfig) }
        loginViewModel.loginState.loginEmailError shouldBeInstanceOf LoginEmailError.DialogError.InvalidCredentialsError::class
        loginViewModel.onDialogDismiss()
        loginViewModel.loginState.loginEmailError shouldBe LoginEmailError.None
    }

    @Test
    fun `given button is clicked, when addAuthenticatedUser returns UserAlreadyExists error, then UserAlreadyExists is passed`() {
        coEvery { loginUseCase.invoke(any(), any(), any(), any()) } returns AuthenticationResult.Success(authSession)
        coEvery { addAuthenticatedUserUseCase.invoke(any(), any()) } returns AddAuthenticatedUserUseCase.Result.Failure.UserAlreadyExists
        runTest { loginViewModel.login(serverConfig) }
        loginViewModel.loginState.loginEmailError shouldBeInstanceOf LoginEmailError.DialogError.UserAlreadyExists::class
    }
}

