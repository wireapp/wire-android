package com.wire.android.ui.newauthentication.login

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.config.SnapshotExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.ClientScopeProvider
import com.wire.android.framework.TestClient
import com.wire.android.ui.authentication.login.DomainClaimedByOrg
import com.wire.android.ui.authentication.login.LoginNavArgs
import com.wire.android.ui.authentication.login.LoginPasswordPath
import com.wire.android.ui.authentication.login.LoginViewModelExtension
import com.wire.android.ui.authentication.login.sso.LoginSSOViewModelExtension
import com.wire.android.ui.authentication.login.sso.SSOUrlConfig
import com.wire.android.ui.navArgs
import com.wire.android.ui.newauthentication.login.ValidateEmailOrSSOCodeUseCase.Result.ValidEmail
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.android.util.deeplink.SSOFailureCodes
import com.wire.android.util.newServerConfig
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.AuthenticationScope
import com.wire.kalium.logic.feature.auth.EnterpriseLoginResult
import com.wire.kalium.logic.feature.auth.LoginRedirectPath
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.auth.sso.FetchSSOSettingsUseCase
import com.wire.kalium.logic.feature.auth.sso.SSOInitiateLoginResult
import com.wire.kalium.logic.feature.auth.sso.SSOLoginSessionResult
import com.wire.kalium.logic.feature.auth.sso.ValidateSSOCodeUseCase.Companion.SSO_CODE_WIRE_PREFIX
import com.wire.kalium.logic.feature.client.RegisterClientResult
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class, SnapshotExtension::class, NavigationTestExtension::class)
class NewLoginViewModelTest {
    private val dispatchers = TestDispatcherProvider()

    @Test
    fun `given onLoginStarted is called, when valid input is SSO, then proceed to SSO flow`() = runTest(dispatchers.main()) {
        val (arrangement, sut) = Arrangement()
            .withEmailOrSSOCodeValidatorReturning(ValidateEmailOrSSOCodeUseCase.Result.ValidSSOCode)
            .arrange()

        sut.onLoginStarted(action = arrangement.action)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            arrangement.loginSSOViewModelExtension.initiateSSO(any(), any(), any(), any(), any())
        }
        coVerify(exactly = 0) {
            arrangement.authenticationScope.getLoginFlowForDomainUseCase(any())
        }
    }

    @Test
    fun `given onLoginStarted is called, when valid input is email, then proceed to enterprise flow`() = runTest(dispatchers.main()) {
        val (arrangement, sut) = Arrangement()
            .withEmailOrSSOCodeValidatorReturning(ValidateEmailOrSSOCodeUseCase.Result.ValidEmail)
            .withAuthenticationScopeSuccess()
            .withGetLoginFlowForDomainReturning(EnterpriseLoginResult.Success(LoginRedirectPath.Default))
            .arrange()

        sut.onLoginStarted(action = arrangement.action)
        advanceUntilIdle()

        coVerify(exactly = 0) {
            arrangement.loginSSOViewModelExtension.initiateSSO(any(), any(), any(), any(), any())
        }
        coVerify(exactly = 1) {
            arrangement.authenticationScope.getLoginFlowForDomainUseCase(any())
        }
    }

    @Test
    fun `given onLoginStarted is called, when invalid input, then update error state`() = runTest(dispatchers.main()) {
        val (arrangement, sut) = Arrangement()
            .withEmailOrSSOCodeValidatorReturning(ValidateEmailOrSSOCodeUseCase.Result.InvalidInput)
            .arrange()

        sut.onLoginStarted(action = arrangement.action)

        coVerify(exactly = 0) {
            arrangement.loginSSOViewModelExtension.initiateSSO(any(), any(), any(), any(), any())
        }
        coVerify(exactly = 0) {
            arrangement.authenticationScope.getLoginFlowForDomainUseCase(any())
        }
        verify(exactly = 0) {
            arrangement.action(any())
        }
        assertEquals(NewLoginFlowState.Error.TextFieldError.InvalidValue, sut.state.flowState)
    }

    @Test
    fun `given success, when initiating SSO, then call SSO action with url`() = runTest(dispatchers.main()) {
        val redirectUrl = "https://redirect.url"
        val config = SSOUrlConfig(newServerConfig(1).links, SSO_CODE_WITH_PREFIX)
        val (arrangement, sut) = Arrangement()
            .withEmailOrSSOCodeValidatorReturning(ValidateEmailOrSSOCodeUseCase.Result.ValidSSOCode)
            .withInitiateSSOSuccess(redirectUrl, config.serverConfig)
            .arrange()

        sut.initiateSSO(config.serverConfig, config.ssoCode, arrangement.action)
        advanceUntilIdle()

        verify(exactly = 1) {
            arrangement.action(eq(NewLoginAction.SSO(redirectUrl, config)))
        }
    }

    @Test
    fun `given failure, when initiating SSO, then update error state`() = runTest(dispatchers.main()) {
        val serverConfig = newServerConfig(1).links
        val (arrangement, sut) = Arrangement()
            .withEmailOrSSOCodeValidatorReturning(ValidateEmailOrSSOCodeUseCase.Result.ValidSSOCode)
            .withInitiateSSOFailure(SSOInitiateLoginResult.Failure.InvalidCode)
            .arrange()

        sut.initiateSSO(serverConfig, SSO_CODE_WITH_PREFIX, arrangement.action)
        advanceUntilIdle()

        verify(exactly = 0) {
            arrangement.action(any())
        }
        assertEquals(NewLoginFlowState.Error.DialogError.InvalidSSOCode, sut.state.flowState)
    }

    @Test
    fun `given auth scope failure, when initiating SSO, then update error state`() = runTest(dispatchers.main()) {
        val serverConfig = newServerConfig(1).links
        val (arrangement, sut) = Arrangement()
            .withEmailOrSSOCodeValidatorReturning(ValidateEmailOrSSOCodeUseCase.Result.ValidSSOCode)
            .withInitiateSSOAuthScopeFailure(AutoVersionAuthScopeUseCase.Result.Failure.UnknownServerVersion)
            .arrange()

        sut.initiateSSO(serverConfig, SSO_CODE_WITH_PREFIX, arrangement.action)
        advanceUntilIdle()

        verify(exactly = 0) {
            arrangement.action(any())
        }
        assertEquals(NewLoginFlowState.Error.DialogError.ServerVersionNotSupported, sut.state.flowState)
    }

    @Test
    fun `given default SSO code, when confirming custom config dialog, then initiate SSO with that code`() = runTest(dispatchers.main()) {
        val serverConfig = newServerConfig(1).links
        val ssoCode = SSO_CODE_WITH_PREFIX
        val (arrangement, sut) = Arrangement()
            .withFetchDefaultSSOCodeSuccess(ssoCode)
            .arrange()

        sut.onCustomServerDialogConfirm(serverConfig, arrangement.action)
        advanceUntilIdle()

        verify(exactly = 0) {
            arrangement.action(any())
        }
        coVerify(exactly = 1) {
            arrangement.loginSSOViewModelExtension.initiateSSO(serverConfig, ssoCode, any(), any(), any())
        }
    }

    @Test
    fun `given no default SSO code, when confirming custom config dialog, then call CustomConfig action`() = runTest(dispatchers.main()) {
        val serverConfig = newServerConfig(1).links
        val (arrangement, sut) = Arrangement()
            .withFetchDefaultSSOCodeSuccess(null)
            .arrange()

        sut.onCustomServerDialogConfirm(serverConfig, arrangement.action)
        advanceUntilIdle()

        verify(exactly = 1) {
            arrangement.action(
                match {
                    it is NewLoginAction.CustomConfig && it.customServerConfig == serverConfig
                }
            )
        }
        coVerify(exactly = 0) {
            arrangement.loginSSOViewModelExtension.initiateSSO(serverConfig, any(), any(), any(), any())
        }
    }

    @Test
    fun `given auth scope failure, when confirming custom config dialog, then update error state`() = runTest(dispatchers.main()) {
        val serverConfig = newServerConfig(1).links
        val (arrangement, sut) = Arrangement()
            .withFetchDefaultSSOCodeAuthScopeFailure(AutoVersionAuthScopeUseCase.Result.Failure.UnknownServerVersion)
            .arrange()

        sut.onCustomServerDialogConfirm(serverConfig, arrangement.action)
        advanceUntilIdle()

        verify(exactly = 0) {
            arrangement.action(any())
        }
        assertEquals(NewLoginFlowState.Error.DialogError.ServerVersionNotSupported, sut.state.flowState)
    }

    @Test
    fun `given fetch default SSO failure, when confirming custom config dialog, then update error state`() = runTest(dispatchers.main()) {
        val serverConfig = newServerConfig(1).links
        val failure = CoreFailure.Unknown(RuntimeException("Error!"))
        val (arrangement, sut) = Arrangement()
            .withFetchDefaultSSOCodeFailure(FetchSSOSettingsUseCase.Result.Failure(failure))
            .arrange()

        sut.onCustomServerDialogConfirm(serverConfig, arrangement.action)
        advanceUntilIdle()

        verify(exactly = 0) {
            arrangement.action(any())
        }
        assertEquals(NewLoginFlowState.Error.DialogError.GenericError(failure), sut.state.flowState)
    }

    @Test
    fun `given SSO session established, when handling SSO result, then register client`() = runTest(dispatchers.main()) {
        val ssoDeepLinkResult = DeepLinkResult.SSOLogin.Success("cookie", "server-config-id")
        val config = SSOUrlConfig(newServerConfig(1).links, SSO_CODE_WITH_PREFIX)
        val userId = UserId("user-id", "domain")
        val (arrangement, sut) = Arrangement()
            .withEstablishSSOSessionSuccess(userId)
            .withRegisterClientReturning(RegisterClientResult.Success(TestClient.CLIENT))
            .withIsInitialSyncCompletedReturning(true)
            .arrange()

        sut.handleSSOResult(ssoDeepLinkResult, config, arrangement.action)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            arrangement.loginViewModelExtension.registerClient(userId, any(), any(), any())
        }
    }

    @Test
    fun `given auth scope failure, when handling SSO result, then update error state`() = runTest(dispatchers.main()) {
        val ssoDeepLinkResult = DeepLinkResult.SSOLogin.Success("cookie", "server-config-id")
        val config = SSOUrlConfig(newServerConfig(1).links, SSO_CODE_WITH_PREFIX)
        val (arrangement, sut) = Arrangement()
            .withEstablishSSOSessionAuthScopeFailure(AutoVersionAuthScopeUseCase.Result.Failure.UnknownServerVersion)
            .arrange()

        sut.handleSSOResult(ssoDeepLinkResult, config, arrangement.action)
        advanceUntilIdle()

        verify(exactly = 0) {
            arrangement.action(any())
        }
        assertEquals(NewLoginFlowState.Error.DialogError.ServerVersionNotSupported, sut.state.flowState)
    }

    @Test
    fun `given SSO login failure, when handling SSO result, then update error state`() = runTest(dispatchers.main()) {
        val ssoDeepLinkResult = DeepLinkResult.SSOLogin.Success("cookie", "server-config-id")
        val config = SSOUrlConfig(newServerConfig(1).links, SSO_CODE_WITH_PREFIX)
        val (arrangement, sut) = Arrangement()
            .withEstablishSSOSessionLoginFailure(SSOLoginSessionResult.Failure.InvalidCookie)
            .arrange()

        sut.handleSSOResult(ssoDeepLinkResult, config, arrangement.action)
        advanceUntilIdle()

        verify(exactly = 0) {
            arrangement.action(any())
        }
        assertEquals(NewLoginFlowState.Error.DialogError.InvalidSSOCookie, sut.state.flowState)
    }

    @Test
    fun `given add user failure, when handling SSO result, then update error state`() = runTest(dispatchers.main()) {
        val ssoDeepLinkResult = DeepLinkResult.SSOLogin.Success("cookie", "server-config-id")
        val config = SSOUrlConfig(newServerConfig(1).links, SSO_CODE_WITH_PREFIX)
        val (arrangement, sut) = Arrangement()
            .withEstablishSSOSessionAddUserFailure(AddAuthenticatedUserUseCase.Result.Failure.UserAlreadyExists)
            .arrange()

        sut.handleSSOResult(ssoDeepLinkResult, config, arrangement.action)
        advanceUntilIdle()

        verify(exactly = 0) {
            arrangement.action(any())
        }
        assertEquals(NewLoginFlowState.Error.DialogError.UserAlreadyExists, sut.state.flowState)
    }

    private fun testHandleSSOResultRegisterClientResults(
        result: RegisterClientResult,
        expectedNextStep: NewLoginAction.Success.NextStep,
        isInitialSyncCompleted: Boolean = true,
    ) = runTest(dispatchers.main()) {
        val ssoDeepLinkResult = DeepLinkResult.SSOLogin.Success("cookie", "server-config-id")
        val config = SSOUrlConfig(newServerConfig(1).links, SSO_CODE_WITH_PREFIX)
        val userId = UserId("user-id", "domain")
        val (arrangement, sut) = Arrangement()
            .withEstablishSSOSessionSuccess(userId)
            .withRegisterClientReturning(result)
            .withIsInitialSyncCompletedReturning(isInitialSyncCompleted)
            .arrange()

        sut.handleSSOResult(ssoDeepLinkResult, config, arrangement.action)
        advanceUntilIdle()

        verify(exactly = 1) { arrangement.action(NewLoginAction.Success(expectedNextStep)) }
    }

    @Test
    fun `given client registered and initial sync completed, when handling SSO result, then call NextStep-None action`() =
        testHandleSSOResultRegisterClientResults(
            result = RegisterClientResult.Success(TestClient.CLIENT),
            isInitialSyncCompleted = true,
            expectedNextStep = NewLoginAction.Success.NextStep.None,
        )

    @Test
    fun `given client registered and initial sync not completed, when handling SSO result, then call NextStep-InitialSync action`() =
        testHandleSSOResultRegisterClientResults(
            result = RegisterClientResult.Success(TestClient.CLIENT),
            isInitialSyncCompleted = false,
            expectedNextStep = NewLoginAction.Success.NextStep.InitialSync,
        )

    @Test
    fun `given client registered and E2EI required, when handling SSO result, then call NextStep-E2EIEnrollment action`() =
        testHandleSSOResultRegisterClientResults(
            result = RegisterClientResult.E2EICertificateRequired(TestClient.CLIENT, UserId("user-id", "domain")),
            expectedNextStep = NewLoginAction.Success.NextStep.E2EIEnrollment,
        )

    @Test
    fun `given too many clients, when handling SSO result, then call NextStep-TooManyDevices action`() =
        testHandleSSOResultRegisterClientResults(
            result = RegisterClientResult.Failure.TooManyClients,
            expectedNextStep = NewLoginAction.Success.NextStep.TooManyDevices,
        )

    @Test
    fun `given register client other failure, when handling SSO result, then update error state`() = runTest(dispatchers.main()) {
        val ssoDeepLinkResult = DeepLinkResult.SSOLogin.Success("cookie", "server-config-id")
        val config = SSOUrlConfig(newServerConfig(1).links, SSO_CODE_WITH_PREFIX)
        val failure = CoreFailure.Unknown(RuntimeException("Error!"))
        val (arrangement, sut) = Arrangement()
            .withEstablishSSOSessionSuccess(UserId("user-id", "domain"))
            .withRegisterClientReturning(RegisterClientResult.Failure.Generic(failure))
            .arrange()

        sut.handleSSOResult(ssoDeepLinkResult, config, arrangement.action)
        advanceUntilIdle()

        verify(exactly = 0) {
            arrangement.action(any())
        }
        assertEquals(NewLoginFlowState.Error.DialogError.GenericError(failure), sut.state.flowState)
    }

    private fun testCustomConfigWhenHandlingSSOResult(config: SSOUrlConfig?) = runTest(dispatchers.main()) {
        val ssoDeepLinkResult = DeepLinkResult.SSOLogin.Success("cookie", "server-config-id")
        val defaultConfig = newServerConfig(1).links
        val expectedConfig = config?.serverConfig ?: defaultConfig
        val failure = CoreFailure.Unknown(RuntimeException("Error!"))
        val (arrangement, sut) = Arrangement()
            .withNavArgsServerConfig(defaultConfig)
            .withEstablishSSOSessionSuccess(UserId("user-id", "domain"))
            .withRegisterClientReturning(RegisterClientResult.Failure.Generic(failure))
            .arrange()

        sut.handleSSOResult(ssoDeepLinkResult, config, arrangement.action)
        advanceUntilIdle()

        coVerify {
            arrangement.loginSSOViewModelExtension.establishSSOSession(any(), any(), expectedConfig, any(), any(), any(), any())
        }
    }

    @Test
    fun `given custom config passed, when handling SSO result, then use custom config`() =
        testCustomConfigWhenHandlingSSOResult(SSOUrlConfig(newServerConfig(2).links, SSO_CODE_WITH_PREFIX))

    @Test
    fun `given no custom config passed, when handling SSO result, then use default config`() =
        testCustomConfigWhenHandlingSSOResult(null)

    @Test
    fun `given SSO result failure, when handling SSO result, then update error state`() = runTest(dispatchers.main()) {
        val ssoDeepLinkResult = DeepLinkResult.SSOLogin.Failure(SSOFailureCodes.NotFound)
        val config = SSOUrlConfig(newServerConfig(2).links, SSO_CODE_WITH_PREFIX)
        val failure = CoreFailure.Unknown(RuntimeException("Error!"))
        val (arrangement, sut) = Arrangement()
            .withEstablishSSOSessionSuccess(UserId("user-id", "domain"))
            .withRegisterClientReturning(RegisterClientResult.Failure.Generic(failure))
            .arrange()

        sut.handleSSOResult(ssoDeepLinkResult, config, arrangement.action)
        advanceUntilIdle()

        verify(exactly = 0) {
            arrangement.action(any())
        }
        assertEquals(NewLoginFlowState.Error.DialogError.SSOResultFailure(ssoDeepLinkResult.ssoError), sut.state.flowState)
    }

    private val email: String = "email@wire.com"
    private fun testEnterpriseLoginActions(result: EnterpriseLoginResult, expected: NewLoginAction) = runTest(dispatchers.main()) {
        val (arrangement, sut) = Arrangement()
            .withAuthenticationScopeSuccess()
            .withGetLoginFlowForDomainReturning(result)
            .arrange()

        sut.getEnterpriseLoginFlow(email, arrangement.action)
        advanceUntilIdle()

        verify(exactly = 1) {
            arrangement.action(expected)
        }
    }

    @Test
    fun `given not supported failure, when enterprise login, then call EnterpriseLoginNotSupported action`() =
        testEnterpriseLoginActions(
            result = EnterpriseLoginResult.Failure.NotSupported,
            expected = NewLoginAction.EnterpriseLoginNotSupported(email),
        )

    @Test
    fun `given default path, when enterprise login, then call EmailPassword action with creation`() =
        testEnterpriseLoginActions(
            result = EnterpriseLoginResult.Success(LoginRedirectPath.Default),
            expected = NewLoginAction.EmailPassword(email, LoginPasswordPath(isCloudAccountCreationPossible = true)),
        )

    @Test
    fun `given no registration path, when enterprise login, then call EmailPassword action with no creation`() =
        testEnterpriseLoginActions(
            result = EnterpriseLoginResult.Success(LoginRedirectPath.NoRegistration),
            expected = NewLoginAction.EmailPassword(email, LoginPasswordPath(isCloudAccountCreationPossible = false)),
        )

    @Test
    fun `given existing account path, when enterprise login, then call EmailPassword action with no creation and claimed domain flag`() =
        testEnterpriseLoginActions(
            result = EnterpriseLoginResult.Success(LoginRedirectPath.ExistingAccountWithClaimedDomain("claimed-domain")),
            expected = NewLoginAction.EmailPassword(
                userIdentifier = email,
                loginPasswordPath = LoginPasswordPath(
                    isCloudAccountCreationPossible = false,
                    isDomainClaimedByOrg = DomainClaimedByOrg.Claimed("claimed-domain"),
                )
            ),
        )

    @Test
    fun `given SSO path & code with prefix, when enterprise login, then initiate SSO with given SSO code`() = runTest(dispatchers.main()) {
        val ssoCode = SSO_CODE_WITH_PREFIX
        val (arrangement, sut) = Arrangement()
            .withAuthenticationScopeSuccess()
            .withGetLoginFlowForDomainReturning(EnterpriseLoginResult.Success(LoginRedirectPath.SSO(ssoCode)))
            .arrange()

        sut.getEnterpriseLoginFlow(email, arrangement.action)
        advanceUntilIdle()

        verify(exactly = 0) {
            arrangement.action(any())
        }
        coVerify(exactly = 1) {
            arrangement.loginSSOViewModelExtension.initiateSSO(any(), ssoCode, any(), any(), any())
        }
    }

    @Test
    fun `given SSO path & code without prefix, when enterprise login, then initiate SSO with given SSO code with prefix`() =
        runTest(dispatchers.main()) {
            val ssoCodeWithoutPrefix = SSO_CODE_WITHOUT_PREFIX
            val ssoCodeWithPrefix = SSO_CODE_WITH_PREFIX
            val (arrangement, sut) = Arrangement()
                .withAuthenticationScopeSuccess()
                .withGetLoginFlowForDomainReturning(EnterpriseLoginResult.Success(LoginRedirectPath.SSO(ssoCodeWithoutPrefix)))
                .arrange()
    
            sut.getEnterpriseLoginFlow(email, arrangement.action)
            advanceUntilIdle()
    
            verify(exactly = 0) {
                arrangement.action(any())
            }
            coVerify(exactly = 1) {
                arrangement.loginSSOViewModelExtension.initiateSSO(any(), ssoCodeWithPrefix, any(), any(), any())
            }
        }

    @Test
    fun `given custom backend path, when enterprise login, then update custom backend state`() = runTest(dispatchers.main()) {
        val customServerConfig: ServerConfig.Links = newServerConfig(2).links
        val (arrangement, sut) = Arrangement()
            .withAuthenticationScopeSuccess()
            .withGetLoginFlowForDomainReturning(EnterpriseLoginResult.Success(LoginRedirectPath.CustomBackend(customServerConfig)))
            .arrange()

        sut.getEnterpriseLoginFlow(email, arrangement.action)
        advanceUntilIdle()

        verify(exactly = 0) {
            arrangement.action(any())
        }
        assertEquals(NewLoginFlowState.CustomConfigDialog(customServerConfig), sut.state.flowState)
    }

    @Test
    fun `given failure, when enterprise login, then update error state`() = runTest(dispatchers.main()) {
        val failure = CoreFailure.Unknown(RuntimeException("Error!"))
        val (arrangement, sut) = Arrangement()
            .withAuthenticationScopeSuccess()
            .withGetLoginFlowForDomainReturning(EnterpriseLoginResult.Failure.Generic(failure))
            .arrange()

        sut.getEnterpriseLoginFlow(email, arrangement.action)
        advanceUntilIdle()

        verify(exactly = 0) {
            arrangement.action(any())
        }
        assertEquals(NewLoginFlowState.Error.DialogError.GenericError(failure), sut.state.flowState)
    }

    @Test
    fun `given auth scope failure, when enterprise login, then update error state`() = runTest(dispatchers.main()) {
        val (arrangement, sut) = Arrangement()
            .withAuthenticationScopeFailure(AutoVersionAuthScopeUseCase.Result.Failure.UnknownServerVersion)
            .arrange()

        sut.getEnterpriseLoginFlow(email, arrangement.action)
        advanceUntilIdle()

        verify(exactly = 0) {
            arrangement.action(any())
        }
        assertEquals(NewLoginFlowState.Error.DialogError.ServerVersionNotSupported, sut.state.flowState)
    }

    inner class Arrangement {
        @MockK
        lateinit var coreLogic: CoreLogic

        @MockK
        lateinit var loginViewModelExtension: LoginViewModelExtension

        @MockK
        lateinit var addAuthenticatedUserUseCase: AddAuthenticatedUserUseCase

        @MockK
        lateinit var loginSSOViewModelExtension: LoginSSOViewModelExtension

        @MockK
        private lateinit var savedStateHandle: SavedStateHandle

        @MockK
        private lateinit var clientScopeProviderFactory: ClientScopeProvider.Factory

        @MockK
        private lateinit var userDataStoreProvider: UserDataStoreProvider

        @MockK
        lateinit var authenticationScope: AuthenticationScope

        val validateEmailOrSSOCodeUseCase: ValidateEmailOrSSOCodeUseCase = mockk()

        val action: (NewLoginAction) -> Unit = mockk()

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            every {
                savedStateHandle.get<String>(any())
            } returns null
            every {
                savedStateHandle[any()] = any<String>()
            } returns Unit
            every {
                action(any())
            } returns Unit
        }

        fun withNavArgsServerConfig(serverConfig: ServerConfig.Links) = apply {
            every {
                savedStateHandle.navArgs<LoginNavArgs>()
            } returns LoginNavArgs(loginPasswordPath = LoginPasswordPath(serverConfig))
        }

        fun withEmailOrSSOCodeValidatorReturning(result: ValidateEmailOrSSOCodeUseCase.Result = ValidEmail) = apply {
            every {
                validateEmailOrSSOCodeUseCase(any())
            } returns result
        }

        fun withGetLoginFlowForDomainReturning(result: EnterpriseLoginResult) = apply {
            coEvery {
                authenticationScope.getLoginFlowForDomainUseCase(any())
            } returns result
        }

        fun withRegisterClientReturning(result: RegisterClientResult) = apply {
            coEvery {
                loginViewModelExtension.registerClient(any(), any(), any(), any())
            } returns result
        }

        fun withIsInitialSyncCompletedReturning(result: Boolean) = apply {
            coEvery {
                loginViewModelExtension.isInitialSyncCompleted(any())
            } returns result
        }

        fun withAuthenticationScopeFailure(failure: AutoVersionAuthScopeUseCase.Result.Failure) = apply {
            coEvery {
                loginSSOViewModelExtension.withAuthenticationScope(any(), any(), any())
            } coAnswers {
                arg<(AutoVersionAuthScopeUseCase.Result.Failure) -> Unit>(1)(failure)
            }
        }

        fun withAuthenticationScopeSuccess(authScope: AuthenticationScope = authenticationScope) = apply {
            coEvery {
                loginSSOViewModelExtension.withAuthenticationScope(any(), any(), any())
            } coAnswers {
                arg<suspend (AuthenticationScope) -> Unit>(2)(authScope)
            }
        }

        fun withInitiateSSOAuthScopeFailure(failure: AutoVersionAuthScopeUseCase.Result.Failure) = apply {
            coEvery {
                loginSSOViewModelExtension.initiateSSO(any(), any(), any(), any(), any())
            } coAnswers {
                arg<(AutoVersionAuthScopeUseCase.Result.Failure) -> Unit>(2)(failure)
            }
        }

        fun withInitiateSSOFailure(failure: SSOInitiateLoginResult.Failure) = apply {
            coEvery {
                loginSSOViewModelExtension.initiateSSO(any(), any(), any(), any(), any())
            } coAnswers {
                arg<(SSOInitiateLoginResult.Failure) -> Unit>(3)(failure)
            }
        }

        fun withInitiateSSOSuccess(url: String, serverConfig: ServerConfig.Links) = apply {
            coEvery {
                loginSSOViewModelExtension.initiateSSO(any(), any(), any(), any(), any())
            } coAnswers {
                arg<suspend (String, ServerConfig.Links) -> Unit>(4)(url, serverConfig)
            }
        }

        fun withFetchDefaultSSOCodeAuthScopeFailure(failure: AutoVersionAuthScopeUseCase.Result.Failure) = apply {
            coEvery {
                loginSSOViewModelExtension.fetchDefaultSSOCode(any(), any(), any(), any())
            } coAnswers {
                arg<(AutoVersionAuthScopeUseCase.Result.Failure) -> Unit>(1)(failure)
            }
        }

        fun withFetchDefaultSSOCodeFailure(failure: FetchSSOSettingsUseCase.Result.Failure) = apply {
            coEvery {
                loginSSOViewModelExtension.fetchDefaultSSOCode(any(), any(), any(), any())
            } coAnswers {
                arg<(FetchSSOSettingsUseCase.Result.Failure) -> Unit>(2)(failure)
            }
        }

        fun withFetchDefaultSSOCodeSuccess(defaultSSOCode: String?) = apply {
            coEvery {
                loginSSOViewModelExtension.fetchDefaultSSOCode(any(), any(), any(), any())
            } coAnswers {
                arg<suspend (String?) -> Unit>(3)(defaultSSOCode)
            }
        }

        fun withEstablishSSOSessionAuthScopeFailure(failure: AutoVersionAuthScopeUseCase.Result.Failure) = apply {
            coEvery {
                loginSSOViewModelExtension.establishSSOSession(any(), any(), any(), any(), any(), any(), any())
            } coAnswers {
                arg<(AutoVersionAuthScopeUseCase.Result.Failure) -> Unit>(3)(failure)
            }
        }

        fun withEstablishSSOSessionLoginFailure(failure: SSOLoginSessionResult.Failure) = apply {
            coEvery {
                loginSSOViewModelExtension.establishSSOSession(any(), any(), any(), any(), any(), any(), any())
            } coAnswers {
                arg<(SSOLoginSessionResult.Failure) -> Unit>(4)(failure)
            }
        }

        fun withEstablishSSOSessionAddUserFailure(failure: AddAuthenticatedUserUseCase.Result.Failure) = apply {
            coEvery {
                loginSSOViewModelExtension.establishSSOSession(any(), any(), any(), any(), any(), any(), any())
            } coAnswers {
                arg<(AddAuthenticatedUserUseCase.Result.Failure) -> Unit>(5)(failure)
            }
        }

        fun withEstablishSSOSessionSuccess(userId: UserId) = apply {
            coEvery {
                loginSSOViewModelExtension.establishSSOSession(any(), any(), any(), any(), any(), any(), any())
            } coAnswers {
                arg<suspend (UserId) -> Unit>(6)(userId)
            }
        }

        fun arrange() = this to NewLoginViewModel(
            validateEmailOrSSOCodeUseCase,
            coreLogic,
            savedStateHandle,
            addAuthenticatedUserUseCase,
            clientScopeProviderFactory,
            userDataStoreProvider,
            loginViewModelExtension,
            loginSSOViewModelExtension,
            dispatchers
        )
    }
    
    companion object {
        private const val SSO_CODE_WITHOUT_PREFIX: String = "fd994b20-b9af-11ec-ae36-00163e9b33ca"
        private const val SSO_CODE_WITH_PREFIX: String = "$SSO_CODE_WIRE_PREFIX$SSO_CODE_WITHOUT_PREFIX"
    }
}
