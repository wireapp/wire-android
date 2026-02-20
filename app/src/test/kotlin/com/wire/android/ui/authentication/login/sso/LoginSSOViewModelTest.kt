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
import com.wire.android.assertions.shouldBeEqualTo
import com.wire.android.assertions.shouldBeInstanceOf
import com.wire.android.assertions.shouldNotBeInstanceOf
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.config.SnapshotExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.config.mockUri
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.ClientScopeProvider
import com.wire.android.framework.TestClient
import com.wire.android.framework.TestUser
import com.wire.android.ui.authentication.login.LoginNavArgs
import com.wire.android.ui.authentication.login.LoginPasswordPath
import com.wire.android.ui.authentication.login.LoginState
import com.wire.android.ui.common.dialogs.CustomServerDetailsDialogState
import com.ramcosta.composedestinations.generated.app.navArgs
import com.wire.android.util.EMPTY
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.android.util.deeplink.SSOFailureCodes
import com.wire.android.util.newServerConfig
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.CommonApiVersionType
import com.wire.kalium.logic.configuration.server.ServerConfig
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
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.IOException

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
            it.coreFailure shouldBeEqualTo networkFailure
        }
    }

    @Test
    fun `given sync is not completed, when establishSSOSession is called, navigate to initial sync screen`() = runTest {
        val expectedCookie = "some-cookie"
        val (arrangement, loginViewModel) = Arrangement()
            .withEstablishSSOSession(expectedCookie)
            .withIsSyncCompletedReturning(false)
            .withRegisterClientReturning(RegisterClientResult.Success(TestClient.CLIENT))
            .arrange()

        loginViewModel.establishSSOSession(expectedCookie, SERVER_CONFIG.id, SERVER_CONFIG.links)
        loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Loading>()
        advanceUntilIdle()

        coVerify(exactly = 1) {
            arrangement.ssoExtension.establishSSOSession(
                eq(expectedCookie),
                eq(SERVER_CONFIG.id),
                eq(SERVER_CONFIG.links),
                capture(onAuthScopeFailureSlot),
                capture(onSSOLoginFailureSlot),
                capture(onAddAuthenticatedUserFailureSlot),
                capture(onSuccessEstablishSSOSessionSlot)
            )
        }

        onSuccessEstablishSSOSessionSlot.captured.invoke(TestUser.USER_ID)
        loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Success>().let {
            it.initialSyncCompleted shouldBeEqualTo false
            it.isE2EIRequired shouldBeEqualTo false
        }
    }

    @Test
    fun `given establishSSOSession is called and initial sync is completed, when SSOLogin Success, navigate to home screen`() =
        runTest {
            val expectedCookie = "some-cookie"
            val (arrangement, loginViewModel) = Arrangement()
                .withEstablishSSOSession(expectedCookie)
                .withIsSyncCompletedReturning(true)
                .withRegisterClientReturning(RegisterClientResult.Success(TestClient.CLIENT))
                .arrange()

            loginViewModel.establishSSOSession(expectedCookie, SERVER_CONFIG.id, SERVER_CONFIG.links)
            loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Loading>()
            advanceUntilIdle()

            coVerify(exactly = 1) {
                arrangement.ssoExtension.establishSSOSession(
                    eq(expectedCookie),
                    eq(SERVER_CONFIG.id),
                    eq(SERVER_CONFIG.links),
                    capture(onAuthScopeFailureSlot),
                    capture(onSSOLoginFailureSlot),
                    capture(onAddAuthenticatedUserFailureSlot),
                    capture(onSuccessEstablishSSOSessionSlot)
                )
            }

            onSuccessEstablishSSOSessionSlot.captured.invoke(TestUser.USER_ID)
            loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Success>().let {
                it.initialSyncCompleted shouldBeEqualTo true
                it.isE2EIRequired shouldBeEqualTo false
            }
        }

    @Test
    fun `given establishSSOSession is called, when SSOLoginSessionResult return InvalidCookie, then SSOLoginResult fails`() = runTest {
        val expectedCookie = "some-cookie"
        val (arrangement, loginViewModel) = Arrangement()
            .withEstablishSSOSession(expectedCookie)
            .withIsSyncCompletedReturning(false)
            .withRegisterClientReturning(RegisterClientResult.Success(TestClient.CLIENT))
            .arrange()

        loginViewModel.establishSSOSession(expectedCookie, SERVER_CONFIG.id, SERVER_CONFIG.links)
        loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Loading>()
        advanceUntilIdle()

        coVerify(exactly = 1) {
            arrangement.ssoExtension.establishSSOSession(
                eq(expectedCookie),
                eq(SERVER_CONFIG.id),
                eq(SERVER_CONFIG.links),
                capture(onAuthScopeFailureSlot),
                capture(onSSOLoginFailureSlot),
                capture(onAddAuthenticatedUserFailureSlot),
                capture(onSuccessEstablishSSOSessionSlot)
            )
        }

        onSSOLoginFailureSlot.captured.invoke(SSOLoginSessionResult.Failure.InvalidCookie)
        loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Error.DialogError.InvalidSSOCookie>()
    }

    @Test
    fun `given HandleSSOResult is called, when ssoResult is null, then loginSSOError state should be none`() =
        runTest {
            val (_, loginViewModel) = Arrangement().arrange()

            loginViewModel.handleSSOResult(null, serverConfig = SERVER_CONFIG.links)
            advanceUntilIdle()
            loginViewModel.loginState.flowState.shouldNotBeInstanceOf<LoginState.Error>()
        }

    @Test
    fun `given HandleSSOResult is called, when ssoResult is failure, then loginSSOError state should be dialog error`() =
        runTest {
            val (_, loginViewModel) = Arrangement().arrange()

            loginViewModel.handleSSOResult(DeepLinkResult.SSOLogin.Failure(SSOFailureCodes.Unknown))
            advanceUntilIdle()
            loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Error.DialogError.SSOResultError>().let {
                it.result shouldBeEqualTo SSOFailureCodes.Unknown
            }
        }

    @Test
    fun `given HandleSSOResult is called, when SSOLoginResult is success, then establishSSOSession should be called once`() =
        runTest {
            val expectedCookie = "some-cookie"
            val (arrangement, loginViewModel) = Arrangement()
                .withEstablishSSOSession(expectedCookie)
                .withIsSyncCompletedReturning(true)
                .withRegisterClientReturning(RegisterClientResult.Success(TestClient.CLIENT))
                .arrange()

            loginViewModel.handleSSOResult(DeepLinkResult.SSOLogin.Success(expectedCookie, SERVER_CONFIG.id), SERVER_CONFIG.links)
            advanceUntilIdle()
            coVerify(exactly = 1) {
                arrangement.ssoExtension.establishSSOSession(
                    eq(expectedCookie),
                    eq(SERVER_CONFIG.id),
                    eq(SERVER_CONFIG.links),
                    capture(onAuthScopeFailureSlot),
                    capture(onSSOLoginFailureSlot),
                    capture(onAddAuthenticatedUserFailureSlot),
                    capture(onSuccessEstablishSSOSessionSlot)
                )
            }
            onSuccessEstablishSSOSessionSlot.captured.invoke(TestUser.USER_ID)
            loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Success>()
        }

    @Test
    fun `given establishSSOSession called, when addAuthenticatedUser returns UserAlreadyExists error, then UserAlreadyExists is passed`() =
        runTest {
            val expectedCookie = "some-cookie"
            val (arrangement, loginViewModel) = Arrangement()
                .withEstablishSSOSession(expectedCookie)
                .withIsSyncCompletedReturning(true)
                .withRegisterClientReturning(RegisterClientResult.Success(TestClient.CLIENT))
                .arrange()

            loginViewModel.handleSSOResult(DeepLinkResult.SSOLogin.Success(expectedCookie, SERVER_CONFIG.id), SERVER_CONFIG.links)
            advanceUntilIdle()
            coVerify(exactly = 1) {
                arrangement.ssoExtension.establishSSOSession(
                    eq(expectedCookie),
                    eq(SERVER_CONFIG.id),
                    eq(SERVER_CONFIG.links),
                    capture(onAuthScopeFailureSlot),
                    capture(onSSOLoginFailureSlot),
                    capture(onAddAuthenticatedUserFailureSlot),
                    capture(onSuccessEstablishSSOSessionSlot)
                )
            }
            onAddAuthenticatedUserFailureSlot.captured.invoke(AddAuthenticatedUserUseCase.Result.Failure.UserAlreadyExists)

            coVerify(exactly = 0) { arrangement.getOrRegisterClientUseCase(any()) }
            loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Error.DialogError.UserAlreadyExists>()
        }

    @Test
    fun `given getOrRegister returns TooManyClients, when establishSSOSession, then TooManyClients is passed`() =
        runTest {
            val expectedCookie = "some-cookie"
            val (arrangement, loginViewModel) = Arrangement()
                .withEstablishSSOSession(expectedCookie)
                .withRegisterClientReturning(RegisterClientResult.Failure.TooManyClients)
                .withIsSyncCompletedReturning(true)
                .arrange()

            loginViewModel.establishSSOSession(expectedCookie, SERVER_CONFIG.id, SERVER_CONFIG.links)
            advanceUntilIdle()
            coVerify(exactly = 1) {
                arrangement.ssoExtension.establishSSOSession(
                    eq(expectedCookie),
                    eq(SERVER_CONFIG.id),
                    eq(SERVER_CONFIG.links),
                    capture(onAuthScopeFailureSlot),
                    capture(onSSOLoginFailureSlot),
                    capture(onAddAuthenticatedUserFailureSlot),
                    capture(onSuccessEstablishSSOSessionSlot)
                )
            }
            onSuccessEstablishSSOSessionSlot.captured.invoke(TestUser.USER_ID)

            coVerify(exactly = 1) { arrangement.getOrRegisterClientUseCase(any()) }
            loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Error.TooManyDevicesError>()
        }

    @Test
    fun `given establishSSOSession called with custom server config, then establish SSO session using custom server config`() = runTest {
        val expectedCookie = "some-cookie"
        val customConfig = newServerConfig(2)
        val (arrangement, loginViewModel) = Arrangement()
            .withEstablishSSOSession(expectedCookie, customConfig)
            .withRegisterClientReturning(RegisterClientResult.Success(TestClient.CLIENT))
            .withIsSyncCompletedReturning(true)
            .arrange()

        loginViewModel.establishSSOSession(expectedCookie, customConfig.id, customConfig.links)
        advanceUntilIdle()
        coVerify(exactly = 1) {
            arrangement.ssoExtension.establishSSOSession(
                eq(expectedCookie),
                eq(customConfig.id),
                eq(customConfig.links),
                capture(onAuthScopeFailureSlot),
                capture(onSSOLoginFailureSlot),
                capture(onAddAuthenticatedUserFailureSlot),
                capture(onSuccessEstablishSSOSessionSlot)
            )
        }
        onSuccessEstablishSSOSessionSlot.captured.invoke(TestUser.USER_ID)

        coVerify(exactly = 1) { arrangement.getOrRegisterClientUseCase(any()) }
        loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Success>()
    }

    @Test
    fun `given email, when clicking login, then start the domain lookup flow`() = runTest {
        val expectedEmail = "email@wire.com"
        val customConfig = newServerConfig(2)
        val (arrangement, loginViewModel) = Arrangement()
            .withDomainLookupReturning(DomainLookupUseCase.Result.Success(customConfig.links))
            .withIsSyncCompletedReturning(true)
            .arrange()

        loginViewModel.ssoTextState.setTextAndPlaceCursorAtEnd(expectedEmail)
        loginViewModel.domainLookupFlow()
        advanceUntilIdle()

        coVerify(exactly = 1) { arrangement.authenticationScope.domainLookup(expectedEmail) }
        assertEquals(customConfig.links, loginViewModel.loginState.customServerDialogState!!.serverLinks)
    }

    @Test
    fun `given error, when doing domain lookup, then error state is updated`() = runTest {
        val expectedEmail = "email@wire.com"
        val expected = CoreFailure.Unknown(IOException())
        val (arrangement, loginViewModel) = Arrangement()
            .withDomainLookupReturning(DomainLookupUseCase.Result.Failure(expected))
            .withIsSyncCompletedReturning(true)
            .arrange()

        loginViewModel.ssoTextState.setTextAndPlaceCursorAtEnd(expectedEmail)
        loginViewModel.domainLookupFlow()
        advanceUntilIdle()

        coVerify(exactly = 1) { arrangement.authenticationScope.domainLookup(expectedEmail) }
        loginViewModel.loginState.customServerDialogState shouldBeEqualTo null
        loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Error.DialogError.GenericError>().let {
            it.coreFailure shouldBeEqualTo expected
        }
    }

    @Test
    fun `given backend switch confirmed and sso init successful, then open web url with updated server config`() =
        runTest(dispatcherProvider.main()) {
            val expectedSSOCode = "wire-fd994b20-b9af-11ec-ae36-00163e9b33ca"
            val customConfig = newServerConfig(2)
            val expectedUrl = "https://wire.com/sso"
            val (arrangement, loginViewModel) = Arrangement()
                .withInitiateSSO(expectedSSOCode, customConfig)
                .withFetchSSOSettings(customConfig)
                .arrange()

            loginViewModel.ssoTextState.setTextAndPlaceCursorAtEnd(expectedSSOCode)
            loginViewModel.loginState =
                loginViewModel.loginState.copy(customServerDialogState = CustomServerDetailsDialogState(customConfig.links))

            loginViewModel.openWebUrl.test {
                loginViewModel.onCustomServerDialogConfirm()
                advanceUntilIdle()
                coVerify(exactly = 1) {
                    arrangement.ssoExtension.fetchDefaultSSOCode(
                        eq(customConfig.links),
                        capture(onAuthScopeFailureSlot),
                        capture(onFetchSSOSettingsFailureSlot),
                        capture(onSuccessFetchSSOCodeSlot)
                    )
                }
                onSuccessFetchSSOCodeSlot.captured.invoke(expectedSSOCode)

                advanceUntilIdle()
                coVerify(exactly = 1) {
                    arrangement.ssoExtension.initiateSSO(
                        eq(customConfig.links),
                        eq(expectedSSOCode),
                        capture(onAuthScopeFailureSlot),
                        capture(onSSOInitiateFailureSlot),
                        capture(onSuccessSlot)
                    )
                }
                onSuccessSlot.captured.invoke(expectedUrl, customConfig.links)

                val (url, customServerConfig) = awaitItem()
                assertEquals(expectedUrl, url)
                assertEquals(customConfig.links, customServerConfig)

                loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Default>()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `given backend switch confirmed, when the new server has a default sso code, then initiate sso login`() =
        runTest(dispatcherProvider.main()) {
            val expectedSSOCode = "wire-fd994b20-b9af-11ec-ae36-00163e9b33ca"
            val customConfig = newServerConfig(2)
            val (arrangement, loginViewModel) = Arrangement()
                .withInitiateSSO(expectedSSOCode, customConfig)
                .withFetchSSOSettings(customConfig)
                .withValidateEmailReturning(true)
                .arrange()

            loginViewModel.ssoTextState.setTextAndPlaceCursorAtEnd(expectedSSOCode)
            loginViewModel.loginState =
                loginViewModel.loginState.copy(customServerDialogState = CustomServerDetailsDialogState(customConfig.links))

            loginViewModel.onCustomServerDialogConfirm()
            advanceUntilIdle()
            coVerify(exactly = 1) {
                arrangement.ssoExtension.fetchDefaultSSOCode(
                    eq(customConfig.links),
                    capture(onAuthScopeFailureSlot),
                    capture(onFetchSSOSettingsFailureSlot),
                    capture(onSuccessFetchSSOCodeSlot)
                )
            }
            onSuccessFetchSSOCodeSlot.captured.invoke(expectedSSOCode)
        }

    @Test
    fun `given backend switch confirmed, when the new server does not have a default sso code, then do not initiate sso login`() =
        runTest(dispatcherProvider.main()) {
            val expectedSSOCode = "wire-fd994b20-b9af-11ec-ae36-00163e9b33ca"
            val customConfig = newServerConfig(2)
            val (arrangement, loginViewModel) = Arrangement()
                .withInitiateSSO(expectedSSOCode, customConfig)
                .withFetchSSOSettings(customConfig)
                .withValidateEmailReturning(true)
                .arrange()

            loginViewModel.ssoTextState.setTextAndPlaceCursorAtEnd(expectedSSOCode)
            loginViewModel.loginState =
                loginViewModel.loginState.copy(customServerDialogState = CustomServerDetailsDialogState(customConfig.links))

            loginViewModel.onCustomServerDialogConfirm()
            advanceUntilIdle()
            coVerify(exactly = 1) {
                arrangement.ssoExtension.fetchDefaultSSOCode(
                    eq(customConfig.links),
                    capture(onAuthScopeFailureSlot),
                    capture(onFetchSSOSettingsFailureSlot),
                    capture(onSuccessFetchSSOCodeSlot)
                )
            }
            coVerify(exactly = 0) {
                arrangement.ssoExtension.initiateSSO(
                    eq(customConfig.links),
                    eq(expectedSSOCode),
                    capture(onAuthScopeFailureSlot),
                    capture(onSSOInitiateFailureSlot),
                    capture(onSuccessSlot)
                )
            }
            onSuccessFetchSSOCodeSlot.captured.invoke(null)
        }

    @Test
    fun `given error, when checking for server default SSO code, then do not initiate sso login`() = runTest(dispatcherProvider.main()) {
        val expectedSSOCode = "wire-fd994b20-b9af-11ec-ae36-00163e9b33ca"
        val customConfig = newServerConfig(2)
        val (arrangement, loginViewModel) = Arrangement()
            .withInitiateSSO(expectedSSOCode, customConfig)
            .withFetchSSOSettings(customConfig)
            .withValidateEmailReturning(true)
            .arrange()

        loginViewModel.ssoTextState.setTextAndPlaceCursorAtEnd(expectedSSOCode)
        loginViewModel.loginState =
            loginViewModel.loginState.copy(customServerDialogState = CustomServerDetailsDialogState(customConfig.links))

        loginViewModel.onCustomServerDialogConfirm()
        advanceUntilIdle()
        coVerify(exactly = 1) {
            arrangement.ssoExtension.fetchDefaultSSOCode(
                eq(customConfig.links),
                capture(onAuthScopeFailureSlot),
                capture(onFetchSSOSettingsFailureSlot),
                capture(onSuccessFetchSSOCodeSlot)
            )
        }
        coVerify(exactly = 0) {
            arrangement.ssoExtension.initiateSSO(
                eq(customConfig.links),
                eq(expectedSSOCode),
                capture(onAuthScopeFailureSlot),
                capture(onSSOInitiateFailureSlot),
                capture(onSuccessSlot)
            )
        }
        onFetchSSOSettingsFailureSlot.captured.invoke(FetchSSOSettingsUseCase.Result.Failure(CoreFailure.Unknown(IOException())))
    }

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
            withFetchSSOSettings()
        }

        fun withIsSyncCompletedReturning(isComplete: Boolean) = apply {
            every { userDataStoreProvider.getOrCreate(any()).initialSyncCompleted } returns flowOf(isComplete)
        }

        fun withRegisterClientReturning(result: RegisterClientResult) = apply {
            coEvery { getOrRegisterClientUseCase(any()) } returns result
        }

        fun withDomainLookupReturning(expected: DomainLookupUseCase.Result) = apply {
            coEvery { authenticationScope.domainLookup(any()) } returns expected
        }

        fun withInitiateSSO(ssoCode: String, customConfig: ServerConfig = SERVER_CONFIG) = apply {
            coEvery {
                ssoExtension.initiateSSO(
                    eq(customConfig.links),
                    eq(ssoCode),
                    any(),
                    any(),
                    any()
                )
            } returns Unit
        }

        fun withEstablishSSOSession(cookie: String, customConfig: ServerConfig = SERVER_CONFIG) = apply {
            coEvery {
                ssoExtension.establishSSOSession(
                    eq(cookie),
                    eq(customConfig.id),
                    eq(customConfig.links),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns Unit
        }

        fun withFetchSSOSettings(customConfig: ServerConfig = SERVER_CONFIG) = apply {
            coEvery {
                ssoExtension.fetchDefaultSSOCode(
                    eq(customConfig.links),
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
            savedStateHandle = savedStateHandle,
            addAuthenticatedUser = addAuthenticatedUserUseCase,
            validateEmailUseCase = validateEmailUseCase,
            coreLogic = coreLogic,
            clientScopeProviderFactory = clientScopeProviderFactory,
            userDataStoreProvider = userDataStoreProvider,
            serverConfig = SERVER_CONFIG.links,
            ssoExtension = ssoExtension,
        )
    }

    companion object {
        val onAuthScopeFailureSlot = slot<((AutoVersionAuthScopeUseCase.Result.Failure) -> Unit)>()
        val onSSOInitiateFailureSlot = slot<((SSOInitiateLoginResult.Failure) -> Unit)>()
        val onSuccessSlot = slot<(suspend (redirectUrl: String, serverConfig: ServerConfig.Links) -> Unit)>()
        val onSSOLoginFailureSlot = slot<((SSOLoginSessionResult.Failure) -> Unit)>()
        val onAddAuthenticatedUserFailureSlot = slot<((AddAuthenticatedUserUseCase.Result.Failure) -> Unit)>()
        val onSuccessEstablishSSOSessionSlot = slot<(suspend (UserId) -> Unit)>()
        val onSuccessFetchSSOCodeSlot = slot<(suspend (String?) -> Unit)>()
        val onFetchSSOSettingsFailureSlot = slot<(FetchSSOSettingsUseCase.Result.Failure) -> Unit>()

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
