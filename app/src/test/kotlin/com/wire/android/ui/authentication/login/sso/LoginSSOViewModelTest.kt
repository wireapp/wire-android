package com.wire.android.ui.authentication.login.sso

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.di.ClientScopeProvider
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.authentication.login.LoginError
import com.wire.android.util.EMPTY
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.android.util.deeplink.SSOFailureCodes
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.client.Client
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.AuthSession
import com.wire.kalium.logic.feature.auth.sso.GetSSOLoginSessionUseCase
import com.wire.kalium.logic.feature.auth.sso.SSOInitiateLoginResult
import com.wire.kalium.logic.feature.auth.sso.SSOInitiateLoginUseCase
import com.wire.kalium.logic.feature.auth.sso.SSOLoginSessionResult
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
import kotlinx.coroutines.flow.first
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
class LoginSSOViewModelTest {

    @MockK
    private lateinit var savedStateHandle: SavedStateHandle

    @MockK
    private lateinit var serverConfig: ServerConfig

    @MockK
    private lateinit var ssoInitiateLoginUseCase: SSOInitiateLoginUseCase

    @MockK
    private lateinit var addAuthenticatedUserUseCase: AddAuthenticatedUserUseCase

    @MockK
    private lateinit var clientScopeProviderFactory: ClientScopeProvider.Factory

    @MockK
    private lateinit var clientScope: ClientScope

    @MockK
    private lateinit var registerClientUseCase: RegisterClientUseCase

    @MockK
    private lateinit var getSSOLoginSessionUseCase: GetSSOLoginSessionUseCase

    @MockK
    private lateinit var authSession: AuthSession

    @MockK
    private lateinit var client: Client

    @MockK
    private lateinit var navigationManager: NavigationManager
    private lateinit var loginViewModel: LoginSSOViewModel

    private val apiBaseUrl: String = "apiBaseUrl"
    private val userId: QualifiedID = QualifiedID("userId", "domain")

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        every { savedStateHandle.get<String>(any()) } returns ""
        every { savedStateHandle.set(any(), any<String>()) } returns Unit
        every { serverConfig.apiBaseUrl } returns apiBaseUrl
        every { clientScopeProviderFactory.create(any()).clientScope } returns clientScope
        every { clientScope.register } returns registerClientUseCase
        every { serverConfig.id } returns "0"
        loginViewModel = LoginSSOViewModel(
            savedStateHandle,
            ssoInitiateLoginUseCase,
            getSSOLoginSessionUseCase,
            addAuthenticatedUserUseCase,
            clientScopeProviderFactory,
            navigationManager
        )
    }

    @Test
    fun `given empty string, when entering code, then button is disabled`() {
        loginViewModel.onSSOCodeChange(TextFieldValue(String.EMPTY))
        loginViewModel.loginState.loginEnabled shouldBeEqualTo false
        loginViewModel.loginState.loading shouldBeEqualTo false
    }

    @Test
    fun `given non-empty string, when entering code, then button is enabled`() {
        loginViewModel.onSSOCodeChange(TextFieldValue("abc"))
        loginViewModel.loginState.loginEnabled shouldBeEqualTo true
        loginViewModel.loginState.loading shouldBeEqualTo false
    }

    @Test
    fun `given button is clicked, when logging in, then show loading`() {
        val scheduler = TestCoroutineScheduler()
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        coEvery { ssoInitiateLoginUseCase.invoke(any()) } returns SSOInitiateLoginResult.Success("")

        loginViewModel.onSSOCodeChange(TextFieldValue("abc"))
        loginViewModel.loginState.loginEnabled shouldBeEqualTo true
        loginViewModel.loginState.loading shouldBeEqualTo false
        loginViewModel.login()
        loginViewModel.loginState.loginEnabled shouldBeEqualTo false
        loginViewModel.loginState.loading shouldBeEqualTo true
        scheduler.advanceUntilIdle()
        loginViewModel.loginState.loginEnabled shouldBeEqualTo true
        loginViewModel.loginState.loading shouldBeEqualTo false
    }

    @Test
    fun `given button is clicked, when login returns Success, then open the web url from the response`() {
        val scheduler = TestCoroutineScheduler()
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        val ssoCode = "wire-fd994b20-b9af-11ec-ae36-00163e9b33ca"
        val param = SSOInitiateLoginUseCase.Param.WithRedirect(ssoCode, loginViewModel.serverConfig)
        val url = "https://wire.com/sso"
        coEvery { ssoInitiateLoginUseCase.invoke(param) } returns SSOInitiateLoginResult.Success(url)
        loginViewModel.onSSOCodeChange(TextFieldValue(ssoCode))

        runTest {
            loginViewModel.login()
            loginViewModel.openWebUrl.first() shouldBe url
        }

        coVerify(exactly = 1) { ssoInitiateLoginUseCase.invoke(param) }
    }

    @Test
    fun `given button is clicked, when login returns InvalidCodeFormat error, then InvalidCodeFormatError is passed`() {
        coEvery { ssoInitiateLoginUseCase.invoke(any()) } returns SSOInitiateLoginResult.Failure.InvalidCodeFormat

        runTest { loginViewModel.login() }

        loginViewModel.loginState.loginSSOError shouldBeInstanceOf LoginError.TextFieldError.InvalidValue::class
    }

    @Test
    fun `given button is clicked, when login returns InvalidCode error, then InvalidCodeError is passed`() {
        coEvery { ssoInitiateLoginUseCase.invoke(any()) } returns SSOInitiateLoginResult.Failure.InvalidCode

        runTest { loginViewModel.login() }

        loginViewModel.loginState.loginSSOError shouldBeInstanceOf LoginError.DialogError.InvalidCodeError::class
    }

    @Test
    fun `given button is clicked, when login returns InvalidRequest error, then GenericError IllegalArgument is passed`() {
        coEvery { ssoInitiateLoginUseCase.invoke(any()) } returns SSOInitiateLoginResult.Failure.InvalidRedirect

        runTest { loginViewModel.login() }

        loginViewModel.loginState.loginSSOError shouldBeInstanceOf LoginError.DialogError.GenericError::class
        with(loginViewModel.loginState.loginSSOError as LoginError.DialogError.GenericError) {
            coreFailure shouldBeInstanceOf CoreFailure.Unknown::class
            with(coreFailure as CoreFailure.Unknown) {
                this.rootCause shouldBeInstanceOf IllegalArgumentException::class
            }
        }
    }

    @Test
    fun `given button is clicked, when login returns Generic error, then GenericError is passed`() {
        val networkFailure = NetworkFailure.NoNetworkConnection(null)
        coEvery { ssoInitiateLoginUseCase.invoke(any()) } returns SSOInitiateLoginResult.Failure.Generic(networkFailure)

        runTest { loginViewModel.login() }

        loginViewModel.loginState.loginSSOError shouldBeInstanceOf LoginError.DialogError.GenericError::class
        (loginViewModel.loginState.loginSSOError as LoginError.DialogError.GenericError).coreFailure shouldBe networkFailure
    }

    @Test
    fun `given establishSSOSession is called, when SSOLogin Success, then SSOLoginResult is passed`() {
        coEvery { getSSOLoginSessionUseCase.invoke(any(), any()) } returns SSOLoginSessionResult.Success(authSession)
        coEvery { addAuthenticatedUserUseCase.invoke(any(), any()) } returns AddAuthenticatedUserUseCase.Result.Success(userId)
        coEvery {
            registerClientUseCase.invoke(any())
        } returns RegisterClientResult.Success(client)

        runTest { loginViewModel.establishSSOSession("") }

        coVerify(exactly = 1) { navigationManager.navigate(any()) }
        coVerify(exactly = 1) { getSSOLoginSessionUseCase.invoke(any(), any()) }
        coVerify(exactly = 1) {
            registerClientUseCase.invoke(any())
        }
        coVerify(exactly = 1) { addAuthenticatedUserUseCase.invoke(any(), any()) }
    }

    @Test
    fun `given establishSSOSession is called, when SSOLoginSessionResult return InvalidCookie, then SSOLoginResult fails`() {
        coEvery { getSSOLoginSessionUseCase.invoke(any(), any()) } returns SSOLoginSessionResult.Failure.InvalidCookie
        coEvery { addAuthenticatedUserUseCase.invoke(any(), any()) } returns AddAuthenticatedUserUseCase.Result.Success(userId)
        coEvery {
            registerClientUseCase.invoke(any())
        } returns RegisterClientResult.Success(client)

        runTest { loginViewModel.establishSSOSession("") }
        loginViewModel.loginState.loginSSOError shouldBeInstanceOf LoginError.DialogError.InvalidSSOCookie::class
        coVerify(exactly = 1) { getSSOLoginSessionUseCase.invoke(any(), any()) }
        coVerify(exactly = 0) { loginViewModel.registerClient(any()) }
        coVerify(exactly = 0) { addAuthenticatedUserUseCase.invoke(any(), any()) }
        coVerify(exactly = 0) { loginViewModel.navigateToConvScreen() }
    }

    @Test
    fun `given HandleSSOResult is called, when ssoResult is null, then loginSSOError state should be none`() {
        runTest { loginViewModel.handleSSOResult(null) }
        loginViewModel.loginState.loginSSOError shouldBeEqualTo LoginError.None
    }

    @Test
    fun `given HandleSSOResult is called, when ssoResult is failure, then loginSSOError state should be dialog error`() {
        runTest { loginViewModel.handleSSOResult(DeepLinkResult.SSOLogin.Failure(SSOFailureCodes.Unknown)) }
        loginViewModel.loginState.loginSSOError shouldBeEqualTo LoginError.DialogError.SSOResultError(SSOFailureCodes.Unknown)
    }

    @Test
    fun `given HandleSSOResult is called, when SSOLoginResult is success, then establishSSOSession should be called once`() {
        coEvery { getSSOLoginSessionUseCase.invoke(any(), any()) } returns SSOLoginSessionResult.Success(authSession)
        coEvery { addAuthenticatedUserUseCase.invoke(any(), any()) } returns AddAuthenticatedUserUseCase.Result.Success(userId)
        coEvery {
            registerClientUseCase.invoke(any())
        } returns RegisterClientResult.Success(client)

        runTest { loginViewModel.handleSSOResult(DeepLinkResult.SSOLogin.Success("", "")) }
        coVerify(exactly = 1) { loginViewModel.navigateToConvScreen() }
    }


    @Test
    fun `given establishSSOSession called, when addAuthenticatedUser returns UserAlreadyExists error, then UserAlreadyExists is passed`() {
        coEvery { getSSOLoginSessionUseCase.invoke(any(), any()) } returns SSOLoginSessionResult.Success(authSession)
        coEvery { addAuthenticatedUserUseCase.invoke(any(), any()) } returns AddAuthenticatedUserUseCase.Result.Failure.UserAlreadyExists

        runTest { loginViewModel.establishSSOSession("") }

        loginViewModel.loginState.loginSSOError shouldBeInstanceOf LoginError.DialogError.UserAlreadyExists::class
        coVerify(exactly = 1) { getSSOLoginSessionUseCase.invoke(any(), any()) }
        coVerify(exactly = 0) { loginViewModel.registerClient(any()) }
        coVerify(exactly = 1) { addAuthenticatedUserUseCase.invoke(any(), any()) }
        coVerify(exactly = 0) { loginViewModel.navigateToConvScreen() }
    }

    @Test
    fun `given establishSSOSession is called, when registerClientUseCase returns TooManyClients error, then TooManyClients is passed`() {
        coEvery { getSSOLoginSessionUseCase.invoke(any(), any()) } returns SSOLoginSessionResult.Success(authSession)
        coEvery { addAuthenticatedUserUseCase.invoke(any(), any()) } returns AddAuthenticatedUserUseCase.Result.Success(userId)
        coEvery {
            registerClientUseCase.invoke(any())
        } returns RegisterClientResult.Failure.TooManyClients

        runTest { loginViewModel.establishSSOSession("") }

        loginViewModel.loginState.loginSSOError shouldBeInstanceOf LoginError.TooManyDevicesError::class

        coVerify(exactly = 1) {
            registerClientUseCase.invoke(any())
        }
        coVerify(exactly = 1) { getSSOLoginSessionUseCase.invoke(any(), any()) }
        coVerify(exactly = 1) { addAuthenticatedUserUseCase.invoke(any(), any()) }
        coVerify(exactly = 0) { loginViewModel.navigateToConvScreen() }
    }
}

