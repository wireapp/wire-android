package com.wire.android.ui.authentication.login

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.di.ClientScopeProvider
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItemDestinationsRoutes
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.configuration.ServerConfig
import com.wire.kalium.logic.data.client.Client
import com.wire.kalium.logic.data.id.QualifiedID
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
class LoginViewModelTest {

    @MockK private lateinit var loginUseCase: LoginUseCase
    @MockK private lateinit var clientScopeProviderFactory: ClientScopeProvider.Factory
    @MockK private lateinit var clientScope: ClientScope
    @MockK private lateinit var registerClientUseCase: RegisterClientUseCase
    @MockK private lateinit var savedStateHandle: SavedStateHandle
    @MockK private lateinit var navigationManager: NavigationManager
    @MockK private lateinit var authSession: AuthSession
    @MockK private lateinit var serverConfig: ServerConfig
    @MockK private lateinit var client: Client

    private lateinit var loginViewModel: LoginViewModel

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
        loginViewModel = LoginViewModel(loginUseCase, clientScopeProviderFactory, savedStateHandle, navigationManager)
    }

    @Test
    fun `when fields are empty, button is disabled`() {
        loginViewModel.onPasswordChange(TextFieldValue(String.EMPTY))
        loginViewModel.onUserIdentifierChange(TextFieldValue(String.EMPTY))
        loginViewModel.loginState.loginEnabled shouldBeEqualTo false
        loginViewModel.loginState.loading shouldBeEqualTo false
    }

    @Test
    fun `when fields are filled, button is enabled`() {
        loginViewModel.onPasswordChange(TextFieldValue("abc"))
        loginViewModel.onUserIdentifierChange(TextFieldValue("abc"))
        loginViewModel.loginState.loginEnabled shouldBeEqualTo true
        loginViewModel.loginState.loading shouldBeEqualTo false
    }

    @Test
    fun `when button is clicked, show loading`() {
        val scheduler = TestCoroutineScheduler()
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        coEvery { loginUseCase.invoke(any(), any(), any(), any(), any()) } returns AuthenticationResult.Failure.InvalidCredentials

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
    fun `when button is clicked and login returns Success, navigateToConvScreen is called`() {
        val scheduler = TestCoroutineScheduler()
        val password = "abc"
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        coEvery { loginUseCase.invoke(any(), any(), any(), any(), any()) } returns AuthenticationResult.Success(authSession)
        coEvery { navigationManager.navigate(any()) } returns Unit
        coEvery { registerClientUseCase.invoke(any(), any(), any())} returns RegisterClientResult.Success(client)
        loginViewModel.onPasswordChange(TextFieldValue(password))
        runTest { loginViewModel.login(serverConfig) }
        coVerify(exactly = 1) { registerClientUseCase.invoke(password, null) }
        coVerify(exactly = 1) {
            navigationManager.navigate(NavigationCommand(NavigationItemDestinationsRoutes.HOME, BackStackMode.CLEAR_WHOLE))
        }
    }

    @Test
    fun `when button is clicked and login returns InvalidUserIdentifier error, InvalidUserIdentifierError is passed`() {
        coEvery { loginUseCase.invoke(any(), any(), any(), any(), any()) } returns AuthenticationResult.Failure.InvalidUserIdentifier
        runTest { loginViewModel.login(serverConfig) }
        loginViewModel.loginState.loginError shouldBeInstanceOf LoginError.TextFieldError.InvalidUserIdentifierError::class
    }

    @Test
    fun `when button is clicked, with InvalidCredentials error, InvalidCredentialsError is passed`() {
        coEvery { loginUseCase.invoke(any(), any(), any(), any(), any()) } returns AuthenticationResult.Failure.InvalidCredentials
        runTest { loginViewModel.login(serverConfig) }
        loginViewModel.loginState.loginError shouldBeInstanceOf LoginError.DialogError.InvalidCredentialsError::class
    }

    @Test
    fun `when button is clicked and login returns Generic error, GenericError is passed`() {
        coEvery { loginUseCase.invoke(any(), any(), any(), any(), any()) } returns
                AuthenticationResult.Failure.Generic(NetworkFailure.NoNetworkConnection)
        runTest { loginViewModel.login(serverConfig) }
        loginViewModel.loginState.loginError shouldBeInstanceOf LoginError.DialogError.GenericError::class
        (loginViewModel.loginState.loginError as LoginError.DialogError.GenericError).coreFailure shouldBe
                NetworkFailure.NoNetworkConnection
    }

    @Test
    fun `when login returns DialogError and dialog is dismissed, hide error`() {
        coEvery { loginUseCase.invoke(any(), any(), any(), any(), any()) } returns AuthenticationResult.Failure.InvalidCredentials
        runTest { loginViewModel.login(serverConfig) }
        loginViewModel.loginState.loginError shouldBeInstanceOf LoginError.DialogError.InvalidCredentialsError::class
        loginViewModel.clearLoginError()
        loginViewModel.loginState.loginError shouldBe LoginError.None
    }
}

