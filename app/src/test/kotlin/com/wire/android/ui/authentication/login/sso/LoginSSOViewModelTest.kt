package com.wire.android.ui.authentication.login.sso

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.mockUri
import com.wire.android.di.AuthServerConfigProvider
import com.wire.android.di.ClientScopeProvider
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.authentication.login.LoginError
import com.wire.android.util.EMPTY
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.android.util.deeplink.SSOFailureCodes
import com.wire.android.util.newServerConfig
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.configuration.server.CommonApiVersionType
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.client.Client
import com.wire.kalium.logic.data.client.ClientType
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.SsoId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.AuthTokens
import com.wire.kalium.logic.feature.auth.AuthenticationScope
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.auth.sso.GetSSOLoginSessionUseCase
import com.wire.kalium.logic.feature.auth.sso.SSOInitiateLoginResult
import com.wire.kalium.logic.feature.auth.sso.SSOInitiateLoginUseCase
import com.wire.kalium.logic.feature.auth.sso.SSOLoginSessionResult
import com.wire.kalium.logic.feature.client.ClientScope
import com.wire.kalium.logic.feature.client.GetOrRegisterClientUseCase
import com.wire.kalium.logic.feature.client.RegisterClientResult
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
    private lateinit var ssoInitiateLoginUseCase: SSOInitiateLoginUseCase

    @MockK
    private lateinit var addAuthenticatedUserUseCase: AddAuthenticatedUserUseCase

    @MockK
    private lateinit var clientScopeProviderFactory: ClientScopeProvider.Factory

    @MockK
    private lateinit var clientScope: ClientScope

    @MockK
    private lateinit var getOrRegisterClientUseCase: GetOrRegisterClientUseCase

    @MockK
    private lateinit var getSSOLoginSessionUseCase: GetSSOLoginSessionUseCase

    @MockK
    private lateinit var authServerConfigProvider: AuthServerConfigProvider

    @MockK
    private lateinit var autoVersionAuthScopeUseCase: AutoVersionAuthScopeUseCase

    @MockK
    private lateinit var authenticationScope: AuthenticationScope

    @MockK
    private lateinit var navigationManager: NavigationManager
    private lateinit var loginViewModel: LoginSSOViewModel

    private val userId: QualifiedID = QualifiedID("userId", "domain")

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        mockUri()
        every { savedStateHandle.get<String>(any()) } returns null
        every { savedStateHandle.set(any(), any<String>()) } returns Unit
        every { clientScopeProviderFactory.create(any()).clientScope } returns clientScope
        every { clientScope.getOrRegister } returns getOrRegisterClientUseCase
        every { authServerConfigProvider.authServer.value } returns newServerConfig(1).links
        coEvery {
            autoVersionAuthScopeUseCase()
        } returns AutoVersionAuthScopeUseCase.Result.Success(
            authenticationScope
        )
        every { authenticationScope.ssoLoginScope.initiate } returns ssoInitiateLoginUseCase
        every { authenticationScope.ssoLoginScope.getLoginSession } returns getSSOLoginSessionUseCase

        loginViewModel = LoginSSOViewModel(
            savedStateHandle,
            autoVersionAuthScopeUseCase,
            addAuthenticatedUserUseCase,
            clientScopeProviderFactory,
            navigationManager,
            authServerConfigProvider
        )
    }

    @Test
    fun `given empty string, when entering code, then button is disabled`() {
        loginViewModel.onSSOCodeChange(TextFieldValue(String.EMPTY))
        loginViewModel.loginState.ssoLoginEnabled shouldBeEqualTo false
        loginViewModel.loginState.ssoLoginLoading shouldBeEqualTo false
    }

    @Test
    fun `given non-empty string, when entering code, then button is enabled`() {
        loginViewModel.onSSOCodeChange(TextFieldValue("abc"))
        loginViewModel.loginState.ssoLoginEnabled shouldBeEqualTo true
        loginViewModel.loginState.ssoLoginLoading shouldBeEqualTo false
    }

    @Test
    fun `given button is clicked, when logging in, then show loading`() {
        val scheduler = TestCoroutineScheduler()
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        coEvery { ssoInitiateLoginUseCase(any()) } returns SSOInitiateLoginResult.Success("")

        loginViewModel.onSSOCodeChange(TextFieldValue("abc"))
        loginViewModel.loginState.ssoLoginEnabled shouldBeEqualTo true
        loginViewModel.loginState.ssoLoginLoading shouldBeEqualTo false
        loginViewModel.login()
        loginViewModel.loginState.ssoLoginEnabled shouldBeEqualTo false
        loginViewModel.loginState.ssoLoginLoading shouldBeEqualTo true
        scheduler.advanceUntilIdle()
        loginViewModel.loginState.ssoLoginEnabled shouldBeEqualTo true
        loginViewModel.loginState.ssoLoginLoading shouldBeEqualTo false
    }

    @Test
    fun `given button is clicked, when login returns Success, then open the web url from the response`() {
        val scheduler = TestCoroutineScheduler()
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        val ssoCode = "wire-fd994b20-b9af-11ec-ae36-00163e9b33ca"
        val param = SSOInitiateLoginUseCase.Param.WithRedirect(ssoCode)
        val url = "https://wire.com/sso"
        coEvery { ssoInitiateLoginUseCase(param) } returns SSOInitiateLoginResult.Success(url)
        loginViewModel.onSSOCodeChange(TextFieldValue(ssoCode))

        runTest {
            loginViewModel.login()
            loginViewModel.openWebUrl.first() shouldBe url
        }

        coVerify(exactly = 1) { ssoInitiateLoginUseCase(param) }
    }

    @Test
    fun `given button is clicked, when login returns InvalidCodeFormat error, then InvalidCodeFormatError is passed`() {
        coEvery { ssoInitiateLoginUseCase(any()) } returns SSOInitiateLoginResult.Failure.InvalidCodeFormat

        runTest { loginViewModel.login() }

        loginViewModel.loginState.loginError shouldBeInstanceOf LoginError.TextFieldError.InvalidValue::class
    }

    @Test
    fun `given button is clicked, when login returns InvalidCode error, then InvalidCodeError is passed`() {
        coEvery { ssoInitiateLoginUseCase(any()) } returns SSOInitiateLoginResult.Failure.InvalidCode

        runTest { loginViewModel.login() }

        loginViewModel.loginState.loginError shouldBeInstanceOf LoginError.DialogError.InvalidCodeError::class
    }

    @Test
    fun `given button is clicked, when login returns InvalidRequest error, then GenericError IllegalArgument is passed`() {
        coEvery { ssoInitiateLoginUseCase(any()) } returns SSOInitiateLoginResult.Failure.InvalidRedirect

        runTest { loginViewModel.login() }

        loginViewModel.loginState.loginError shouldBeInstanceOf LoginError.DialogError.GenericError::class
        with(loginViewModel.loginState.loginError as LoginError.DialogError.GenericError) {
            coreFailure shouldBeInstanceOf CoreFailure.Unknown::class
            with(coreFailure as CoreFailure.Unknown) {
                this.rootCause shouldBeInstanceOf IllegalArgumentException::class
            }
        }
    }

    @Test
    fun `given button is clicked, when login returns Generic error, then GenericError is passed`() {
        val networkFailure = NetworkFailure.NoNetworkConnection(null)
        coEvery { ssoInitiateLoginUseCase(any()) } returns SSOInitiateLoginResult.Failure.Generic(networkFailure)

        runTest { loginViewModel.login() }

        loginViewModel.loginState.loginError shouldBeInstanceOf LoginError.DialogError.GenericError::class
        (loginViewModel.loginState.loginError as LoginError.DialogError.GenericError).coreFailure shouldBe networkFailure
    }

    @Test
    fun `given establishSSOSession is called, when SSOLogin Success, then SSOLoginResult is passed`() {
        coEvery { getSSOLoginSessionUseCase(any()) } returns SSOLoginSessionResult.Success(AUTH_TOKEN, SSO_ID, null)
        coEvery { addAuthenticatedUserUseCase(any(), any(), any(), any()) } returns AddAuthenticatedUserUseCase.Result.Success(userId)
        coEvery {
            getOrRegisterClientUseCase(any())
        } returns RegisterClientResult.Success(CLIENT)

        runTest { loginViewModel.establishSSOSession("", serverConfigId = SERVER_CONFIG.id) }

        coVerify(exactly = 1) { navigationManager.navigate(any()) }
        coVerify(exactly = 1) { getSSOLoginSessionUseCase(any()) }
        coVerify(exactly = 1) {
            getOrRegisterClientUseCase(any())
        }
        coVerify(exactly = 1) { addAuthenticatedUserUseCase(any(), any(), any(), any()) }
    }

    @Test
    fun `given establishSSOSession is called, when SSOLoginSessionResult return InvalidCookie, then SSOLoginResult fails`() {
        coEvery { getSSOLoginSessionUseCase(any()) } returns SSOLoginSessionResult.Failure.InvalidCookie
        coEvery { addAuthenticatedUserUseCase(any(), any(), any(), any()) } returns AddAuthenticatedUserUseCase.Result.Success(userId)
        coEvery { getOrRegisterClientUseCase(any()) } returns RegisterClientResult.Success(CLIENT)

        runTest { loginViewModel.establishSSOSession("", serverConfigId = SERVER_CONFIG.id) }
        loginViewModel.loginState.loginError shouldBeInstanceOf LoginError.DialogError.InvalidSSOCookie::class
        coVerify(exactly = 1) { getSSOLoginSessionUseCase(any()) }
        coVerify(exactly = 0) { loginViewModel.registerClient(any(), null) }
        coVerify(exactly = 0) { addAuthenticatedUserUseCase(any(), any(), any(), any()) }
        coVerify(exactly = 0) { loginViewModel.navigateToConvScreen() }
    }

    @Test
    fun `given HandleSSOResult is called, when ssoResult is null, then loginSSOError state should be none`() {
        runTest { loginViewModel.handleSSOResult(null) }
        loginViewModel.loginState.loginError shouldBeEqualTo LoginError.None
    }

    @Test
    fun `given HandleSSOResult is called, when ssoResult is failure, then loginSSOError state should be dialog error`() {
        runTest { loginViewModel.handleSSOResult(DeepLinkResult.SSOLogin.Failure(SSOFailureCodes.Unknown)) }
        loginViewModel.loginState.loginError shouldBeEqualTo LoginError.DialogError.SSOResultError(SSOFailureCodes.Unknown)
    }

    @Test
    fun `given HandleSSOResult is called, when SSOLoginResult is success, then establishSSOSession should be called once`() {
        coEvery { getSSOLoginSessionUseCase(any()) } returns SSOLoginSessionResult.Success(AUTH_TOKEN, SSO_ID, null)
        coEvery { addAuthenticatedUserUseCase(any(), any(), any(), any()) } returns AddAuthenticatedUserUseCase.Result.Success(userId)
        coEvery { getOrRegisterClientUseCase(any()) } returns RegisterClientResult.Success(CLIENT)

        runTest { loginViewModel.handleSSOResult(DeepLinkResult.SSOLogin.Success("", "")) }
        coVerify(exactly = 1) { loginViewModel.navigateToConvScreen() }
    }

    @Test
    fun `given establishSSOSession called, when addAuthenticatedUser returns UserAlreadyExists error, then UserAlreadyExists is passed`() {
        coEvery { getSSOLoginSessionUseCase(any()) } returns SSOLoginSessionResult.Success(AUTH_TOKEN, SSO_ID, null)
        coEvery {
            addAuthenticatedUserUseCase(
                any(),
                any(),
                any(),
                any()
            )
        } returns AddAuthenticatedUserUseCase.Result.Failure.UserAlreadyExists

        runTest { loginViewModel.establishSSOSession("", serverConfigId = SERVER_CONFIG.id) }

        loginViewModel.loginState.loginError shouldBeInstanceOf LoginError.DialogError.UserAlreadyExists::class
        coVerify(exactly = 1) { getSSOLoginSessionUseCase(any()) }
        coVerify(exactly = 0) { loginViewModel.registerClient(any(), null) }
        coVerify(exactly = 1) { addAuthenticatedUserUseCase(any(), any(), any(), any()) }
        coVerify(exactly = 0) { loginViewModel.navigateToConvScreen() }
    }

    @Test
    fun `given establishSSOSession is called, when getOrRegister Client returns TooManyClients error, then TooManyClients is passed`() {
        coEvery { getSSOLoginSessionUseCase(any()) } returns SSOLoginSessionResult.Success(AUTH_TOKEN, SSO_ID, null)
        coEvery { addAuthenticatedUserUseCase(any(), any(), any(), any()) } returns AddAuthenticatedUserUseCase.Result.Success(userId)
        coEvery {
            getOrRegisterClientUseCase(any())
        } returns RegisterClientResult.Failure.TooManyClients

        runTest { loginViewModel.establishSSOSession("", serverConfigId = SERVER_CONFIG.id) }

        loginViewModel.loginState.loginError shouldBeInstanceOf LoginError.TooManyDevicesError::class

        coVerify(exactly = 1) { getOrRegisterClientUseCase(any()) }
        coVerify(exactly = 1) { getSSOLoginSessionUseCase(any()) }
        coVerify(exactly = 1) { addAuthenticatedUserUseCase(any(), any(), any(), any()) }
        coVerify(exactly = 0) { loginViewModel.navigateToConvScreen() }
    }

    @Test
    fun `given establishSSOSession is called, when registerTokenUseCase returns PushTokenFailure error, then LoginError is None`() {
        coEvery { getSSOLoginSessionUseCase(any()) } returns SSOLoginSessionResult.Success(AUTH_TOKEN, SSO_ID, null)
        coEvery { addAuthenticatedUserUseCase(any(), any(), any(), any()) } returns AddAuthenticatedUserUseCase.Result.Success(userId)
        coEvery {
            getOrRegisterClientUseCase(any())
        } returns RegisterClientResult.Success(CLIENT)

        runTest { loginViewModel.establishSSOSession("", serverConfigId = SERVER_CONFIG.id) }

        loginViewModel.loginState.loginError shouldBeInstanceOf LoginError.None::class

        coVerify(exactly = 1) { navigationManager.navigate(any()) }
        coVerify(exactly = 1) { getSSOLoginSessionUseCase(any()) }
        coVerify(exactly = 1) {
            getOrRegisterClientUseCase(any())
        }
        coVerify(exactly = 1) { addAuthenticatedUserUseCase(any(), any(), any(), any()) }
    }

    companion object {
        val CLIENT_ID = ClientId("test")
        val CLIENT = Client(
            CLIENT_ID, ClientType.Permanent, "time", null,
            null, "label", "cookie", null, "model"
        )
        val SSO_ID: SsoId = SsoId("scim_id", null, null)
        val AUTH_TOKEN = AuthTokens(
            userId = UserId("user_id", "domain"),
            accessToken = "access_token",
            refreshToken = "refresh_token",
            tokenType = "token_type"
        )
        val SERVER_CONFIG = ServerConfig(
            id = "config",
            links = ServerConfig.Links(
                api = "https://server-apiBaseUrl.de",
                accounts = "https://server-accountBaseUrl.de",
                webSocket = "https://server-webSocketBaseUrl.de",
                blackList = "https://server-blackListUrl.de",
                teams = "https://server-teamsUrl.de",
                website = "https://server-websiteUrl.de",
                title = "server-title",
                false,
                apiProxy = null
            ),
            metaData = ServerConfig.MetaData(
                commonApiVersion = CommonApiVersionType.Valid(1),
                domain = "domain.com",
                federation = false
            )
        )
    }
}
