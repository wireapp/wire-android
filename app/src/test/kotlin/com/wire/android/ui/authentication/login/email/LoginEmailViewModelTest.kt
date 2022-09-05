package com.wire.android.ui.authentication.login.email

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.mockUri
import com.wire.android.di.AuthServerConfigProvider
import com.wire.android.di.ClientScopeProvider
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItemDestinationsRoutes
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.authentication.login.LoginError
import com.wire.android.util.EMPTY
import com.wire.android.util.newServerConfig
import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.data.client.Client
import com.wire.kalium.logic.data.client.ClientType
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.user.SsoId
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.AuthSession
import com.wire.kalium.logic.feature.auth.AuthenticationResult
import com.wire.kalium.logic.feature.auth.LoginUseCase
import com.wire.kalium.logic.feature.client.ClientScope
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.client.RegisterClientUseCase
import com.wire.kalium.logic.feature.server.FetchApiVersionResult
import com.wire.kalium.logic.feature.server.FetchApiVersionUseCase
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
import com.wire.kalium.logic.feature.session.RegisterTokenResult
import com.wire.kalium.logic.feature.session.RegisterTokenUseCase
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
    private lateinit var getSessionsUseCase: GetSessionsUseCase

    @MockK
    private lateinit var clientScope: ClientScope

    @MockK
    private lateinit var registerClientUseCase: RegisterClientUseCase

    @MockK
    private lateinit var registerTokenUseCase: RegisterTokenUseCase

    @MockK
    private lateinit var savedStateHandle: SavedStateHandle

    @MockK
    private lateinit var navigationManager: NavigationManager

    @MockK
    private lateinit var authSession: AuthSession

    @MockK
    private lateinit var qualifiedIdMapper: QualifiedIdMapper

    @MockK
    private lateinit var fetchApiVersion: FetchApiVersionUseCase

    @MockK
    private lateinit var authServerConfigProvider: AuthServerConfigProvider

    private lateinit var loginViewModel: LoginEmailViewModel

    private val userId: QualifiedID = QualifiedID("userId", "domain")

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        mockUri()
        every { savedStateHandle.get<String>(any()) } returns ""
        every { qualifiedIdMapper.fromStringToQualifiedID(any()) } returns userId
        every { savedStateHandle.set(any(), any<String>()) } returns Unit
        every { clientScopeProviderFactory.create(any()).clientScope } returns clientScope
        every { clientScope.register } returns registerClientUseCase
        every { clientScope.registerPushToken } returns registerTokenUseCase
        every { authSession.session.userId } returns userId
        every { authServerConfigProvider.authServer.value } returns newServerConfig(1).links
        coEvery { fetchApiVersion(newServerConfig(1).links) } returns FetchApiVersionResult.Success(newServerConfig(1))
        loginViewModel = LoginEmailViewModel(
            loginUseCase,
            addAuthenticatedUserUseCase,
            qualifiedIdMapper,
            clientScopeProviderFactory,
            getSessionsUseCase,
            fetchApiVersion,
            savedStateHandle,
            navigationManager,
            authServerConfigProvider
        )
    }

    @Test
    fun `given empty strings, when entering credentials, then button is disabled`() {
        loginViewModel.onPasswordChange(TextFieldValue(String.EMPTY))
        loginViewModel.onUserIdentifierChange(TextFieldValue(String.EMPTY))
        loginViewModel.loginState.emailLoginEnabled shouldBeEqualTo false
        loginViewModel.loginState.emailLoginLoading shouldBeEqualTo false
    }

    @Test
    fun `given non-empty strings, when entering credentials, then button is enabled`() {
        loginViewModel.onPasswordChange(TextFieldValue("abc"))
        loginViewModel.onUserIdentifierChange(TextFieldValue("abc"))
        loginViewModel.loginState.emailLoginEnabled shouldBeEqualTo true
        loginViewModel.loginState.emailLoginLoading shouldBeEqualTo false
    }

    @Test
    fun `given button is clicked, when logging in, then show loading`() {
        val scheduler = TestCoroutineScheduler()
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        coEvery { loginUseCase(any(), any(), any()) } returns AuthenticationResult.Failure.InvalidCredentials
        coEvery { addAuthenticatedUserUseCase(any(), any()) } returns AddAuthenticatedUserUseCase.Result.Success(userId)

        loginViewModel.onPasswordChange(TextFieldValue("abc"))
        loginViewModel.onUserIdentifierChange(TextFieldValue("abc"))
        loginViewModel.loginState.emailLoginEnabled shouldBeEqualTo true
        loginViewModel.loginState.emailLoginLoading shouldBeEqualTo false
        loginViewModel.login()
        loginViewModel.loginState.emailLoginEnabled shouldBeEqualTo false
        loginViewModel.loginState.emailLoginLoading shouldBeEqualTo true
        scheduler.advanceUntilIdle()
        loginViewModel.loginState.emailLoginEnabled shouldBeEqualTo true
        loginViewModel.loginState.emailLoginLoading shouldBeEqualTo false
    }

    @Test
    fun `given button is clicked, when login returns Success, then navigateToConvScreen is called`() {
        val scheduler = TestCoroutineScheduler()
        val password = "abc"
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        coEvery { loginUseCase(any(), any(), any()) } returns AuthenticationResult.Success(authSession, SSO_ID)
        coEvery { addAuthenticatedUserUseCase(any(), any()) } returns AddAuthenticatedUserUseCase.Result.Success(userId)
        coEvery { navigationManager.navigate(any()) } returns Unit
        coEvery { registerClientUseCase(any()) } returns RegisterClientResult.Success(CLIENT)
        coEvery { registerTokenUseCase(any(), CLIENT.id) } returns RegisterTokenResult.Success

        loginViewModel.onPasswordChange(TextFieldValue(password))

        runTest { loginViewModel.login() }
        coVerify(exactly = 1) { loginUseCase(any(), any(), any()) }
        coVerify(exactly = 1) { registerClientUseCase(any()) }
        coVerify(exactly = 1) { registerTokenUseCase(any(), CLIENT.id) }
        coVerify(exactly = 1) {
            navigationManager.navigate(
                NavigationCommand(
                    NavigationItemDestinationsRoutes.HOME,
                    BackStackMode.CLEAR_WHOLE
                )
            )
        }
    }

    @Test
    fun `given button is clicked, when login returns InvalidUserIdentifier error, then InvalidUserIdentifierError is passed`() {
        coEvery { loginUseCase(any(), any(), any()) } returns AuthenticationResult.Failure.InvalidUserIdentifier

        runTest { loginViewModel.login() }

        loginViewModel.loginState.loginError shouldBeInstanceOf LoginError.TextFieldError.InvalidValue::class
    }

    @Test
    fun `given button is clicked, when login returns InvalidCredentials error, then InvalidCredentialsError is passed`() {
        coEvery { loginUseCase(any(), any(), any()) } returns AuthenticationResult.Failure.InvalidCredentials

        runTest { loginViewModel.login() }

        loginViewModel.loginState.loginError shouldBeInstanceOf LoginError.DialogError.InvalidCredentialsError::class
    }

    @Test
    fun `given button is clicked, when login returns Generic error, then GenericError is passed`() {
        val networkFailure = NetworkFailure.NoNetworkConnection(null)
        coEvery { loginUseCase(any(), any(), any()) } returns
                AuthenticationResult.Failure.Generic(networkFailure)

        runTest { loginViewModel.login() }

        loginViewModel.loginState.loginError shouldBeInstanceOf LoginError.DialogError.GenericError::class
        (loginViewModel.loginState.loginError as LoginError.DialogError.GenericError).coreFailure shouldBe networkFailure
    }

    @Test
    fun `given dialog is dismissed, when login returns DialogError, then hide error`() {
        coEvery { loginUseCase(any(), any(), any()) } returns AuthenticationResult.Failure.InvalidCredentials

        runTest { loginViewModel.login() }

        loginViewModel.loginState.loginError shouldBeInstanceOf LoginError.DialogError.InvalidCredentialsError::class
        loginViewModel.onDialogDismiss()
        loginViewModel.loginState.loginError shouldBe LoginError.None
    }

    @Test
    fun `given button is clicked, when addAuthenticatedUser returns UserAlreadyExists error, then UserAlreadyExists is passed`() {
        coEvery { loginUseCase(any(), any(), any()) } returns AuthenticationResult.Success(authSession, SSO_ID)
        coEvery { addAuthenticatedUserUseCase(any(), any()) } returns AddAuthenticatedUserUseCase.Result.Failure.UserAlreadyExists

        runTest { loginViewModel.login() }

        loginViewModel.loginState.loginError shouldBeInstanceOf LoginError.DialogError.UserAlreadyExists::class
    }

    companion object {
        val CLIENT_ID = ClientId("test")
        val CLIENT = Client(
            CLIENT_ID, ClientType.Permanent, "time", null,
            null, "label", "cookie", null, "model"
        )
        val SSO_ID: SsoId = SsoId("scim_id", null, null)
    }
}

