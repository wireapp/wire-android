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
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.SnapshotExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.config.mockUri
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.AuthServerConfigProvider
import com.wire.android.di.ClientScopeProvider
import com.wire.android.framework.TestClient
import com.wire.android.ui.authentication.login.LoginState
import com.wire.android.util.EMPTY
import com.wire.android.util.newServerConfig
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.logic.configuration.server.CommonApiVersionType
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.auth.AccountTokens
import com.wire.kalium.logic.data.auth.verification.VerifiableAction
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.user.SsoId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.AuthenticationResult
import com.wire.kalium.logic.feature.auth.AuthenticationScope
import com.wire.kalium.logic.feature.auth.LoginUseCase
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.auth.verification.RequestSecondFactorVerificationCodeUseCase
import com.wire.kalium.logic.feature.client.ClientScope
import com.wire.kalium.logic.feature.client.GetOrRegisterClientUseCase
import com.wire.kalium.logic.feature.client.RegisterClientResult
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class, SnapshotExtension::class)
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
    private lateinit var getOrRegisterClientUseCase: GetOrRegisterClientUseCase

    @MockK
    private lateinit var savedStateHandle: SavedStateHandle

    @MockK
    private lateinit var qualifiedIdMapper: QualifiedIdMapper

    @MockK
    private lateinit var autoVersionAuthScopeUseCase: AutoVersionAuthScopeUseCase

    @MockK
    private lateinit var coreLogic: CoreLogic

    @MockK
    private lateinit var requestSecondFactorCodeUseCase: RequestSecondFactorVerificationCodeUseCase

    @MockK
    private lateinit var authServerConfigProvider: AuthServerConfigProvider

    @MockK
    private lateinit var userDataStoreProvider: UserDataStoreProvider

    @MockK
    private lateinit var authenticationScope: AuthenticationScope

    private lateinit var loginViewModel: LoginEmailViewModel

    private val userId: QualifiedID = QualifiedID("userId", "domain")

    private val dispatcherProvider = TestDispatcherProvider(StandardTestDispatcher())

    private fun runTest(test: suspend TestScope.() -> Unit) = runTest(dispatcherProvider.main(), testBody = test)

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        mockUri()
        every { savedStateHandle.get<String>(any()) } returns null
        every { qualifiedIdMapper.fromStringToQualifiedID(any()) } returns userId
        every { savedStateHandle.set(any(), any<String>()) } returns Unit
        every { clientScopeProviderFactory.create(any()).clientScope } returns clientScope
        every { clientScope.getOrRegister } returns getOrRegisterClientUseCase
        every { authServerConfigProvider.authServer } returns MutableStateFlow((newServerConfig(1).links))
        coEvery {
            autoVersionAuthScopeUseCase(any())
        } returns AutoVersionAuthScopeUseCase.Result.Success(
            authenticationScope
        )

        every { authenticationScope.login } returns loginUseCase
        every { authenticationScope.requestSecondFactorVerificationCode } returns requestSecondFactorCodeUseCase
        every { coreLogic.versionedAuthenticationScope(any()) } returns autoVersionAuthScopeUseCase
        loginViewModel = LoginEmailViewModel(
            addAuthenticatedUserUseCase,
            clientScopeProviderFactory,
            savedStateHandle,
            authServerConfigProvider,
            userDataStoreProvider,
            coreLogic,
            dispatcherProvider
        )
    }

    @Test
    fun `given empty strings, when entering credentials, then button is disabled`() {
        loginViewModel.passwordTextState.setTextAndPlaceCursorAtEnd(String.EMPTY)
        loginViewModel.userIdentifierTextState.setTextAndPlaceCursorAtEnd(String.EMPTY)
        loginViewModel.loginState.loginEnabled shouldBeEqualTo false
        loginViewModel.loginState.flowState.shouldNotBeInstanceOf<LoginState.Loading>()
    }

    @Test
    fun `given non-empty strings, when entering credentials, then button is enabled`() {
        loginViewModel.passwordTextState.setTextAndPlaceCursorAtEnd("abc")
        loginViewModel.userIdentifierTextState.setTextAndPlaceCursorAtEnd("abc")
        loginViewModel.loginState.loginEnabled shouldBeEqualTo true
        loginViewModel.loginState.flowState.shouldNotBeInstanceOf<LoginState.Loading>()
    }

    @Test
    fun `given button is clicked, when logging in, then show loading`() = runTest {
        coEvery {
            loginUseCase(any(), any(), any(), any(), any())
        } returns AuthenticationResult.Failure.InvalidCredentials.InvalidPasswordIdentityCombination
        coEvery {
            addAuthenticatedUserUseCase(any(), any(), any(), any())
        } returns AddAuthenticatedUserUseCase.Result.Success(userId)

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
        val password = "abc"
        coEvery { loginUseCase(any(), any(), any(), any(), any()) } returns AuthenticationResult.Success(
            AUTH_TOKEN,
            SSO_ID,
            SERVER_CONFIG.id,
            null
        )
        coEvery { addAuthenticatedUserUseCase(any(), any(), any(), any()) } returns AddAuthenticatedUserUseCase.Result.Success(userId)
        coEvery { getOrRegisterClientUseCase(any()) } returns RegisterClientResult.Success(CLIENT)
        every { userDataStoreProvider.getOrCreate(any()).initialSyncCompleted } returns flowOf(true)

        loginViewModel.passwordTextState.setTextAndPlaceCursorAtEnd(password)

        loginViewModel.login()
        advanceUntilIdle()
        coVerify(exactly = 1) { loginUseCase(any(), any(), any(), any(), any()) }
        coVerify(exactly = 1) { getOrRegisterClientUseCase(any()) }
        loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Success>().let {
            it.initialSyncCompleted shouldBe true
            it.isE2EIRequired shouldBe false
        }
    }

    @Test
    fun `given button is clicked and initial sync is not completed, when login returns Success, then navigate to initial sync screen`() =
        runTest {
            val password = "abc"
            coEvery { loginUseCase(any(), any(), any(), any(), any()) } returns AuthenticationResult.Success(
                AUTH_TOKEN,
                SSO_ID,
                SERVER_CONFIG.id,
                null
            )
            coEvery { addAuthenticatedUserUseCase(any(), any(), any(), any()) } returns AddAuthenticatedUserUseCase.Result.Success(userId)
            coEvery { getOrRegisterClientUseCase(any()) } returns RegisterClientResult.Success(CLIENT)
            every { userDataStoreProvider.getOrCreate(any()).initialSyncCompleted } returns flowOf(false)

            loginViewModel.passwordTextState.setTextAndPlaceCursorAtEnd(password)

            loginViewModel.login()
            advanceUntilIdle()
            coVerify(exactly = 1) { loginUseCase(any(), any(), any(), any(), any()) }
            coVerify(exactly = 1) { getOrRegisterClientUseCase(any()) }
            loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Success>().let {
                it.initialSyncCompleted shouldBe false
                it.isE2EIRequired shouldBe false
            }
        }

    @Test
    fun `given button is clicked, when login returns InvalidUserIdentifier error, then InvalidUserIdentifierError is passed`() = runTest {
        coEvery {
            loginUseCase(any(), any(), any(), any(), any())
        } returns AuthenticationResult.Failure.InvalidUserIdentifier

        loginViewModel.login()
        advanceUntilIdle()
        loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Error.TextFieldError.InvalidValue>()
    }

    @Test
    fun `given button is clicked, when login returns InvalidCredentials error, then InvalidCredentialsError is passed`() = runTest {
        coEvery {
            loginUseCase(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns AuthenticationResult.Failure.InvalidCredentials.InvalidPasswordIdentityCombination

        loginViewModel.login()
        advanceUntilIdle()
        loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Error.DialogError.InvalidCredentialsError>()
    }

    @Test
    fun `given button is clicked, when login returns Generic error, then GenericError is passed`() = runTest {
        val networkFailure = NetworkFailure.NoNetworkConnection(null)
        coEvery {
            loginUseCase(any(), any(), any(), any(), any())
        } returns AuthenticationResult.Failure.Generic(networkFailure)

        loginViewModel.login()
        advanceUntilIdle()

        loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Error.DialogError.GenericError>().let {
            it.coreFailure shouldBe networkFailure
        }
    }

    @Test
    fun `given dialog is dismissed, when login returns DialogError, then hide error`() = runTest {
        coEvery {
            loginUseCase(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns AuthenticationResult.Failure.InvalidCredentials.InvalidPasswordIdentityCombination

        loginViewModel.login()
        advanceUntilIdle()
        loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Error.DialogError.InvalidCredentialsError>()
        loginViewModel.clearLoginErrors()
        loginViewModel.loginState.flowState.shouldNotBeInstanceOf<LoginState.Error>()
    }

    @Test
    fun `given button is clicked, when addAuthenticatedUser returns UserAlreadyExists error, then UserAlreadyExists is passed`() = runTest {
        coEvery { loginUseCase(any(), any(), any(), any(), any()) } returns AuthenticationResult.Success(
            AUTH_TOKEN,
            SSO_ID,
            SERVER_CONFIG.id,
            null
        )
        coEvery {
            addAuthenticatedUserUseCase(
                any(),
                any(),
                any(),
                any()
            )
        } returns AddAuthenticatedUserUseCase.Result.Failure.UserAlreadyExists

        loginViewModel.login()
        advanceUntilIdle()

        loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Error.DialogError.UserAlreadyExists>()
    }

    @Test
    fun `given login fails with missing 2fa, when logging in, then should send an email to input`() = runTest {
        val email = "some.email@example.org"
        coEvery { loginUseCase(any(), any(), any(), any(), any()) } returns AuthenticationResult.Failure.InvalidCredentials.Missing2FA
        coEvery { requestSecondFactorCodeUseCase(any(), any()) } returns RequestSecondFactorVerificationCodeUseCase.Result.Success
        loginViewModel.userIdentifierTextState.setTextAndPlaceCursorAtEnd(email)

        loginViewModel.login()
        advanceUntilIdle()

        coVerify(exactly = 1) { requestSecondFactorCodeUseCase(email, VerifiableAction.LOGIN_OR_CLIENT_REGISTRATION) }
    }

    @Test
    fun `given missing 2fa, when logging in, then email should be enabled and not loading`() = runTest {
        val email = "some.email@example.org"
        coEvery { loginUseCase(any(), any(), any(), any(), any()) } returns AuthenticationResult.Failure.InvalidCredentials.Missing2FA
        coEvery { requestSecondFactorCodeUseCase(any(), any()) } returns RequestSecondFactorVerificationCodeUseCase.Result.Success

        loginViewModel.userIdentifierTextState.setTextAndPlaceCursorAtEnd(email)
        loginViewModel.passwordTextState.setTextAndPlaceCursorAtEnd("somePassword")
        loginViewModel.login()
        advanceUntilIdle()

        loginViewModel.loginState.flowState.shouldNotBeInstanceOf<LoginState.Loading>()
        loginViewModel.loginState.loginEnabled shouldBe true
    }

    @Test
    fun `given login fails with 2fa missing and 2fa request succeeds, when logging in, then should request user input`() = runTest {
        val email = "some.email@example.org"
        coEvery { loginUseCase(any(), any(), any(), any(), any()) } returns AuthenticationResult.Failure.InvalidCredentials.Missing2FA
        coEvery { requestSecondFactorCodeUseCase(any(), any()) } returns RequestSecondFactorVerificationCodeUseCase.Result.Success
        loginViewModel.userIdentifierTextState.setTextAndPlaceCursorAtEnd(email)

        loginViewModel.login()
        advanceUntilIdle()

        loginViewModel.secondFactorVerificationCodeState.isCodeInputNecessary shouldBe true
        coVerify(exactly = 1) { requestSecondFactorCodeUseCase(email, VerifiableAction.LOGIN_OR_CLIENT_REGISTRATION) }
    }

    @Test
    fun `given login fails with 2fa missing and 2fa request fails generically, when logging in, then should NOT request user input`() =
        runTest {
            val email = "some.email@example.org"
            coEvery { loginUseCase(any(), any(), any(), any(), any()) } returns AuthenticationResult.Failure.InvalidCredentials.Missing2FA
            coEvery {
                requestSecondFactorCodeUseCase(
                    any(),
                    any()
                )
            } returns RequestSecondFactorVerificationCodeUseCase.Result.Failure.Generic(
                CoreFailure.Unknown(null)
            )
            loginViewModel.userIdentifierTextState.setTextAndPlaceCursorAtEnd(email)

            loginViewModel.login()
            advanceUntilIdle()

            loginViewModel.secondFactorVerificationCodeState.isCodeInputNecessary shouldBe false
            coVerify(exactly = 1) { requestSecondFactorCodeUseCase(email, VerifiableAction.LOGIN_OR_CLIENT_REGISTRATION) }
        }

    @Test
    fun `given 2fa code request fails with too many requests, when logging in, then should request user input`() = runTest {
        val email = "some.email@example.org"
        coEvery { loginUseCase(any(), any(), any(), any(), any()) } returns AuthenticationResult.Failure.InvalidCredentials.Missing2FA
        coEvery {
            requestSecondFactorCodeUseCase(any(), any())
        } returns RequestSecondFactorVerificationCodeUseCase.Result.Failure.TooManyRequests
        loginViewModel.userIdentifierTextState.setTextAndPlaceCursorAtEnd(email)

        loginViewModel.login()
        advanceUntilIdle()

        loginViewModel.secondFactorVerificationCodeState.isCodeInputNecessary shouldBe true
        coVerify(exactly = 1) { requestSecondFactorCodeUseCase(email, VerifiableAction.LOGIN_OR_CLIENT_REGISTRATION) }
    }

    @Test
    fun `given login fails with missing 2fa, when logging in, then should state 2FA input is needed`() = runTest {
        val email = "some.email@example.org"
        coEvery { loginUseCase(any(), any(), any(), any(), any()) } returns AuthenticationResult.Failure.InvalidCredentials.Missing2FA
        coEvery { requestSecondFactorCodeUseCase(any(), any()) } returns RequestSecondFactorVerificationCodeUseCase.Result.Success
        loginViewModel.userIdentifierTextState.setTextAndPlaceCursorAtEnd(email)
        loginViewModel.login()
        advanceUntilIdle()
        loginViewModel.secondFactorVerificationCodeState.isCodeInputNecessary shouldBe true
    }

    @Test
    fun `given login fails with invalid 2fa, when logging in, then should mark the current code as invalid`() = runTest {
        coEvery { loginUseCase(any(), any(), any(), any(), any()) } returns AuthenticationResult.Failure.InvalidCredentials.Invalid2FA
        loginViewModel.userIdentifierTextState.setTextAndPlaceCursorAtEnd("some.email@example.org")

        loginViewModel.login()
        advanceUntilIdle()
        loginViewModel.secondFactorVerificationCodeState.isCurrentCodeInvalid shouldBe true
    }

    @Test
    fun `given 2fa is needed, when code is filled, then should login with entered code and navigate out of login`() = runTest {
        val email = "some.email@example.org"
        val code = "123456"
        coEvery { loginUseCase(any(), any(), any(), any(), any()) } returns AuthenticationResult.Success(
            AUTH_TOKEN,
            SSO_ID,
            SERVER_CONFIG.id,
            null
        )
        coEvery { addAuthenticatedUserUseCase(any(), any(), any(), any()) } returns AddAuthenticatedUserUseCase.Result.Success(userId)
        coEvery { getOrRegisterClientUseCase(any()) } returns RegisterClientResult.Success(CLIENT)
        every { userDataStoreProvider.getOrCreate(any()).initialSyncCompleted } returns flowOf(true)

        loginViewModel.userIdentifierTextState.setTextAndPlaceCursorAtEnd(email)
        loginViewModel.secondFactorVerificationCodeTextState.setTextAndPlaceCursorAtEnd(code)
        advanceUntilIdle()
        coVerify(exactly = 1) { loginUseCase(email, any(), any(), any(), code) }
        coVerify(exactly = 1) { getOrRegisterClientUseCase(any()) }
        loginViewModel.loginState.flowState.shouldBeInstanceOf<LoginState.Success>()
    }

    @Test
    fun `given 2fa login succeeds and registration fails, when code is filled, then should no longer require input`() = runTest {
        val email = "some.email@example.org"
        val code = "123456"

        coEvery { loginUseCase(any(), any(), any(), any(), any()) } returns AuthenticationResult.Success(
            AUTH_TOKEN,
            SSO_ID,
            SERVER_CONFIG.id,
            null
        )
        coEvery { addAuthenticatedUserUseCase(any(), any(), any(), any()) } returns AddAuthenticatedUserUseCase.Result.Success(userId)
        coEvery { getOrRegisterClientUseCase(any()) } returns RegisterClientResult.Failure.TooManyClients
        every { userDataStoreProvider.getOrCreate(any()).initialSyncCompleted } returns flowOf(true)

        loginViewModel.userIdentifierTextState.setTextAndPlaceCursorAtEnd(email)
        loginViewModel.secondFactorVerificationCodeTextState.setTextAndPlaceCursorAtEnd(code)
        advanceUntilIdle()
        loginViewModel.secondFactorVerificationCodeState.isCodeInputNecessary shouldBe false
    }

    @Test
    fun `given 2fa is needed, when code is filled, then should register client without explicit 2fa code`() = runTest {
        val email = "some.email@example.org"
        val code = "123456"
        coEvery { loginUseCase(any(), any(), any(), any(), any()) } returns AuthenticationResult.Success(
            AUTH_TOKEN,
            SSO_ID,
            SERVER_CONFIG.id,
            null
        )
        coEvery { addAuthenticatedUserUseCase(any(), any(), any(), any()) } returns AddAuthenticatedUserUseCase.Result.Success(userId)
        coEvery { getOrRegisterClientUseCase(any()) } returns RegisterClientResult.Success(CLIENT)
        every { userDataStoreProvider.getOrCreate(any()).initialSyncCompleted } returns flowOf(true)

        loginViewModel.userIdentifierTextState.setTextAndPlaceCursorAtEnd(email)
        loginViewModel.secondFactorVerificationCodeTextState.setTextAndPlaceCursorAtEnd(code)
        advanceUntilIdle()
        coVerify(exactly = 1) { getOrRegisterClientUseCase(match { it.secondFactorVerificationCode == null }) }
    }

    @Test
    fun `given 2fa is needed, when user used handle to login, then show correct error message`() = runTest {
        val email = "some.handle"
        val code = "123456"
        coEvery { loginUseCase(any(), any(), any(), any(), any()) } returns AuthenticationResult.Failure.InvalidCredentials.Missing2FA

        loginViewModel.userIdentifierTextState.setTextAndPlaceCursorAtEnd(email)
        loginViewModel.secondFactorVerificationCodeTextState.setTextAndPlaceCursorAtEnd(code)
        advanceUntilIdle()
        coVerify(exactly = 0) { addAuthenticatedUserUseCase(any(), any(), any(), any()) }
        coVerify(exactly = 0) { getOrRegisterClientUseCase(any()) }
        assertEquals(LoginState.Error.DialogError.Request2FAWithHandle, loginViewModel.loginState.flowState)
    }

    companion object {
        val CLIENT = TestClient.CLIENT
        val SSO_ID: SsoId = SsoId("scim_id", null, null)
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
