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

package com.wire.android.ui.authentication.login.sso

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.SnapshotExtension
import com.wire.android.config.mockUri
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.AuthServerConfigProvider
import com.wire.android.di.ClientScopeProvider
import com.wire.android.framework.TestClient
import com.wire.android.ui.authentication.login.LoginState
import com.wire.android.ui.common.dialogs.CustomServerDialogState
import com.wire.android.util.EMPTY
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.android.util.deeplink.SSOFailureCodes
import com.wire.android.util.newServerConfig
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.configuration.server.CommonApiVersionType
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.auth.AccountTokens
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.SsoId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.AuthenticationScope
import com.wire.kalium.logic.feature.auth.DomainLookupUseCase
import com.wire.kalium.logic.feature.auth.ValidateEmailUseCase
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.auth.sso.FetchSSOSettingsUseCase
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldNotBeInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class, SnapshotExtension::class)
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

    private val authServerConfigProvider: AuthServerConfigProvider = AuthServerConfigProvider()

    @MockK
    private lateinit var userDataStoreProvider: UserDataStoreProvider

    @MockK
    private lateinit var autoVersionAuthScopeUseCase: AutoVersionAuthScopeUseCase

    @MockK
    private lateinit var coreLogic: CoreLogic

    @MockK
    private lateinit var authenticationScope: AuthenticationScope

    @MockK
    private lateinit var validateEmailUseCase: ValidateEmailUseCase

    @MockK
    private lateinit var fetchSSOSettings: FetchSSOSettingsUseCase

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

        authServerConfigProvider.updateAuthServer(newServerConfig(1).links)

        coEvery {
            autoVersionAuthScopeUseCase(null)
        } returns AutoVersionAuthScopeUseCase.Result.Success(
            authenticationScope
        )
        every { authenticationScope.ssoLoginScope.initiate } returns ssoInitiateLoginUseCase
        every { authenticationScope.ssoLoginScope.getLoginSession } returns getSSOLoginSessionUseCase
        every { coreLogic.versionedAuthenticationScope(any()) } returns autoVersionAuthScopeUseCase
        every { authenticationScope.ssoLoginScope.fetchSSOSettings } returns fetchSSOSettings

        loginViewModel = LoginSSOViewModel(
            savedStateHandle,
            addAuthenticatedUserUseCase,
            validateEmailUseCase,
            coreLogic,
            clientScopeProviderFactory,
            authServerConfigProvider,
            userDataStoreProvider
        )
    }

    @Test
    fun `given empty string, when entering code, then button is disabled`() {
        loginViewModel.ssoTextState.setTextAndPlaceCursorAtEnd(String.EMPTY)
        loginViewModel.loginState.loginEnabled shouldBeEqualTo false
        loginViewModel.loginState.flowState shouldNotBeInstanceOf LoginState.Loading::class
    }

    @Test
    fun `given non-empty string, when entering code, then button is enabled`() {
        loginViewModel.ssoTextState.setTextAndPlaceCursorAtEnd("abc")
        loginViewModel.loginState.loginEnabled shouldBeEqualTo true
        loginViewModel.loginState.flowState shouldNotBeInstanceOf LoginState.Loading::class
    }

    @Test
    fun `given sso code and button is clicked, when login returns Success, then open the web url from the response`() = runTest {
        val ssoCode = "wire-fd994b20-b9af-11ec-ae36-00163e9b33ca"
        val param = SSOInitiateLoginUseCase.Param.WithRedirect(ssoCode)
        val url = "https://wire.com/sso"
        coEvery { ssoInitiateLoginUseCase(param) } returns SSOInitiateLoginResult.Success(url)
        every { validateEmailUseCase(any()) } returns false
        loginViewModel.ssoTextState.setTextAndPlaceCursorAtEnd(ssoCode)
        loginViewModel.login()
        advanceUntilIdle()
        // loginViewModel.openWebUrl.firstOrNull() shouldBe url
        coVerify(exactly = 1) { ssoInitiateLoginUseCase(param) }
    }

    @Test
    fun `given sso code and  button is clicked, when login returns InvalidCodeFormat error, then InvalidCodeFormatError is passed`() =
        runTest {
            coEvery { ssoInitiateLoginUseCase(any()) } returns SSOInitiateLoginResult.Failure.InvalidCodeFormat
            every { validateEmailUseCase(any()) } returns false

            loginViewModel.login()
            advanceUntilIdle()
            loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Error.TextFieldError.InvalidValue>()
        }

    @Test
    fun `given  sso code and button is clicked, when login returns InvalidCode error, then InvalidCodeError is passed`() = runTest {
        coEvery { ssoInitiateLoginUseCase(any()) } returns SSOInitiateLoginResult.Failure.InvalidCode
        every { validateEmailUseCase(any()) } returns false

        loginViewModel.login()
        advanceUntilIdle()

        loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Error.DialogError.InvalidSSOCodeError>()
    }

    @Test
    fun `given sso code and button is clicked, when login returns InvalidRequest error, then GenericError IllegalArgument is passed`() =
        runTest {
            coEvery { ssoInitiateLoginUseCase(any()) } returns SSOInitiateLoginResult.Failure.InvalidRedirect
            every { validateEmailUseCase(any()) } returns false

            loginViewModel.login()
            advanceUntilIdle()

            loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Error.DialogError.GenericError>().let {
                it.coreFailure.shouldBeInstanceOf<CoreFailure.Unknown>().let {
                    it.rootCause.shouldBeInstanceOf<IllegalArgumentException>()
                }
            }
        }

    @Test
    fun `given sso code and button is clicked, when login returns Generic error, then GenericError is passed`() = runTest {
        val networkFailure = NetworkFailure.NoNetworkConnection(null)
        coEvery { ssoInitiateLoginUseCase(any()) } returns SSOInitiateLoginResult.Failure.Generic(networkFailure)
        every { validateEmailUseCase(any()) } returns false

        loginViewModel.login()
        advanceUntilIdle()

        loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Error.DialogError.GenericError>().let {
            it.coreFailure shouldBe networkFailure
        }
    }

    @Test
    fun `given sync is not completed, when establishSSOSession is called, navigate to initial sync screen`() = runTest {
        coEvery { getSSOLoginSessionUseCase(any()) } returns SSOLoginSessionResult.Success(AUTH_TOKEN, SSO_ID, null)
        coEvery {
            addAuthenticatedUserUseCase(
                any(),
                any(),
                any(),
                any()
            )
        } returns AddAuthenticatedUserUseCase.Result.Success(
            userId
        )
        coEvery { getOrRegisterClientUseCase(any()) } returns RegisterClientResult.Success(CLIENT)
        every { userDataStoreProvider.getOrCreate(any()).initialSyncCompleted } returns flowOf(false)

        loginViewModel.establishSSOSession("", serverConfigId = SERVER_CONFIG.id)
        advanceUntilIdle()

        coVerify(exactly = 1) { getSSOLoginSessionUseCase(any()) }
        coVerify(exactly = 1) { getOrRegisterClientUseCase(any()) }
        coVerify(exactly = 1) { addAuthenticatedUserUseCase(any(), any(), any(), any()) }
        loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Success>().let {
            it.initialSyncCompleted shouldBe false
            it.isE2EIRequired shouldBe false
        }
    }

    @Test
    fun `given establishSSOSession is called and initial sync is completed, when SSOLogin Success, navigate to home screen`() =
        runTest {
            coEvery { getSSOLoginSessionUseCase(any()) } returns SSOLoginSessionResult.Success(AUTH_TOKEN, SSO_ID, null)
            coEvery {
                addAuthenticatedUserUseCase(
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns AddAuthenticatedUserUseCase.Result.Success(
                userId
            )
            coEvery { getOrRegisterClientUseCase(any()) } returns RegisterClientResult.Success(CLIENT)
            every { userDataStoreProvider.getOrCreate(any()).initialSyncCompleted } returns flowOf(true)

            loginViewModel.establishSSOSession("", serverConfigId = SERVER_CONFIG.id)
            advanceUntilIdle()

            coVerify(exactly = 1) { getSSOLoginSessionUseCase(any()) }
            coVerify(exactly = 1) { getOrRegisterClientUseCase(any()) }
            coVerify(exactly = 1) { addAuthenticatedUserUseCase(any(), any(), any(), any()) }
            loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Success>().let {
                it.initialSyncCompleted shouldBe true
                it.isE2EIRequired shouldBe false
            }
        }

    @Test
    fun `given establishSSOSession is called, when SSOLoginSessionResult return InvalidCookie, then SSOLoginResult fails`() = runTest {
        coEvery { getSSOLoginSessionUseCase(any()) } returns SSOLoginSessionResult.Failure.InvalidCookie
        coEvery {
            addAuthenticatedUserUseCase(
                any(),
                any(),
                any(),
                any()
            )
        } returns AddAuthenticatedUserUseCase.Result.Success(
            userId
        )
        coEvery { getOrRegisterClientUseCase(any()) } returns RegisterClientResult.Success(CLIENT)

        loginViewModel.establishSSOSession("", serverConfigId = SERVER_CONFIG.id)
        advanceUntilIdle()
        loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Error.DialogError.InvalidSSOCookie>()
        coVerify(exactly = 1) { getSSOLoginSessionUseCase(any()) }
        coVerify(exactly = 0) { loginViewModel.registerClient(any(), null) }
        coVerify(exactly = 0) { addAuthenticatedUserUseCase(any(), any(), any(), any()) }
    }

    @Test
    fun `given HandleSSOResult is called, when ssoResult is null, then loginSSOError state should be none`() =
        runTest {
            loginViewModel.handleSSOResult(null)
            advanceUntilIdle()
            loginViewModel.loginState.flowState.shouldNotBeInstanceOf<LoginState.Error>()
        }

    @Test
    fun `given HandleSSOResult is called, when ssoResult is failure, then loginSSOError state should be dialog error`() =
        runTest {
            loginViewModel.handleSSOResult(DeepLinkResult.SSOLogin.Failure(SSOFailureCodes.Unknown))
            advanceUntilIdle()
            loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Error.DialogError.SSOResultError>().let {
                it.result shouldBe SSOFailureCodes.Unknown
            }
        }

    @Test
    fun `given HandleSSOResult is called, when SSOLoginResult is success, then establishSSOSession should be called once`() =
        runTest {
            coEvery { getSSOLoginSessionUseCase(any()) } returns SSOLoginSessionResult.Success(AUTH_TOKEN, SSO_ID, null)
            coEvery {
                addAuthenticatedUserUseCase(
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns AddAuthenticatedUserUseCase.Result.Success(
                userId
            )
            coEvery { getOrRegisterClientUseCase(any()) } returns RegisterClientResult.Success(CLIENT)
            every { userDataStoreProvider.getOrCreate(any()).initialSyncCompleted } returns flowOf(true)

            loginViewModel.handleSSOResult(DeepLinkResult.SSOLogin.Success("", ""))
            advanceUntilIdle()

            loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Success>()
        }

    @Test
    fun `given establishSSOSession called, when addAuthenticatedUser returns UserAlreadyExists error, then UserAlreadyExists is passed`() =
        runTest {
            coEvery { getSSOLoginSessionUseCase(any()) } returns SSOLoginSessionResult.Success(AUTH_TOKEN, SSO_ID, null)
            coEvery {
                addAuthenticatedUserUseCase(
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns AddAuthenticatedUserUseCase.Result.Failure.UserAlreadyExists

            loginViewModel.establishSSOSession("", serverConfigId = SERVER_CONFIG.id)
            advanceUntilIdle()

            loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Error.DialogError.UserAlreadyExists>()
            coVerify(exactly = 1) { getSSOLoginSessionUseCase(any()) }
            coVerify(exactly = 0) { loginViewModel.registerClient(any(), null) }
            coVerify(exactly = 1) { addAuthenticatedUserUseCase(any(), any(), any(), any()) }
        }

    @Test
    fun `given getOrRegister returns TooManyClients, when establishSSOSession, then TooManyClients is passed`() =
        runTest {
            coEvery { getSSOLoginSessionUseCase(any()) } returns SSOLoginSessionResult.Success(AUTH_TOKEN, SSO_ID, null)
            coEvery {
                addAuthenticatedUserUseCase(
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns AddAuthenticatedUserUseCase.Result.Success(
                userId
            )
            coEvery {
                getOrRegisterClientUseCase(any())
            } returns RegisterClientResult.Failure.TooManyClients

            loginViewModel.establishSSOSession("", serverConfigId = SERVER_CONFIG.id)
            advanceUntilIdle()

            loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Error.TooManyDevicesError>()

            coVerify(exactly = 1) { getOrRegisterClientUseCase(any()) }
            coVerify(exactly = 1) { getSSOLoginSessionUseCase(any()) }
            coVerify(exactly = 1) { addAuthenticatedUserUseCase(any(), any(), any(), any()) }
        }

    @Test
    fun `given email, when clicking login, then start the domain lookup flow`() = runTest {
        val expected = newServerConfig(2).links
        every { validateEmailUseCase(any()) } returns true
        coEvery { authenticationScope.domainLookup(any()) } returns DomainLookupUseCase.Result.Success(expected)
        loginViewModel.ssoTextState.setTextAndPlaceCursorAtEnd("email@wire.com")

        loginViewModel.domainLookupFlow()
        advanceUntilIdle()

        coVerify(exactly = 1) { authenticationScope.domainLookup("email@wire.com") }
        assertEquals(expected, loginViewModel.loginState.customServerDialogState!!.serverLinks)
    }

    @Test
    fun `given backend switch confirmed, then auth server provider is updated`() = runTest {
        val expected = newServerConfig(2).links
        loginViewModel.loginState = loginViewModel.loginState.copy(customServerDialogState = CustomServerDialogState(expected))
        coEvery { fetchSSOSettings.invoke() } returns FetchSSOSettingsUseCase.Result.Success("ssoCode")
        coEvery { ssoInitiateLoginUseCase(any()) } returns SSOInitiateLoginResult.Success("url")

        loginViewModel.onCustomServerDialogConfirm()

        advanceUntilIdle()
        assertEquals(authServerConfigProvider.authServer.value, expected)
    }

    @Test
    fun `given backend switch confirmed, when the new server have a default sso code, then initiate sso login`() = runTest {
        val expected = newServerConfig(2).links
        every { validateEmailUseCase(any()) } returns true
        coEvery { fetchSSOSettings.invoke() } returns FetchSSOSettingsUseCase.Result.Success("ssoCode")
        coEvery { ssoInitiateLoginUseCase(any()) } returns SSOInitiateLoginResult.Success("url")

        loginViewModel.loginState = loginViewModel.loginState.copy(customServerDialogState = CustomServerDialogState(expected))

        loginViewModel.onCustomServerDialogConfirm()

        advanceUntilIdle()
        assertEquals(authServerConfigProvider.authServer.value, expected)
        coVerify(exactly = 1) {
            fetchSSOSettings.invoke()
            ssoInitiateLoginUseCase.invoke("wire-ssoCode")
        }
    }

    @Test
    fun `given backend switch confirmed, when the new server have NO default sso code, then do not initiate sso login`() = runTest {
        val expected = newServerConfig(2).links
        every { validateEmailUseCase(any()) } returns true
        coEvery { fetchSSOSettings.invoke() } returns FetchSSOSettingsUseCase.Result.Success(null)
        loginViewModel.loginState = loginViewModel.loginState.copy(customServerDialogState = CustomServerDialogState(expected))

        loginViewModel.onCustomServerDialogConfirm()

        advanceUntilIdle()
        assertEquals(authServerConfigProvider.authServer.value, expected)
        coVerify(exactly = 1) { fetchSSOSettings.invoke() }

        coVerify(exactly = 0) { ssoInitiateLoginUseCase.invoke(any()) }
    }

    @Test
    fun `given error, when checking for server default SSO code, then do not initiate sso login`() = runTest {
        val expected = newServerConfig(2).links
        every { validateEmailUseCase(any()) } returns true
        coEvery { fetchSSOSettings.invoke() } returns FetchSSOSettingsUseCase.Result.Failure(CoreFailure.Unknown(IOException()))
        loginViewModel.loginState = loginViewModel.loginState.copy(customServerDialogState = CustomServerDialogState(expected))

        loginViewModel.onCustomServerDialogConfirm()

        advanceUntilIdle()
        assertEquals(authServerConfigProvider.authServer.value, expected)
        coVerify(exactly = 1) { fetchSSOSettings.invoke() }

        coVerify(exactly = 0) { ssoInitiateLoginUseCase.invoke(any()) }
    }

    @Test
    fun `given error, when doing domain lookup, then error state is updated`() = runTest {
        val expected = CoreFailure.Unknown(IOException())
        every { validateEmailUseCase(any()) } returns true
        coEvery { authenticationScope.domainLookup(any()) } returns DomainLookupUseCase.Result.Failure(expected)
        loginViewModel.ssoTextState.setTextAndPlaceCursorAtEnd("email@wire.com")

        loginViewModel.domainLookupFlow()
        advanceUntilIdle()

        coVerify(exactly = 1) { authenticationScope.domainLookup("email@wire.com") }
        loginViewModel.loginState.customServerDialogState shouldBe null
        loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Error.DialogError.GenericError>().let {
            it.coreFailure shouldBe expected
        }
    }

    companion object {
        val CLIENT = TestClient.CLIENT
        val SSO_ID: SsoId = SsoId("scim_id", null, null)
        val AUTH_TOKEN = AccountTokens(
            userId = UserId("user_id", "domain"),
            accessToken = "access_token",
            refreshToken = "refresh_token",
            tokenType = "token_type",
            cookieLabel = null
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
