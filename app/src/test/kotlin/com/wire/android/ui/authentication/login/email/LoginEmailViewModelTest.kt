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

@file:Suppress("MaxLineLength")

package com.wire.android.ui.authentication.login.email

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.wire.android.assertions.shouldBeEqualTo
import com.wire.android.assertions.shouldBeInstanceOf
import com.wire.android.assertions.shouldNotBeEqualTo
import com.wire.android.assertions.shouldNotBeInstanceOf
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
import com.wire.android.util.newServerConfig
import com.wire.android.util.ui.CountdownTimer
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.CommonApiVersionType
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.auth.AccountTokens
import com.wire.kalium.logic.data.auth.verification.VerifiableAction
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.user.SsoId
import com.wire.kalium.logic.data.user.SsoManagedBy
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.AuthenticationResult
import com.wire.kalium.logic.feature.auth.AuthenticationScope
import com.wire.kalium.logic.feature.auth.LoginUseCase
import com.wire.kalium.logic.feature.auth.LogoutUseCase
import com.wire.kalium.logic.feature.auth.PersistSelfUserEmailResult
import com.wire.kalium.logic.feature.auth.PersistSelfUserEmailUseCase
import com.wire.kalium.logic.feature.auth.ValidateEmailUseCase
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.auth.verification.RequestSecondFactorVerificationCodeUseCase
import com.wire.kalium.logic.feature.client.ClientScope
import com.wire.kalium.logic.feature.client.GetOrRegisterClientUseCase
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import com.wire.kalium.logic.feature.session.DeleteSessionUseCase
import com.wire.kalium.logic.feature.session.UpdateCurrentSessionUseCase
import com.wire.kalium.logic.feature.user.UserScope
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class, SnapshotExtension::class, NavigationTestExtension::class)
class LoginEmailViewModelTest {

    private val dispatcherProvider = TestDispatcherProvider(StandardTestDispatcher())

    private fun runTest(test: suspend TestScope.() -> Unit) = runTest(dispatcherProvider.main(), testBody = test)

    @Test
    fun `given empty strings, when entering credentials, then button is disabled`() {
        val (arrangement, loginViewModel) = Arrangement().arrange()
        loginViewModel.passwordTextState.setTextAndPlaceCursorAtEnd(String.EMPTY)
        loginViewModel.userIdentifierTextState.setTextAndPlaceCursorAtEnd(String.EMPTY)
        loginViewModel.loginState.loginEnabled shouldBeEqualTo false
        loginViewModel.loginState.flowState.shouldNotBeInstanceOf<LoginState.Loading>()
    }

    @Test
    fun `given non-empty strings, when entering credentials, then button is enabled`() {
        val (arrangement, loginViewModel) = Arrangement().arrange()
        loginViewModel.passwordTextState.setTextAndPlaceCursorAtEnd("abc")
        loginViewModel.userIdentifierTextState.setTextAndPlaceCursorAtEnd("abc")
        loginViewModel.loginState.loginEnabled shouldBeEqualTo true
        loginViewModel.loginState.flowState.shouldNotBeInstanceOf<LoginState.Loading>()
    }

    @Test
    fun `given button is clicked, when logging in, then show loading`() = runTest {
        val (arrangement, loginViewModel) = Arrangement()
            .withLoginReturning(AuthenticationResult.Failure.InvalidCredentials.InvalidPasswordIdentityCombination)
            .withAddAuthenticatedUserReturning(AddAuthenticatedUserUseCase.Result.Success(USER_ID))
            .arrange()

        loginViewModel.passwordTextState.setTextAndPlaceCursorAtEnd("abc")
        loginViewModel.userIdentifierTextState.setTextAndPlaceCursorAtEnd("abc")
        loginViewModel.loginState.loginEnabled shouldBeEqualTo true
        loginViewModel.loginState.flowState.shouldNotBeInstanceOf<LoginState.Loading>()
        loginViewModel.login()
        loginViewModel.loginState.loginEnabled shouldBeEqualTo false
        loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Loading>()
        advanceUntilIdle()
        loginViewModel.loginState.loginEnabled shouldBeEqualTo true
        loginViewModel.loginState.flowState.shouldNotBeInstanceOf<LoginState.Loading>()
    }

    @Test
    fun `given button is clicked and initial sync is completed, when login returns Success, then navigate to home screen`() = runTest {
        val (arrangement, loginViewModel) = Arrangement()
            .withLoginReturning(
                AuthenticationResult.Success(
                    authData = AUTH_TOKEN,
                    ssoID = SSO_ID,
                    managedBy = MANAGED_BY,
                    serverConfigId = SERVER_CONFIG.id,
                    proxyCredentials = null
                )
            )
            .withAddAuthenticatedUserReturning(AddAuthenticatedUserUseCase.Result.Success(USER_ID))
            .withValidateEmailReturning(true)
            .withPersistEmailReturning(PersistSelfUserEmailResult.Success)
            .withGetOrRegisterClientReturning(RegisterClientResult.Success(CLIENT))
            .withInitialSyncCompletedReturning(true)
            .arrange()
        val password = "abc"

        loginViewModel.passwordTextState.setTextAndPlaceCursorAtEnd(password)

        loginViewModel.login()
        advanceUntilIdle()
        coVerify(exactly = 1) { arrangement.loginUseCase(any(), any(), any(), any(), any()) }
        coVerify(exactly = 1) { arrangement.persistSelfUserEmailUseCase(any()) }
        coVerify(exactly = 1) { arrangement.getOrRegisterClientUseCase(any()) }
        loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Success>().let {
            it.initialSyncCompleted shouldBeEqualTo true
            it.isE2EIRequired shouldBeEqualTo false
        }
    }

    @Test
    fun `given button is clicked and initial sync is not completed, when login returns Success, then navigate to initial sync screen`() =
        runTest {
            val password = "abc"
            val (arrangement, loginViewModel) = Arrangement()
                .withLoginReturning(AuthenticationResult.Success(AUTH_TOKEN, SSO_ID, MANAGED_BY, SERVER_CONFIG.id, null))
                .withAddAuthenticatedUserReturning(AddAuthenticatedUserUseCase.Result.Success(USER_ID))
                .withValidateEmailReturning(true)
                .withPersistEmailReturning(PersistSelfUserEmailResult.Success)
                .withGetOrRegisterClientReturning(RegisterClientResult.Success(CLIENT))
                .withInitialSyncCompletedReturning(false)
                .arrange()

            loginViewModel.passwordTextState.setTextAndPlaceCursorAtEnd(password)

            loginViewModel.login()
            advanceUntilIdle()
            coVerify(exactly = 1) { arrangement.loginUseCase(any(), any(), any(), any(), any()) }
            coVerify(exactly = 1) { arrangement.persistSelfUserEmailUseCase(any()) }
            coVerify(exactly = 1) { arrangement.getOrRegisterClientUseCase(any()) }
            loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Success>().let {
                it.initialSyncCompleted shouldBeEqualTo false
                it.isE2EIRequired shouldBeEqualTo false
            }
        }

    @Test
    fun `given button is clicked, when login returns InvalidUserIdentifier error, then InvalidUserIdentifierError is passed`() = runTest {
        val (arrangement, loginViewModel) = Arrangement()
            .withLoginReturning(AuthenticationResult.Failure.InvalidUserIdentifier)
            .arrange()

        loginViewModel.login()
        advanceUntilIdle()
        loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Error.TextFieldError.InvalidValue>()
    }

    @Test
    fun `given button is clicked, when login returns InvalidCredentials error, then InvalidCredentialsError is passed`() = runTest {
        val (arrangement, loginViewModel) = Arrangement()
            .withLoginReturning(AuthenticationResult.Failure.InvalidCredentials.InvalidPasswordIdentityCombination)
            .arrange()

        loginViewModel.login()
        advanceUntilIdle()
        loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Error.DialogError.InvalidCredentialsError>()
    }

    @Test
    fun `given button is clicked, when login returns Generic error, then GenericError is passed`() = runTest {
        val networkFailure = NetworkFailure.NoNetworkConnection(null)
        val (arrangement, loginViewModel) = Arrangement()
            .withLoginReturning(AuthenticationResult.Failure.Generic(networkFailure))
            .arrange()

        loginViewModel.login()
        advanceUntilIdle()

        loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Error.DialogError.GenericError>().let {
            it.coreFailure shouldBeEqualTo networkFailure
        }
    }

    @Test
    fun `given dialog is dismissed, when login returns DialogError, then hide error`() = runTest {
        val (arrangement, loginViewModel) = Arrangement()
            .withLoginReturning(AuthenticationResult.Failure.InvalidCredentials.InvalidPasswordIdentityCombination)
            .arrange()

        loginViewModel.login()
        advanceUntilIdle()
        loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Error.DialogError.InvalidCredentialsError>()
        loginViewModel.clearLoginErrors()
        loginViewModel.loginState.flowState.shouldNotBeInstanceOf<LoginState.Error>()
    }

    @Test
    fun `given button is clicked, when addAuthenticatedUser returns UserAlreadyExists error, then UserAlreadyExists is passed`() = runTest {
        val (arrangement, loginViewModel) = Arrangement()
            .withLoginReturning(AuthenticationResult.Success(AUTH_TOKEN, SSO_ID, MANAGED_BY, SERVER_CONFIG.id, null))
            .withAddAuthenticatedUserReturning(AddAuthenticatedUserUseCase.Result.Failure.UserAlreadyExists)
            .arrange()

        loginViewModel.login()
        advanceUntilIdle()

        loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Error.DialogError.UserAlreadyExists>()
    }

    @Test
    fun `given login fails with missing 2fa, when logging in, then should send an email to input`() = runTest {
        val email = "some.email@example.org"
        val (arrangement, loginViewModel) = Arrangement()
            .withLoginReturning(AuthenticationResult.Failure.InvalidCredentials.Missing2FA)
            .withRequestSecondFactorVerificationCodeReturning(RequestSecondFactorVerificationCodeUseCase.Result.Success)
            .arrange()

        loginViewModel.userIdentifierTextState.setTextAndPlaceCursorAtEnd(email)

        loginViewModel.login()
        advanceUntilIdle()

        coVerify(exactly = 1) {
            arrangement.requestSecondFactorCodeUseCase(email, VerifiableAction.LOGIN_OR_CLIENT_REGISTRATION)
        }
    }

    @Test
    fun `given missing 2fa, when logging in, then email should be enabled and not loading`() = runTest {
        val email = "some.email@example.org"
        val (arrangement, loginViewModel) = Arrangement()
            .withLoginReturning(AuthenticationResult.Failure.InvalidCredentials.Missing2FA)
            .withRequestSecondFactorVerificationCodeReturning(RequestSecondFactorVerificationCodeUseCase.Result.Success)
            .arrange()

        loginViewModel.userIdentifierTextState.setTextAndPlaceCursorAtEnd(email)
        loginViewModel.passwordTextState.setTextAndPlaceCursorAtEnd("somePassword")
        loginViewModel.login()
        advanceUntilIdle()

        loginViewModel.loginState.flowState.shouldNotBeInstanceOf<LoginState.Loading>()
        loginViewModel.loginState.loginEnabled shouldBeEqualTo true
    }

    @Test
    fun `given login fails with 2fa missing and 2fa request succeeds, when logging in, then should request user input`() = runTest {
        val email = "some.email@example.org"
        val (arrangement, loginViewModel) = Arrangement()
            .withLoginReturning(AuthenticationResult.Failure.InvalidCredentials.Missing2FA)
            .withRequestSecondFactorVerificationCodeReturning(RequestSecondFactorVerificationCodeUseCase.Result.Success)
            .arrange()

        loginViewModel.userIdentifierTextState.setTextAndPlaceCursorAtEnd(email)

        loginViewModel.login()
        advanceUntilIdle()

        loginViewModel.secondFactorVerificationCodeState.isCodeInputNecessary shouldBeEqualTo true
        coVerify(exactly = 1) {
            arrangement.requestSecondFactorCodeUseCase(email, VerifiableAction.LOGIN_OR_CLIENT_REGISTRATION)
        }
    }

    @Test
    fun `given login fails with 2fa missing and 2fa request fails generically, when logging in, then should NOT request user input`() =
        runTest {
            val email = "some.email@example.org"
            val (arrangement, loginViewModel) = Arrangement()
                .withLoginReturning(AuthenticationResult.Failure.InvalidCredentials.Missing2FA)
                .withRequestSecondFactorVerificationCodeReturning(
                    RequestSecondFactorVerificationCodeUseCase.Result.Failure.Generic(CoreFailure.Unknown(null))
                )
                .arrange()

            loginViewModel.userIdentifierTextState.setTextAndPlaceCursorAtEnd(email)

            loginViewModel.login()
            advanceUntilIdle()

            loginViewModel.secondFactorVerificationCodeState.isCodeInputNecessary shouldBeEqualTo false
            coVerify(exactly = 1) {
                arrangement.requestSecondFactorCodeUseCase(email, VerifiableAction.LOGIN_OR_CLIENT_REGISTRATION)
            }
        }

    @Test
    fun `given 2fa code request fails with too many requests, when logging in, then should request user input`() = runTest {
        val email = "some.email@example.org"
        val (arrangement, loginViewModel) = Arrangement()
            .withLoginReturning(AuthenticationResult.Failure.InvalidCredentials.Missing2FA)
            .withRequestSecondFactorVerificationCodeReturning(RequestSecondFactorVerificationCodeUseCase.Result.Failure.TooManyRequests)
            .arrange()

        loginViewModel.userIdentifierTextState.setTextAndPlaceCursorAtEnd(email)

        loginViewModel.login()
        advanceUntilIdle()

        loginViewModel.secondFactorVerificationCodeState.isCodeInputNecessary shouldBeEqualTo true
        coVerify(exactly = 1) {
            arrangement.requestSecondFactorCodeUseCase(email, VerifiableAction.LOGIN_OR_CLIENT_REGISTRATION)
        }
    }

    @Test
    fun `given login fails with missing 2fa, when logging in, then should state 2FA input is needed`() = runTest {
        val email = "some.email@example.org"
        val (arrangement, loginViewModel) = Arrangement()
            .withLoginReturning(AuthenticationResult.Failure.InvalidCredentials.Missing2FA)
            .withRequestSecondFactorVerificationCodeReturning(RequestSecondFactorVerificationCodeUseCase.Result.Success)
            .arrange()

        loginViewModel.userIdentifierTextState.setTextAndPlaceCursorAtEnd(email)
        loginViewModel.login()
        advanceUntilIdle()
        loginViewModel.secondFactorVerificationCodeState.isCodeInputNecessary shouldBeEqualTo true
    }

    @Test
    fun `given login fails with invalid 2fa, when logging in, then should mark the current code as invalid`() = runTest {
        val (arrangement, loginViewModel) = Arrangement()
            .withLoginReturning(AuthenticationResult.Failure.InvalidCredentials.Invalid2FA)
            .arrange()
        loginViewModel.userIdentifierTextState.setTextAndPlaceCursorAtEnd("some.email@example.org")

        loginViewModel.login()
        advanceUntilIdle()
        loginViewModel.secondFactorVerificationCodeState.isCurrentCodeInvalid shouldBeEqualTo true
    }

    @Test
    fun `given 2fa is needed, when code is filled, then should login with entered code and navigate out of login`() = runTest {
        val email = "some.email@example.org"
        val code = "123456"
        val (arrangement, loginViewModel) = Arrangement()
            .withLoginReturning(AuthenticationResult.Success(AUTH_TOKEN, SSO_ID, MANAGED_BY, SERVER_CONFIG.id, null))
            .withAddAuthenticatedUserReturning(AddAuthenticatedUserUseCase.Result.Success(USER_ID))
            .withValidateEmailReturning(true)
            .withPersistEmailReturning(PersistSelfUserEmailResult.Success)
            .withGetOrRegisterClientReturning(RegisterClientResult.Success(CLIENT))
            .withInitialSyncCompletedReturning(true)
            .arrange()

        loginViewModel.userIdentifierTextState.setTextAndPlaceCursorAtEnd(email)
        loginViewModel.secondFactorVerificationCodeTextState.setTextAndPlaceCursorAtEnd(code)
        advanceUntilIdle()
        coVerify(exactly = 1) { arrangement.loginUseCase(email, any(), any(), any(), code) }
        coVerify(exactly = 1) {
            arrangement.getOrRegisterClientUseCase(any())
        }
        loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Success>()
    }

    @Test
    fun `given 2fa login succeeds and registration fails, when code is filled, then should no longer require input`() = runTest {
        val email = "some.email@example.org"
        val code = "123456"
        val (arrangement, loginViewModel) = Arrangement()
            .withLoginReturning(AuthenticationResult.Success(AUTH_TOKEN, SSO_ID, MANAGED_BY, SERVER_CONFIG.id, null))
            .withAddAuthenticatedUserReturning(AddAuthenticatedUserUseCase.Result.Success(USER_ID))
            .withValidateEmailReturning(true)
            .withPersistEmailReturning(PersistSelfUserEmailResult.Success)
            .withGetOrRegisterClientReturning(RegisterClientResult.Failure.TooManyClients)
            .withInitialSyncCompletedReturning(true)
            .withDeleteSessionReturning(DeleteSessionUseCase.Result.Success)
            .withUpdateCurrentSessionReturning(UpdateCurrentSessionUseCase.Result.Success)
            .arrange()

        loginViewModel.userIdentifierTextState.setTextAndPlaceCursorAtEnd(email)
        loginViewModel.secondFactorVerificationCodeTextState.setTextAndPlaceCursorAtEnd(code)
        advanceUntilIdle()
        loginViewModel.secondFactorVerificationCodeState.isCodeInputNecessary shouldBeEqualTo false
    }

    @Test
    fun `given 2fa is needed, when code is filled, then should register client without explicit 2fa code`() = runTest {
        val email = "some.email@example.org"
        val code = "123456"
        val (arrangement, loginViewModel) = Arrangement()
            .withLoginReturning(AuthenticationResult.Success(AUTH_TOKEN, SSO_ID, MANAGED_BY, SERVER_CONFIG.id, null))
            .withAddAuthenticatedUserReturning(AddAuthenticatedUserUseCase.Result.Success(USER_ID))
            .withValidateEmailReturning(true)
            .withPersistEmailReturning(PersistSelfUserEmailResult.Success)
            .withGetOrRegisterClientReturning(RegisterClientResult.Success(CLIENT))
            .withInitialSyncCompletedReturning(true)
            .arrange()

        loginViewModel.userIdentifierTextState.setTextAndPlaceCursorAtEnd(email)
        loginViewModel.secondFactorVerificationCodeTextState.setTextAndPlaceCursorAtEnd(code)
        advanceUntilIdle()
        coVerify(exactly = 1) { arrangement.getOrRegisterClientUseCase(match { it.secondFactorVerificationCode == null }) }
    }

    @Test
    fun `given 2fa is needed, when user used handle to login, then show correct error message`() = runTest {
        val email = "some.handle"
        val code = "123456"
        val (arrangement, loginViewModel) = Arrangement()
            .withLoginReturning(AuthenticationResult.Failure.InvalidCredentials.Missing2FA)
            .arrange()

        loginViewModel.userIdentifierTextState.setTextAndPlaceCursorAtEnd(email)
        loginViewModel.secondFactorVerificationCodeTextState.setTextAndPlaceCursorAtEnd(code)
        advanceUntilIdle()
        coVerify(exactly = 0) { arrangement.addAuthenticatedUserUseCase(any(), any(), any(), any()) }
        coVerify(exactly = 0) { arrangement.getOrRegisterClientUseCase(any()) }
        assertEquals(LoginState.Error.DialogError.Request2FAWithHandle, loginViewModel.loginState.flowState)
    }

    @Test
    fun `given email, when logging in, then persist email`() = runTest {
        val email = "some.email@example.org"
        val (arrangement, loginViewModel) = Arrangement()
            .withLoginReturning(AuthenticationResult.Success(AUTH_TOKEN, SSO_ID, MANAGED_BY, SERVER_CONFIG.id, null))
            .withAddAuthenticatedUserReturning(AddAuthenticatedUserUseCase.Result.Success(USER_ID))
            .withValidateEmailReturning(true)
            .withPersistEmailReturning(PersistSelfUserEmailResult.Success)
            .withGetOrRegisterClientReturning(RegisterClientResult.Success(CLIENT))
            .withInitialSyncCompletedReturning(true)
            .arrange()

        loginViewModel.userIdentifierTextState.setTextAndPlaceCursorAtEnd(email)
        loginViewModel.login()
        advanceUntilIdle()

        coVerify(exactly = 1) { arrangement.persistSelfUserEmailUseCase(eq(email)) }
    }

    @Test
    fun `given handle, when logging in, then do not persist email`() = runTest {
        val handle = "some.handle"
        val (arrangement, loginViewModel) = Arrangement()
            .withLoginReturning(AuthenticationResult.Success(AUTH_TOKEN, SSO_ID, MANAGED_BY, SERVER_CONFIG.id, null))
            .withAddAuthenticatedUserReturning(AddAuthenticatedUserUseCase.Result.Success(USER_ID))
            .withValidateEmailReturning(false)
            .withGetOrRegisterClientReturning(RegisterClientResult.Success(CLIENT))
            .withInitialSyncCompletedReturning(true)
            .arrange()

        loginViewModel.userIdentifierTextState.setTextAndPlaceCursorAtEnd(handle)
        loginViewModel.login()
        advanceUntilIdle()

        coVerify(exactly = 0) { arrangement.persistSelfUserEmailUseCase(any()) }
    }

    @Test
    fun `given email and persist email failure, when logging in, then GenericError is passed`() = runTest {
        val email = "some.email@example.org"
        val failure = CoreFailure.Unknown(null)
        val (arrangement, loginViewModel) = Arrangement()
            .withLoginReturning(AuthenticationResult.Success(AUTH_TOKEN, SSO_ID, MANAGED_BY, SERVER_CONFIG.id, null))
            .withAddAuthenticatedUserReturning(AddAuthenticatedUserUseCase.Result.Success(USER_ID))
            .withValidateEmailReturning(true)
            .withPersistEmailReturning(PersistSelfUserEmailResult.Failure(failure))
            .withDeleteSessionReturning(DeleteSessionUseCase.Result.Success)
            .withUpdateCurrentSessionReturning(UpdateCurrentSessionUseCase.Result.Success)
            .arrange()

        loginViewModel.userIdentifierTextState.setTextAndPlaceCursorAtEnd(email)
        loginViewModel.login()
        advanceUntilIdle()

        loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Error.DialogError.GenericError>().let {
            it.coreFailure shouldBeEqualTo failure
        }
    }

    @Test
    fun `given successful login, when logging in, then update login job data with proper values`() = runTest {
        val previousUserId = UserId("currentUserId", "domain")
        val newUserId = UserId("newUserId", "domain")
        val (arrangement, loginViewModel) = Arrangement()
            .withCurrentSessionReturning(CurrentSessionResult.Success(AccountInfo.Valid(previousUserId)))
            .withLoginReturning(
                AuthenticationResult.Success(
                    authData = AUTH_TOKEN.copy(userId = newUserId),
                    ssoID = SSO_ID,
                    managedBy = MANAGED_BY,
                    serverConfigId = SERVER_CONFIG.id,
                    proxyCredentials = null
                )
            )
            .withAddAuthenticatedUserReturning(AddAuthenticatedUserUseCase.Result.Success(newUserId))
            .withValidateEmailReturning(true)
            .withPersistEmailReturning(PersistSelfUserEmailResult.Success)
            .withGetOrRegisterClientReturning(RegisterClientResult.Success(CLIENT))
            .withInitialSyncCompletedReturning(true)
            .arrange()

        loginViewModel.loginJobData.test {
            // initial value
            awaitItem() shouldBeEqualTo null
            // start the login process
            loginViewModel.login()
            advanceUntilIdle()
            // after starting login, first is previous session user id
            awaitItem()?.let {
                it.job shouldNotBeEqualTo null
                it.previousSessionUserId shouldBeEqualTo previousUserId
                it.newSessionUserId shouldBeEqualTo null
            }
            // then after login success, new session user id is set
            awaitItem()?.let {
                it.job shouldNotBeEqualTo null
                it.previousSessionUserId shouldBeEqualTo previousUserId
                it.newSessionUserId shouldBeEqualTo newUserId
            }
            // after completion, it should be null
            awaitItem() shouldBeEqualTo null
        }
    }

    @Test
    fun `given login job data, when canceling login, then cancel login job and set state to Canceled`() = runTest {
        // given
        val (arrangement, viewModel) = Arrangement()
            .withDeleteSessionReturning(DeleteSessionUseCase.Result.Success)
            .withUpdateCurrentSessionReturning(UpdateCurrentSessionUseCase.Result.Success)
            .arrange()
        val loginJob: Job = mockk(relaxUnitFun = true)
        viewModel.loginJobData.value = LoginJobData(job = loginJob)
        // when
        viewModel.cancelLogin()
        advanceUntilIdle()
        // then
        viewModel.loginState.flowState shouldBeEqualTo LoginState.Canceled
        coVerify(exactly = 1) {
            loginJob.cancel()
        }
    }

    @Test
    fun `given no new session yet, when canceling login, then do not logout and delete session`() = runTest {
        // given
        val (arrangement, viewModel) = Arrangement()
            .withDeleteSessionReturning(DeleteSessionUseCase.Result.Success)
            .withUpdateCurrentSessionReturning(UpdateCurrentSessionUseCase.Result.Success)
            .arrange()
        viewModel.loginJobData.value = LoginJobData(job = mockk(relaxUnitFun = true), newSessionUserId = null)
        // when
        viewModel.cancelLogin()
        advanceUntilIdle()
        // then
        coVerify(exactly = 0) {
            arrangement.logoutUseCase(LogoutReason.SELF_HARD_LOGOUT, true)
            arrangement.deleteSessionUseCase(any())
        }
    }

    @Test
    fun `given new session already set, when canceling login, then do logout and delete session`() = runTest {
        // given
        val (arrangement, viewModel) = Arrangement()
            .withDeleteSessionReturning(DeleteSessionUseCase.Result.Success)
            .withUpdateCurrentSessionReturning(UpdateCurrentSessionUseCase.Result.Success)
            .arrange()
        val newUserId = UserId("newUserId", "domain")
        viewModel.loginJobData.value = LoginJobData(job = mockk(relaxUnitFun = true), newSessionUserId = newUserId)
        // when
        viewModel.cancelLogin()
        advanceUntilIdle()
        // then
        coVerify(exactly = 1) {
            arrangement.logoutUseCase(LogoutReason.SELF_HARD_LOGOUT, true)
            arrangement.deleteSessionUseCase(newUserId)
        }
    }

    @Test
    fun `given no previous session, when canceling login, then update current session to null`() = runTest {
        // given
        val (arrangement, viewModel) = Arrangement()
            .withDeleteSessionReturning(DeleteSessionUseCase.Result.Success)
            .withUpdateCurrentSessionReturning(UpdateCurrentSessionUseCase.Result.Success)
            .arrange()
        val newUserId = UserId("newUserId", "domain")
        viewModel.loginJobData.value = LoginJobData(job = mockk(relaxUnitFun = true), newSessionUserId = newUserId)
        // when
        viewModel.cancelLogin()
        advanceUntilIdle()
        // then
        coVerify(exactly = 1) {
            arrangement.updateCurrentSessionUseCase(null)
        }
    }

    @Test
    fun `given some previous session, when canceling login, then update current session to that session`() = runTest {
        // given
        val (arrangement, viewModel) = Arrangement()
            .withDeleteSessionReturning(DeleteSessionUseCase.Result.Success)
            .withUpdateCurrentSessionReturning(UpdateCurrentSessionUseCase.Result.Success)
            .arrange()
        val loginJob: Job = mockk(relaxUnitFun = true)
        val newUserId = UserId("newUserId", "domain")
        val previousUserId = UserId("previousUserId", "domain")
        viewModel.loginJobData.value = LoginJobData(job = loginJob, previousSessionUserId = previousUserId, newSessionUserId = newUserId)
        // when
        viewModel.cancelLogin()
        advanceUntilIdle()
        // then
        coVerify(exactly = 1) {
            arrangement.updateCurrentSessionUseCase(previousUserId)
        }
    }

    @Test
    fun `given login job is ongoing, when new login job is started, then cancel previous login job and start a new one`() = runTest {
        // given
        val loginJob1: Job = mockk(relaxUnitFun = true) // mocked first login job
        val previousUserId = UserId("previousUserId", "domain") // current session user id
        val newUserId1 = UserId("newUserId1", "domain") // new session user id for the first login job
        val newUserId2 = UserId("newUserId2", "domain") // new session user id for the second login job
        val authToken2 = AUTH_TOKEN.copy(userId = newUserId2) // auth token for the second login job
        val (arrangement, viewModel) = Arrangement()
            .withDeleteSessionReturning(DeleteSessionUseCase.Result.Success)
            .withUpdateCurrentSessionReturning(UpdateCurrentSessionUseCase.Result.Success)
            .withCurrentSessionReturning(CurrentSessionResult.Success(AccountInfo.Valid(previousUserId)))
            .withLoginReturning(AuthenticationResult.Success(authToken2, SSO_ID, MANAGED_BY, SERVER_CONFIG.id, null))
            .withAddAuthenticatedUserReturning(AddAuthenticatedUserUseCase.Result.Success(newUserId2))
            .withValidateEmailReturning(true)
            .withPersistEmailReturning(PersistSelfUserEmailResult.Success)
            .withGetOrRegisterClientReturning(RegisterClientResult.Success(CLIENT))
            .withInitialSyncCompletedReturning(true)
            .arrange()
        // first login job ongoing
        viewModel.loginJobData.value = LoginJobData(job = loginJob1, previousSessionUserId = previousUserId, newSessionUserId = newUserId1)
        // when
        viewModel.login() // start login job again
        advanceUntilIdle()
        // then
        coVerify(exactly = 1) { // verify that the first login job has been canceled and reverted
            loginJob1.cancel()
            arrangement.logoutUseCase(LogoutReason.SELF_HARD_LOGOUT, true)
            arrangement.deleteSessionUseCase(newUserId1)
            arrangement.updateCurrentSessionUseCase(previousUserId)
        }
        coVerify(exactly = 1) { // verify that the second login job has been started
            arrangement.loginUseCase(any(), any(), any(), any(), any())
            arrangement.addAuthenticatedUserUseCase(any(), any(), eq(authToken2), any(), any())
        }
    }

    @Test
    fun `given too many clients failure, when registering client, then do not revert new session`() = runTest {
        // given
        val previousUserId = UserId("previousUserId", "domain")
        val newUserId = UserId("newUserId", "domain")
        val authToken = AUTH_TOKEN.copy(userId = newUserId)
        val (arrangement, viewModel) = Arrangement()
            .withDeleteSessionReturning(DeleteSessionUseCase.Result.Success)
            .withUpdateCurrentSessionReturning(UpdateCurrentSessionUseCase.Result.Success)
            .withCurrentSessionReturning(CurrentSessionResult.Success(AccountInfo.Valid(previousUserId)))
            .withLoginReturning(AuthenticationResult.Success(authToken, SSO_ID, MANAGED_BY, SERVER_CONFIG.id, null))
            .withAddAuthenticatedUserReturning(AddAuthenticatedUserUseCase.Result.Success(newUserId))
            .withValidateEmailReturning(true)
            .withPersistEmailReturning(PersistSelfUserEmailResult.Success)
            .withGetOrRegisterClientReturning(RegisterClientResult.Failure.TooManyClients)
            .arrange()
        // when
        viewModel.login()
        advanceUntilIdle()
        // then
        coVerify(exactly = 0) {
            arrangement.logoutUseCase(LogoutReason.SELF_HARD_LOGOUT, true)
            arrangement.deleteSessionUseCase(newUserId)
            arrangement.updateCurrentSessionUseCase(previousUserId)
        }
    }

    @Test
    fun `given other failure than too many clients, when registering client, then revert new session`() = runTest {
        // given
        val previousUserId = UserId("previousUserId", "domain")
        val newUserId = UserId("newUserId", "domain")
        val authToken = AUTH_TOKEN.copy(userId = newUserId)
        val (arrangement, viewModel) = Arrangement()
            .withDeleteSessionReturning(DeleteSessionUseCase.Result.Success)
            .withUpdateCurrentSessionReturning(UpdateCurrentSessionUseCase.Result.Success)
            .withCurrentSessionReturning(CurrentSessionResult.Success(AccountInfo.Valid(previousUserId)))
            .withLoginReturning(AuthenticationResult.Success(authToken, SSO_ID, MANAGED_BY, SERVER_CONFIG.id, null))
            .withAddAuthenticatedUserReturning(AddAuthenticatedUserUseCase.Result.Success(newUserId))
            .withValidateEmailReturning(true)
            .withPersistEmailReturning(PersistSelfUserEmailResult.Success)
            .withGetOrRegisterClientReturning(RegisterClientResult.Failure.Generic(CoreFailure.Unknown(null)))
            .arrange()
        // when
        viewModel.login()
        advanceUntilIdle()
        // then
        coVerify(exactly = 1) {
            arrangement.logoutUseCase(LogoutReason.SELF_HARD_LOGOUT, true)
            arrangement.deleteSessionUseCase(newUserId)
            arrangement.updateCurrentSessionUseCase(previousUserId)
        }
    }

    @Test
    fun `given username entered and username is not allowed, when logging in, then return invalid input value`() =
        runTest {
            val (arrangement, loginViewModel) = Arrangement()
                .withCurrentSessionReturning(CurrentSessionResult.Failure.SessionNotFound)
                .withValidateEmailReturning(false)
                .arrange()
            loginViewModel.passwordTextState.setTextAndPlaceCursorAtEnd("password")
            loginViewModel.userIdentifierTextState.setTextAndPlaceCursorAtEnd("some.username")

            loginViewModel.login(usernameAllowed = false)
            advanceUntilIdle()

            loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Error.TextFieldError.InvalidValue>()
        }

    @Test
    fun `given email entered and username is not allowed, when logging in, then proceed with login`() =
        runTest {
            val (arrangement, loginViewModel) = Arrangement()
                .withCurrentSessionReturning(CurrentSessionResult.Failure.SessionNotFound)
                .withValidateEmailReturning(true)
                .withLoginReturning(AuthenticationResult.Success(AUTH_TOKEN, SSO_ID, MANAGED_BY, SERVER_CONFIG.id, null))
                .withAddAuthenticatedUserReturning(AddAuthenticatedUserUseCase.Result.Success(USER_ID))
                .withPersistEmailReturning(PersistSelfUserEmailResult.Success)
                .withGetOrRegisterClientReturning(RegisterClientResult.Success(CLIENT))
                .withInitialSyncCompletedReturning(true)
                .arrange()
            loginViewModel.passwordTextState.setTextAndPlaceCursorAtEnd("password")
            loginViewModel.userIdentifierTextState.setTextAndPlaceCursorAtEnd("some.email@example.org")

            loginViewModel.login(usernameAllowed = false)
            advanceUntilIdle()

            loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Success>()
        }

    inner class Arrangement {

        @MockK
        internal lateinit var loginUseCase: LoginUseCase

        @MockK
        internal lateinit var addAuthenticatedUserUseCase: AddAuthenticatedUserUseCase

        @MockK
        internal lateinit var clientScopeProviderFactory: ClientScopeProvider.Factory

        @MockK
        internal lateinit var userScope: UserScope

        @MockK
        internal lateinit var clientScope: ClientScope

        @MockK
        internal lateinit var validateEmailUseCase: ValidateEmailUseCase

        @MockK
        internal lateinit var persistSelfUserEmailUseCase: PersistSelfUserEmailUseCase

        @MockK
        internal lateinit var getOrRegisterClientUseCase: GetOrRegisterClientUseCase

        @MockK
        internal lateinit var savedStateHandle: SavedStateHandle

        @MockK
        internal lateinit var qualifiedIdMapper: QualifiedIdMapper

        @MockK
        internal lateinit var autoVersionAuthScopeUseCase: AutoVersionAuthScopeUseCase

        @MockK
        internal lateinit var coreLogic: CoreLogic

        @MockK
        internal lateinit var requestSecondFactorCodeUseCase: RequestSecondFactorVerificationCodeUseCase

        @MockK
        internal lateinit var userDataStoreProvider: UserDataStoreProvider

        @MockK
        internal lateinit var authenticationScope: AuthenticationScope

        @MockK
        internal lateinit var currentSessionUseCase: CurrentSessionUseCase

        @MockK
        internal lateinit var logoutUseCase: LogoutUseCase

        @MockK
        internal lateinit var deleteSessionUseCase: DeleteSessionUseCase

        @MockK
        internal lateinit var updateCurrentSessionUseCase: UpdateCurrentSessionUseCase

        @MockK
        internal lateinit var countdownTimer: CountdownTimer

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            mockUri()
            every { savedStateHandle.get<String>(any()) } returns null
            every { qualifiedIdMapper.fromStringToQualifiedID(any()) } returns USER_ID
            every { savedStateHandle.set(any(), any<String>()) } returns Unit
            every { coreLogic.getGlobalScope().validateEmailUseCase } returns validateEmailUseCase
            every { coreLogic.getSessionScope(any()).users } returns userScope
            every { userScope.persistSelfUserEmail } returns persistSelfUserEmailUseCase
            every { clientScopeProviderFactory.create(any()).clientScope } returns clientScope
            every { clientScope.getOrRegister } returns getOrRegisterClientUseCase
            every { savedStateHandle.navArgs<LoginNavArgs>() } returns LoginNavArgs(
                loginPasswordPath = LoginPasswordPath(newServerConfig(1).links)
            )
            coEvery { autoVersionAuthScopeUseCase(any()) } returns AutoVersionAuthScopeUseCase.Result.Success(authenticationScope)
            every { authenticationScope.login } returns loginUseCase
            every { authenticationScope.requestSecondFactorVerificationCode } returns requestSecondFactorCodeUseCase
            every { coreLogic.versionedAuthenticationScope(any()) } returns autoVersionAuthScopeUseCase
            every { coreLogic.getSessionScope(any()).logout } returns logoutUseCase
            every { coreLogic.getGlobalScope().deleteSession } returns deleteSessionUseCase
            every { coreLogic.getGlobalScope().session.updateCurrentSession } returns updateCurrentSessionUseCase
            every { coreLogic.getGlobalScope().session.currentSession } returns currentSessionUseCase
            coEvery { currentSessionUseCase() } returns CurrentSessionResult.Success(AccountInfo.Valid(USER_ID))
            coEvery { countdownTimer.start(any(), any(), any()) } returns Unit
        }

        fun arrange() = this to LoginEmailViewModel(
            addAuthenticatedUserUseCase,
            clientScopeProviderFactory,
            savedStateHandle,
            userDataStoreProvider,
            coreLogic,
            countdownTimer,
            dispatcherProvider,
            ServerConfig.STAGING
        ).also { it.autoLoginWhenFullCodeEntered = true }

        fun withLoginReturning(result: AuthenticationResult) = apply {
            coEvery {
                loginUseCase(any(), any(), any(), any(), any())
            } returns result
        }

        fun withAddAuthenticatedUserReturning(result: AddAuthenticatedUserUseCase.Result) = apply {
            coEvery {
                addAuthenticatedUserUseCase(any(), any(), any(), any(), any())
            } returns result
        }

        fun withValidateEmailReturning(result: Boolean) = apply {
            coEvery {
                validateEmailUseCase(any())
            } returns result
        }

        fun withPersistEmailReturning(result: PersistSelfUserEmailResult) = apply {
            coEvery {
                persistSelfUserEmailUseCase(any())
            } returns result
        }

        fun withGetOrRegisterClientReturning(result: RegisterClientResult) = apply {
            coEvery {
                getOrRegisterClientUseCase(any())
            } returns result
        }

        fun withRequestSecondFactorVerificationCodeReturning(result: RequestSecondFactorVerificationCodeUseCase.Result) = apply {
            coEvery {
                requestSecondFactorCodeUseCase(any(), any())
            } returns result
        }

        fun withInitialSyncCompletedReturning(result: Boolean) = apply {
            every {
                userDataStoreProvider.getOrCreate(any()).initialSyncCompleted
            } returns flowOf(result)
        }

        fun withCurrentSessionReturning(result: CurrentSessionResult) = apply {
            coEvery {
                currentSessionUseCase()
            } returns result
        }

        fun withDeleteSessionReturning(result: DeleteSessionUseCase.Result) = apply {
            coEvery {
                deleteSessionUseCase(any())
            } returns result
        }

        fun withUpdateCurrentSessionReturning(result: UpdateCurrentSessionUseCase.Result) = apply {
            coEvery {
                updateCurrentSessionUseCase(any())
            } returns result
        }
    }

    companion object {
        val CLIENT = TestClient.CLIENT
        val USER_ID: QualifiedID = QualifiedID("userId", "domain")
        val SSO_ID: SsoId = SsoId("scim_id", null, null)
        val MANAGED_BY: SsoManagedBy = SsoManagedBy.WIRE
        val AUTH_TOKEN = AccountTokens(
            userId = UserId("user_id", "domain"),
            accessToken = "access_token",
            refreshToken = "refresh_token",
            tokenType = "token_type",
            cookieLabel = "cookie_label"
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
