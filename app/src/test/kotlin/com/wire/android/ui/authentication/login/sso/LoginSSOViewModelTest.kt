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
import app.cash.turbine.test
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.config.SnapshotExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.config.mockUri
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.ClientScopeProvider
import com.wire.android.framework.TestClient
import com.wire.android.ui.authentication.login.LoginNavArgs
import com.wire.android.ui.authentication.login.LoginPasswordPath
import com.wire.android.ui.authentication.login.LoginState
import com.wire.android.ui.navArgs
import com.wire.android.util.EMPTY
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
import com.wire.kalium.logic.feature.auth.ValidateEmailUseCase
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.auth.sso.FetchSSOSettingsUseCase
import com.wire.kalium.logic.feature.auth.sso.GetSSOLoginSessionUseCase
import com.wire.kalium.logic.feature.auth.sso.SSOInitiateLoginResult
import com.wire.kalium.logic.feature.auth.sso.SSOInitiateLoginUseCase
import com.wire.kalium.logic.feature.client.ClientScope
import com.wire.kalium.logic.feature.client.GetOrRegisterClientUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldNotBeInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class, SnapshotExtension::class, NavigationTestExtension::class)
class LoginSSOViewModelTest {

    private val dispatcherProvider = TestDispatcherProvider()

    @Test
    fun `given empty string, when entering code, then button is disabled`() {
        val (_, loginViewModel) = Arrangement().arrange()

        loginViewModel.ssoTextState.setTextAndPlaceCursorAtEnd(String.EMPTY)
        loginViewModel.loginState.loginEnabled shouldBeEqualTo false
        loginViewModel.loginState.flowState shouldNotBeInstanceOf LoginState.Loading::class
    }

    @Test
    fun `given non-empty string, when entering code, then button is enabled`() {
        val (_, loginViewModel) = Arrangement().arrange()
        loginViewModel.ssoTextState.setTextAndPlaceCursorAtEnd("abc")
        loginViewModel.loginState.loginEnabled shouldBeEqualTo true
        loginViewModel.loginState.flowState shouldNotBeInstanceOf LoginState.Loading::class
    }

    @Test
    fun `given sso code and button is clicked, when login returns Success, then open the web url from the response`() =
        runTest(dispatcherProvider.main()) {
            val expectedSSOCode = "wire-fd994b20-b9af-11ec-ae36-00163e9b33ca"
            val expectedUrl = "https://wire.com/sso"
            val (arrangement, loginViewModel) = Arrangement()
                .withValidateEmailReturning(false)
                .withInitiateSSO(expectedSSOCode)
                .arrange()

            loginViewModel.ssoTextState.setTextAndPlaceCursorAtEnd(expectedSSOCode)
            loginViewModel.openWebUrl.test {
                loginViewModel.login()
                advanceUntilIdle()

                coVerify(exactly = 1) { arrangement.validateEmailUseCase(eq(expectedSSOCode)) }
                coVerify(exactly = 1) {
                    arrangement.ssoExtension.initiateSSO(
                        eq(SERVER_CONFIG.links),
                        eq(expectedSSOCode),
                        capture(onAuthScopeFailureSlot),
                        capture(onSSOInitiateFailureSlot),
                        capture(onSuccessSlot)
                    )
                }
                onSuccessSlot.captured.invoke(expectedUrl, SERVER_CONFIG.links)

                val (url, customServerConfig) = awaitItem()
                assertEquals(expectedUrl, url)
                assertEquals(SERVER_CONFIG.links, customServerConfig)

                loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Default>()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `given sso code and  button is clicked, when login returns InvalidCodeFormat error, then InvalidCodeFormatError is passed`() =
        runTest {
            val expectedSSOCode = "wire-fd994b20-b9af-11ec-ae36-00163e9b33ca"
            val (arrangement, loginViewModel) = Arrangement()
                .withValidateEmailReturning(false)
                .withInitiateSSO(expectedSSOCode)
                .arrange()

            loginViewModel.ssoTextState.setTextAndPlaceCursorAtEnd(expectedSSOCode)

            loginViewModel.login()
            loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Loading>()
            advanceUntilIdle()

            coVerify(exactly = 1) { arrangement.validateEmailUseCase(eq(expectedSSOCode)) }
            coVerify(exactly = 1) {
                arrangement.ssoExtension.initiateSSO(
                    eq(SERVER_CONFIG.links),
                    eq(expectedSSOCode),
                    capture(onAuthScopeFailureSlot),
                    capture(onSSOInitiateFailureSlot),
                    capture(onSuccessSlot)
                )
            }

            onSSOInitiateFailureSlot.captured.invoke(SSOInitiateLoginResult.Failure.InvalidCodeFormat)
            loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Error.TextFieldError.InvalidValue>()
        }

    @Test
    fun `given  sso code and button is clicked, when login returns InvalidCode error, then InvalidCodeError is passed`() = runTest {
        val expectedSSOCode = "wire-fd994b20-b9af-11ec-ae36-00163e9b33ca"
        val (arrangement, loginViewModel) = Arrangement()
            .withValidateEmailReturning(false)
            .withInitiateSSO(expectedSSOCode)
            .arrange()

        loginViewModel.ssoTextState.setTextAndPlaceCursorAtEnd(expectedSSOCode)

        loginViewModel.login()
        loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Loading>()
        advanceUntilIdle()

        coVerify(exactly = 1) { arrangement.validateEmailUseCase(eq(expectedSSOCode)) }
        coVerify(exactly = 1) {
            arrangement.ssoExtension.initiateSSO(
                eq(SERVER_CONFIG.links),
                eq(expectedSSOCode),
                capture(onAuthScopeFailureSlot),
                capture(onSSOInitiateFailureSlot),
                capture(onSuccessSlot)
            )
        }

        onSSOInitiateFailureSlot.captured.invoke(SSOInitiateLoginResult.Failure.InvalidCode)
        loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Error.DialogError.InvalidSSOCodeError>()
    }


    @Test
    fun `given sso code and button is clicked, when login returns InvalidRequest error, then GenericError IllegalArgument is passed`() =
        runTest {
            val expectedSSOCode = "wire-fd994b20-b9af-11ec-ae36-00163e9b33ca"
            val (arrangement, loginViewModel) = Arrangement()
                .withValidateEmailReturning(false)
                .withInitiateSSO(expectedSSOCode)
                .arrange()

            loginViewModel.ssoTextState.setTextAndPlaceCursorAtEnd(expectedSSOCode)

            loginViewModel.login()
            loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Loading>()
            advanceUntilIdle()

            coVerify(exactly = 1) { arrangement.validateEmailUseCase(eq(expectedSSOCode)) }
            coVerify(exactly = 1) {
                arrangement.ssoExtension.initiateSSO(
                    eq(SERVER_CONFIG.links),
                    eq(expectedSSOCode),
                    capture(onAuthScopeFailureSlot),
                    capture(onSSOInitiateFailureSlot),
                    capture(onSuccessSlot)
                )
            }

            onSSOInitiateFailureSlot.captured.invoke(SSOInitiateLoginResult.Failure.InvalidRedirect)
            loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Error.DialogError.GenericError>().let {
                it.coreFailure.shouldBeInstanceOf<CoreFailure.Unknown>().let {
                    it.rootCause.shouldBeInstanceOf<IllegalArgumentException>()
                }
            }
        }

    @Test
    fun `given sso code and button is clicked, when login returns Generic error, then GenericError is passed`() = runTest {
        val expectedSSOCode = "wire-fd994b20-b9af-11ec-ae36-00163e9b33ca"
        val (arrangement, loginViewModel) = Arrangement()
            .withValidateEmailReturning(false)
            .withInitiateSSO(expectedSSOCode)
            .arrange()

        loginViewModel.ssoTextState.setTextAndPlaceCursorAtEnd(expectedSSOCode)

        loginViewModel.login()
        loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Loading>()
        advanceUntilIdle()

        coVerify(exactly = 1) { arrangement.validateEmailUseCase(eq(expectedSSOCode)) }
        coVerify(exactly = 1) {
            arrangement.ssoExtension.initiateSSO(
                eq(SERVER_CONFIG.links),
                eq(expectedSSOCode),
                capture(onAuthScopeFailureSlot),
                capture(onSSOInitiateFailureSlot),
                capture(onSuccessSlot)
            )
        }

        val networkFailure = NetworkFailure.NoNetworkConnection(null)
        onSSOInitiateFailureSlot.captured.invoke(SSOInitiateLoginResult.Failure.Generic(networkFailure))
        loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Error.DialogError.GenericError>().let {
            it.coreFailure shouldBe networkFailure
        }
    }
//
//    @Test
//    fun `given sync is not completed, when establishSSOSession is called, navigate to initial sync screen`() = runTest {
//        coEvery { getSSOLoginSessionUseCase(any()) } returns SSOLoginSessionResult.Success(AUTH_TOKEN, SSO_ID, null)
//        coEvery {
//            addAuthenticatedUserUseCase(
//                any(),
//                any(),
//                any(),
//                any()
//            )
//        } returns AddAuthenticatedUserUseCase.Result.Success(
//            userId
//        )
//        coEvery { getOrRegisterClientUseCase(any()) } returns RegisterClientResult.Success(CLIENT)
//        every { userDataStoreProvider.getOrCreate(any()).initialSyncCompleted } returns flowOf(false)
//
//        loginViewModel.establishSSOSession(cookie = "", serverConfigId = SERVER_CONFIG.id, serverConfig = SERVER_CONFIG.links)
//        advanceUntilIdle()
//
//        coVerify(exactly = 1) { getSSOLoginSessionUseCase(any()) }
//        coVerify(exactly = 1) { getOrRegisterClientUseCase(any()) }
//        coVerify(exactly = 1) { addAuthenticatedUserUseCase(any(), any(), any(), any()) }
//        loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Success>().let {
//            it.initialSyncCompleted shouldBe false
//            it.isE2EIRequired shouldBe false
//        }
//    }
//
//    @Test
//    fun `given establishSSOSession is called and initial sync is completed, when SSOLogin Success, navigate to home screen`() =
//        runTest {
//            coEvery { getSSOLoginSessionUseCase(any()) } returns SSOLoginSessionResult.Success(AUTH_TOKEN, SSO_ID, null)
//            coEvery {
//                addAuthenticatedUserUseCase(
//                    any(),
//                    any(),
//                    any(),
//                    any()
//                )
//            } returns AddAuthenticatedUserUseCase.Result.Success(
//                userId
//            )
//            coEvery { getOrRegisterClientUseCase(any()) } returns RegisterClientResult.Success(CLIENT)
//            every { userDataStoreProvider.getOrCreate(any()).initialSyncCompleted } returns flowOf(true)
//
//            loginViewModel.establishSSOSession("", serverConfigId = SERVER_CONFIG.id)
//            advanceUntilIdle()
//
//            coVerify(exactly = 1) { getSSOLoginSessionUseCase(any()) }
//            coVerify(exactly = 1) { getOrRegisterClientUseCase(any()) }
//            coVerify(exactly = 1) { addAuthenticatedUserUseCase(any(), any(), any(), any()) }
//            loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Success>().let {
//                it.initialSyncCompleted shouldBe true
//                it.isE2EIRequired shouldBe false
//            }
//        }
//
//    @Test
//    fun `given establishSSOSession is called, when SSOLoginSessionResult return InvalidCookie, then SSOLoginResult fails`() = runTest {
//        coEvery { getSSOLoginSessionUseCase(any()) } returns SSOLoginSessionResult.Failure.InvalidCookie
//        coEvery {
//            addAuthenticatedUserUseCase(
//                any(),
//                any(),
//                any(),
//                any()
//            )
//        } returns AddAuthenticatedUserUseCase.Result.Success(
//            userId
//        )
//        coEvery { getOrRegisterClientUseCase(any()) } returns RegisterClientResult.Success(CLIENT)
//
//        loginViewModel.establishSSOSession("", serverConfigId = SERVER_CONFIG.id)
//        advanceUntilIdle()
//        loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Error.DialogError.InvalidSSOCookie>()
//        coVerify(exactly = 1) { getSSOLoginSessionUseCase(any()) }
//        coVerify(exactly = 0) { loginViewModel.registerClient(any(), null) }
//        coVerify(exactly = 0) { addAuthenticatedUserUseCase(any(), any(), any(), any()) }
//    }
//
//    @Test
//    fun `given HandleSSOResult is called, when ssoResult is null, then loginSSOError state should be none`() =
//        runTest {
//            loginViewModel.handleSSOResult(null, serverConfig = SERVER_CONFIG.links)
//            advanceUntilIdle()
//            loginViewModel.loginState.flowState.shouldNotBeInstanceOf<LoginState.Error>()
//        }
//
//    @Test
//    fun `given HandleSSOResult is called, when ssoResult is failure, then loginSSOError state should be dialog error`() =
//        runTest {
//            loginViewModel.handleSSOResult(DeepLinkResult.SSOLogin.Failure(SSOFailureCodes.Unknown))
//            advanceUntilIdle()
//            loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Error.DialogError.SSOResultError>().let {
//                it.result shouldBe SSOFailureCodes.Unknown
//            }
//        }
//
//    @Test
//    fun `given HandleSSOResult is called, when SSOLoginResult is success, then establishSSOSession should be called once`() =
//        runTest {
//            coEvery { getSSOLoginSessionUseCase(any()) } returns SSOLoginSessionResult.Success(AUTH_TOKEN, SSO_ID, null)
//            coEvery {
//                addAuthenticatedUserUseCase(
//                    any(),
//                    any(),
//                    any(),
//                    any()
//                )
//            } returns AddAuthenticatedUserUseCase.Result.Success(
//                userId
//            )
//            coEvery { getOrRegisterClientUseCase(any()) } returns RegisterClientResult.Success(CLIENT)
//            every { userDataStoreProvider.getOrCreate(any()).initialSyncCompleted } returns flowOf(true)
//
//            loginViewModel.handleSSOResult(DeepLinkResult.SSOLogin.Success("", ""))
//            advanceUntilIdle()
//
//            loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Success>()
//        }
//
//    @Test
//    fun `given establishSSOSession called, when addAuthenticatedUser returns UserAlreadyExists error, then UserAlreadyExists is passed`() =
//        runTest {
//            coEvery { getSSOLoginSessionUseCase(any()) } returns SSOLoginSessionResult.Success(AUTH_TOKEN, SSO_ID, null)
//            coEvery {
//                addAuthenticatedUserUseCase(
//                    any(),
//                    any(),
//                    any(),
//                    any()
//                )
//            } returns AddAuthenticatedUserUseCase.Result.Failure.UserAlreadyExists
//
//            loginViewModel.establishSSOSession("", serverConfigId = SERVER_CONFIG.id)
//            advanceUntilIdle()
//
//            loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Error.DialogError.UserAlreadyExists>()
//            coVerify(exactly = 1) { getSSOLoginSessionUseCase(any()) }
//            coVerify(exactly = 0) { loginViewModel.registerClient(any(), null) }
//            coVerify(exactly = 1) { addAuthenticatedUserUseCase(any(), any(), any(), any()) }
//        }
//
//    @Test
//    fun `given getOrRegister returns TooManyClients, when establishSSOSession, then TooManyClients is passed`() =
//        runTest {
//            coEvery { getSSOLoginSessionUseCase(any()) } returns SSOLoginSessionResult.Success(AUTH_TOKEN, SSO_ID, null)
//            coEvery {
//                addAuthenticatedUserUseCase(
//                    any(),
//                    any(),
//                    any(),
//                    any()
//                )
//            } returns AddAuthenticatedUserUseCase.Result.Success(
//                userId
//            )
//            coEvery {
//                getOrRegisterClientUseCase(any())
//            } returns RegisterClientResult.Failure.TooManyClients
//
//            loginViewModel.establishSSOSession("", serverConfigId = SERVER_CONFIG.id)
//            advanceUntilIdle()
//
//            loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Error.TooManyDevicesError>()
//
//            coVerify(exactly = 1) { getOrRegisterClientUseCase(any()) }
//            coVerify(exactly = 1) { getSSOLoginSessionUseCase(any()) }
//            coVerify(exactly = 1) { addAuthenticatedUserUseCase(any(), any(), any(), any()) }
//        }
//
//    @Test
//    fun `given establishSSOSession called with custom server config, then establish SSO session using custom server config`() = runTest {
//        val customConfig = newServerConfig(2)
//        coEvery { getSSOLoginSessionUseCase(any()) } returns SSOLoginSessionResult.Success(AUTH_TOKEN, SSO_ID, null)
//        coEvery {
//            addAuthenticatedUserUseCase(
//                any(),
//                any(),
//                any(),
//                any()
//            )
//        } returns AddAuthenticatedUserUseCase.Result.Success(userId)
//        coEvery { getOrRegisterClientUseCase(any()) } returns RegisterClientResult.Success(CLIENT)
//        every { userDataStoreProvider.getOrCreate(any()).initialSyncCompleted } returns flowOf(true)
//
//        loginViewModel.establishSSOSession("", serverConfigId = customConfig.id, serverConfig = customConfig.links)
//        advanceUntilIdle()
//
//        coVerify(exactly = 1) {
//            coreLogic.versionedAuthenticationScope(customConfig.links)
//        }
//    }
//
//    @Test
//    fun `given email, when clicking login, then start the domain lookup flow`() = runTest {
//        val expected = newServerConfig(2).links
//        every { validateEmailUseCase(any()) } returns true
//        coEvery { authenticationScope.domainLookup(any()) } returns DomainLookupUseCase.Result.Success(expected)
//        loginViewModel.ssoTextState.setTextAndPlaceCursorAtEnd("email@wire.com")
//
//        loginViewModel.domainLookupFlow()
//        advanceUntilIdle()
//
//        coVerify(exactly = 1) { authenticationScope.domainLookup("email@wire.com") }
//        assertEquals(expected, loginViewModel.loginState.customServerDialogState!!.serverLinks)
//    }
//
//    @Test
//    fun `given backend switch confirmed and sso init successful, then open web url with updated server config`() = runTest {
//        val expected = newServerConfig(2).links
//        loginViewModel.loginState = loginViewModel.loginState.copy(customServerDialogState = CustomServerDetailsDialogState(expected))
//        coEvery { fetchSSOSettings.invoke() } returns FetchSSOSettingsUseCase.Result.Success("ssoCode")
//        coEvery { ssoInitiateLoginUseCase(any()) } returns SSOInitiateLoginResult.Success("url")
//
//        loginViewModel.openWebUrl.test {
//
//            loginViewModel.onCustomServerDialogConfirm()
//
//            advanceUntilIdle()
//
//            val (_, resultCustomServer) = awaitItem()
//            assertEquals(resultCustomServer, expected)
//
//            cancelAndIgnoreRemainingEvents()
//        }
//    }
//
//    @Test
//    fun `given backend switch confirmed, when the new server have a default sso code, then initiate sso login`() = runTest {
//        val expected = newServerConfig(2).links
//        every { validateEmailUseCase(any()) } returns true
//        coEvery { fetchSSOSettings.invoke() } returns FetchSSOSettingsUseCase.Result.Success("ssoCode")
//        coEvery { ssoInitiateLoginUseCase(any()) } returns SSOInitiateLoginResult.Success("url")
//
//        loginViewModel.loginState = loginViewModel.loginState.copy(customServerDialogState = CustomServerDetailsDialogState(expected))
//
//        loginViewModel.onCustomServerDialogConfirm()
//
//        advanceUntilIdle()
//        coVerify {
//            loginSSOViewModelExtension.fetchDefaultSSOCode(eq(expected), any(), any(), any())
//        }
//    }
//
//    @Test
//    fun `given backend switch confirmed, when the new server have NO default sso code, then do not initiate sso login`() = runTest {
//        val expected = newServerConfig(2).links
//        every { validateEmailUseCase(any()) } returns true
//        coEvery { fetchSSOSettings.invoke() } returns FetchSSOSettingsUseCase.Result.Success(null)
//        loginViewModel.loginState = loginViewModel.loginState.copy(customServerDialogState = CustomServerDetailsDialogState(expected))
//
//        loginViewModel.onCustomServerDialogConfirm()
//
//        advanceUntilIdle()
//        coVerify(exactly = 1) {
//            coreLogic.versionedAuthenticationScope(expected)
//            fetchSSOSettings.invoke()
//        }
//        coVerify(exactly = 0) {
//            ssoInitiateLoginUseCase.invoke(any())
//        }
//    }
//
//    @Test
//    fun `given error, when checking for server default SSO code, then do not initiate sso login`() = runTest {
//        val expected = newServerConfig(2).links
//        every { validateEmailUseCase(any()) } returns true
//        coEvery { fetchSSOSettings.invoke() } returns FetchSSOSettingsUseCase.Result.Failure(CoreFailure.Unknown(IOException()))
//        loginViewModel.loginState = loginViewModel.loginState.copy(customServerDialogState = CustomServerDetailsDialogState(expected))
//
//        loginViewModel.onCustomServerDialogConfirm()
//
//        advanceUntilIdle()
//        coVerify(exactly = 1) {
//            coreLogic.versionedAuthenticationScope(expected)
//            fetchSSOSettings.invoke()
//        }
//
//        coVerify(exactly = 0) {
//            ssoInitiateLoginUseCase.invoke(any())
//        }
//    }
//
//    @Test
//    fun `given error, when doing domain lookup, then error state is updated`() = runTest {
//        val expected = CoreFailure.Unknown(IOException())
//        every { validateEmailUseCase(any()) } returns true
//        coEvery { authenticationScope.domainLookup(any()) } returns DomainLookupUseCase.Result.Failure(expected)
//        loginViewModel.ssoTextState.setTextAndPlaceCursorAtEnd("email@wire.com")
//
//        loginViewModel.domainLookupFlow()
//        advanceUntilIdle()
//
//        coVerify(exactly = 1) { authenticationScope.domainLookup("email@wire.com") }
//        loginViewModel.loginState.customServerDialogState shouldBe null
//        loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Error.DialogError.GenericError>().let {
//            it.coreFailure shouldBe expected
//        }
//    }

    private class Arrangement {

        @MockK
        lateinit var savedStateHandle: SavedStateHandle

        @MockK
        lateinit var ssoInitiateLoginUseCase: SSOInitiateLoginUseCase

        @MockK
        lateinit var addAuthenticatedUserUseCase: AddAuthenticatedUserUseCase

        @MockK
        lateinit var clientScopeProviderFactory: ClientScopeProvider.Factory

        @MockK
        lateinit var clientScope: ClientScope

        @MockK
        lateinit var getOrRegisterClientUseCase: GetOrRegisterClientUseCase

        @MockK
        lateinit var getSSOLoginSessionUseCase: GetSSOLoginSessionUseCase

        @MockK
        lateinit var userDataStoreProvider: UserDataStoreProvider

        @MockK
        lateinit var autoVersionAuthScopeUseCase: AutoVersionAuthScopeUseCase

        @MockK
        lateinit var coreLogic: CoreLogic

        @MockK
        lateinit var authenticationScope: AuthenticationScope

        @MockK
        lateinit var validateEmailUseCase: ValidateEmailUseCase

        @MockK
        lateinit var fetchSSOSettings: FetchSSOSettingsUseCase

        @MockK
        lateinit var ssoExtension: LoginSSOViewModelExtension

        private val userId: QualifiedID = QualifiedID("userId", "domain")

        init {
            MockKAnnotations.init(this)
            mockUri()
            every { savedStateHandle.get<String>(any()) } returns null
            every { savedStateHandle.set(any(), any<String>()) } returns Unit
            every { clientScopeProviderFactory.create(any()).clientScope } returns clientScope
            every { clientScope.getOrRegister } returns getOrRegisterClientUseCase
            every { savedStateHandle.navArgs<LoginNavArgs>() } returns LoginNavArgs(
                loginPasswordPath = LoginPasswordPath(SERVER_CONFIG.links)
            )

            coEvery {
                autoVersionAuthScopeUseCase(null)
            } returns AutoVersionAuthScopeUseCase.Result.Success(
                authenticationScope
            )
            every { authenticationScope.ssoLoginScope.initiate } returns ssoInitiateLoginUseCase
            every { authenticationScope.ssoLoginScope.getLoginSession } returns getSSOLoginSessionUseCase
            every { coreLogic.versionedAuthenticationScope(any()) } returns autoVersionAuthScopeUseCase
            every { authenticationScope.ssoLoginScope.fetchSSOSettings } returns fetchSSOSettings
            coEvery { ssoExtension.fetchDefaultSSOCode(any(), any(), any(), any()) } returns Unit
        }

        fun withInitiateSSO(ssoCode: String) = apply {
            coEvery {
                ssoExtension.initiateSSO(
                    eq(SERVER_CONFIG.links),
                    eq(ssoCode),
                    any(),
                    any(),
                    any()
                )
            } returns Unit
        }

        fun withValidateEmailReturning(result: Boolean) = apply {
            every { validateEmailUseCase(any()) } returns result
        }

        fun arrange() = this to LoginSSOViewModel(
            savedStateHandle,
            addAuthenticatedUserUseCase,
            validateEmailUseCase,
            coreLogic,
            clientScopeProviderFactory,
            userDataStoreProvider,
            ssoExtension
        )
    }

    companion object {
        val onAuthScopeFailureLambda: (AutoVersionAuthScopeUseCase.Result.Failure) -> Unit = {}
        val onSSOInitiateFailureLambda: (SSOInitiateLoginResult.Failure) -> Unit = {}
        val onSuccessLambda: (redirectUrl: String, serverConfig: ServerConfig.Links) -> Unit = { _, _ -> }

        val onAuthScopeFailureSlot = slot<((AutoVersionAuthScopeUseCase.Result.Failure) -> Unit)>()
        val onSSOInitiateFailureSlot = slot<((SSOInitiateLoginResult.Failure) -> Unit)>()
        val onSuccessSlot = slot<((redirectUrl: String, serverConfig: ServerConfig.Links) -> Unit)>()

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
