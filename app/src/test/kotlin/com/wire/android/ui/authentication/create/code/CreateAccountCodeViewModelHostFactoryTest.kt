/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.authentication.create.code

import com.wire.android.di.ClientScopeProvider
import com.wire.android.framework.TestUser
import com.wire.android.ui.authentication.create.common.CreateAccountFlowType
import com.wire.android.ui.authentication.create.common.CreateAccountNavArgs
import com.wire.android.ui.authentication.create.common.UserRegistrationInfo
import com.wire.android.util.WillNeverOccurError
import com.wire.android.util.ui.CountdownTimer
import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.auth.AccountTokens
import com.wire.kalium.logic.data.session.StoreSessionParam
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.AuthenticationScope
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.client.ClientScope
import com.wire.kalium.logic.feature.client.GetOrRegisterClientUseCase
import com.wire.kalium.logic.feature.client.RegisterClientParam
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.register.RegisterAccountUseCase
import com.wire.kalium.logic.feature.register.RegisterParam
import com.wire.kalium.logic.feature.register.RegisterResult
import com.wire.kalium.logic.feature.register.RequestActivationCodeResult
import com.wire.kalium.logic.feature.register.RequestActivationCodeUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateAccountCodeViewModelHostFactoryTest {

    @Test
    fun `all authentication scope failures map to unavailable without invoking operations`() = runTest {
        val arrangement = Arrangement()
        listOf(
            AutoVersionAuthScopeUseCase.Result.Failure.UnknownServerVersion,
            AutoVersionAuthScopeUseCase.Result.Failure.TooNewVersion,
            AutoVersionAuthScopeUseCase.Result.Failure.Generic(NetworkFailure.NoNetworkConnection(null)),
        ).forEach { failure ->
            coEvery { arrangement.autoVersionAuthScope(null) } returns failure
            assertEquals(
                ActivationCodeRequestResult.AuthScopeUnavailable,
                arrangement.gateway.requestActivationCode(ServerConfig.PRODUCTION, "alice@example.com"),
            )
            assertEquals(
                AccountRegistrationResult.AuthScopeUnavailable,
                arrangement.gateway.register(ServerConfig.PRODUCTION, personalRequest()),
            )
        }
        coVerify(exactly = 0) { arrangement.requestActivationCode(any()) }
        coVerify(exactly = 0) { arrangement.register(any()) }
    }

    @Test
    fun `activation request maps every result preserving generic identity`() = runTest {
        val arrangement = Arrangement().withAuthScope()
        val failure = NetworkFailure.NoNetworkConnection(null)
        listOf(
            RequestActivationCodeResult.Success to ActivationCodeRequestResult.Sent,
            RequestActivationCodeResult.Failure.AlreadyInUse to ActivationCodeRequestResult.AlreadyInUse,
            RequestActivationCodeResult.Failure.BlacklistedEmail to ActivationCodeRequestResult.Blacklisted,
            RequestActivationCodeResult.Failure.DomainBlocked to ActivationCodeRequestResult.DomainBlocked,
            RequestActivationCodeResult.Failure.InvalidEmail to ActivationCodeRequestResult.InvalidEmail,
            RequestActivationCodeResult.Failure.Generic(failure) to ActivationCodeRequestResult.Generic(failure),
        ).forEach { (kalium, feature) ->
            coEvery { arrangement.requestActivationCode("alice@example.com") } returns kalium
            val actual = arrangement.gateway.requestActivationCode(ServerConfig.PRODUCTION, "alice@example.com")
            assertEquals(feature, actual)
            if (actual is ActivationCodeRequestResult.Generic) assertSame(failure, actual.failure)
        }
    }

    @Test
    fun `register resolves scope before reading activation code and maps exact parameters`() = runTest {
        val arrangement = Arrangement()
        val scopeRequested = CompletableDeferred<Unit>()
        val releaseScope = CompletableDeferred<Unit>()
        val events = mutableListOf<String>()
        coEvery { arrangement.autoVersionAuthScope(null) } coAnswers {
            events += "scope"
            scopeRequested.complete(Unit)
            releaseScope.await()
            AutoVersionAuthScopeUseCase.Result.Success(arrangement.authenticationScope)
        }
        val parameter = slot<RegisterParam>()
        coEvery { arrangement.register(capture(parameter)) } returns RegisterResult.Failure.InvalidActivationCode
        val request = personalRequest {
            events += "code"
            "654321"
        }

        val result = async { arrangement.gateway.register(ServerConfig.PRODUCTION, request) }
        runCurrent()
        assertEquals(listOf("scope"), events)
        releaseScope.complete(Unit)

        assertEquals(AccountRegistrationResult.InvalidActivationCode, result.await())
        assertEquals(listOf("scope", "code"), events)
        val actual = parameter.captured as RegisterParam.PrivateAccount
        assertEquals("Alice", actual.firstName)
        assertEquals("Wire", actual.lastName)
        assertEquals("secret", actual.password)
        assertEquals("alice@example.com", actual.email)
        assertEquals("654321", actual.emailActivationCode)
    }

    @Test
    fun `register maps team parameter and every structured failure`() = runTest {
        val arrangement = Arrangement().withAuthScope()
        val team = CreateAccountRegistrationRequest.Team(
            "Alice", "Wire", "secret", "alice@example.com", { "123456" }, "Wire Team"
        )
        val parameter = slot<RegisterParam>()
        coEvery { arrangement.register(capture(parameter)) } returns RegisterResult.Failure.InvalidActivationCode
        arrangement.gateway.register(ServerConfig.PRODUCTION, team)
        val actualTeam = parameter.captured as RegisterParam.Team
        assertEquals("Alice", actualTeam.firstName)
        assertEquals("Wire", actualTeam.lastName)
        assertEquals("secret", actualTeam.password)
        assertEquals("alice@example.com", actualTeam.email)
        assertEquals("123456", actualTeam.emailActivationCode)
        assertEquals("Wire Team", actualTeam.teamName)
        assertEquals("default", actualTeam.teamIcon)

        val success = mockk<RegisterResult.Success>()
        coEvery { arrangement.register(any()) } returns success
        val mappedSuccess = arrangement.gateway.register(ServerConfig.PRODUCTION, personalRequest()) as AccountRegistrationResult.Success
        assertSame(success, mappedSuccess.credentials.result)

        val failure = NetworkFailure.NoNetworkConnection(null)
        listOf(
            RegisterResult.Failure.InvalidActivationCode to AccountRegistrationResult.InvalidActivationCode,
            RegisterResult.Failure.AccountAlreadyExists to AccountRegistrationResult.AccountAlreadyExists,
            RegisterResult.Failure.BlackListed to AccountRegistrationResult.Blacklisted,
            RegisterResult.Failure.EmailDomainBlocked to AccountRegistrationResult.DomainBlocked,
            RegisterResult.Failure.InvalidEmail to AccountRegistrationResult.InvalidEmail,
            RegisterResult.Failure.TeamMembersLimitReached to AccountRegistrationResult.TeamMembersLimitReached,
            RegisterResult.Failure.UserCreationRestricted to AccountRegistrationResult.UserCreationRestricted,
            RegisterResult.Failure.Generic(failure) to AccountRegistrationResult.Generic(failure),
        ).forEach { (kalium, feature) ->
            coEvery { arrangement.register(any()) } returns kalium
            val actual = arrangement.gateway.register(ServerConfig.PRODUCTION, personalRequest())
            assertEquals(feature, actual)
            if (actual is AccountRegistrationResult.Generic) assertSame(failure, actual.failure)
        }
    }

    @Test
    fun `store uses exact session parameter replace false and maps all failures`() = runTest {
        val arrangement = Arrangement(defaultWebSocket = true)
        val credentials = credentials()
        val parameter = slot<StoreSessionParam>()
        coEvery { arrangement.addAuthenticatedUser(capture(parameter), replace = false) } returns
            AddAuthenticatedUserUseCase.Result.Success(TestUser.SELF_USER_ID)

        assertEquals(StoreAccountSessionResult.Success(TestUser.SELF_USER_ID), arrangement.gateway.storeSession(credentials))
        assertSame(credentials.result.authData, parameter.captured.accountTokens)
        assertEquals(credentials.result.ssoID, parameter.captured.ssoId)
        assertEquals("server", parameter.captured.serverConfigId)
        assertEquals(true, parameter.captured.isPersistentWebSocketEnabled)

        val failure = NetworkFailure.NoNetworkConnection(null)
        listOf(
            AddAuthenticatedUserUseCase.Result.Failure.UserAlreadyExists,
            AddAuthenticatedUserUseCase.Result.Failure.SsoIdentityChanged,
            AddAuthenticatedUserUseCase.Result.Failure.NomadSingleUserViolation,
        ).forEach { kalium ->
            coEvery { arrangement.addAuthenticatedUser(any(), replace = false) } returns kalium
            assertEquals(StoreAccountSessionResult.UserAlreadyExists, arrangement.gateway.storeSession(credentials))
        }
        coEvery { arrangement.addAuthenticatedUser(any(), replace = false) } returns
            AddAuthenticatedUserUseCase.Result.Failure.Generic(failure)
        val generic = arrangement.gateway.storeSession(credentials) as StoreAccountSessionResult.Generic
        assertSame(failure, generic.failure)
    }

    @Test
    fun `client uses exact password and model postfix maps results and preserves impossible exceptions`() = runTest {
        val arrangement = Arrangement(buildFlags = CreateAccountCodeBuildFlags(true, "internal", "debug"))
        val parameter = slot<RegisterClientParam>()
        coEvery { arrangement.getOrRegister(capture(parameter)) } returns RegisterClientResult.Failure.TooManyClients
        assertEquals(CreateAccountClientResult.TooManyDevices, arrangement.gateway.registerClient(TestUser.SELF_USER_ID, "secret"))
        assertEquals("secret", parameter.captured.password)
        assertEquals(" [internal_debug]", parameter.captured.modelPostfix)
        assertNull(parameter.captured.capabilities)

        coEvery { arrangement.getOrRegister(any()) } returns mockk<RegisterClientResult.Success>()
        assertEquals(CreateAccountClientResult.Success, arrangement.gateway.registerClient(TestUser.SELF_USER_ID, "secret"))
        coEvery { arrangement.getOrRegister(any()) } returns mockk<RegisterClientResult.E2EICertificateRequired>()
        assertEquals(
            CreateAccountClientResult.E2EICertificateRequired,
            arrangement.gateway.registerClient(TestUser.SELF_USER_ID, "secret"),
        )
        val failure = NetworkFailure.NoNetworkConnection(null)
        coEvery { arrangement.getOrRegister(any()) } returns RegisterClientResult.Failure.Generic(failure)
        val generic = arrangement.gateway.registerClient(TestUser.SELF_USER_ID, "secret") as CreateAccountClientResult.Generic
        assertSame(failure, generic.failure)

        listOf(
            RegisterClientResult.Failure.InvalidCredentials.InvalidPassword to
                "RegisterClient: wrong password when register client after creating a new account",
            RegisterClientResult.Failure.PasswordAuthRequired to
                "RegisterClient: password required to register client after creating new account with email",
        ).forEach { (kalium, message) ->
            coEvery { arrangement.getOrRegister(any()) } returns kalium
            val thrown = try {
                arrangement.gateway.registerClient(TestUser.SELF_USER_ID, "secret")
                null
            } catch (error: WillNeverOccurError) {
                error
            }
            assertEquals(message, thrown?.message)
        }

        val public = Arrangement(buildFlags = CreateAccountCodeBuildFlags(false, "prod", "release"))
        val publicParameter = slot<RegisterClientParam>()
        coEvery { public.getOrRegister(capture(publicParameter)) } returns RegisterClientResult.Failure.TooManyClients
        public.gateway.registerClient(TestUser.SELF_USER_ID, "secret")
        assertNull(publicParameter.captured.modelPostfix)
    }

    @Test
    fun `host maps nav input custom default and creates distinct timer per view model`() {
        val arrangement = Arrangement(defaultServerConfig = ServerConfig.STAGING)
        val info = UserRegistrationInfo("alice@example.com", firstName = "Alice", lastName = "Wire", password = "secret")
        val custom = arrangement.hostFactory.create(
            CreateAccountNavArgs(CreateAccountFlowType.CreateTeam, info, ServerConfig.PRODUCTION)
        )
        val fallback = arrangement.hostFactory.create(
            CreateAccountNavArgs(CreateAccountFlowType.CreatePersonalAccount, info)
        )

        assertEquals(CreateAccountFlowType.CreateTeam, custom.flowType)
        assertEquals(ServerConfig.PRODUCTION, custom.customServerConfig)
        assertEquals(ServerConfig.PRODUCTION, custom.serverConfig)
        assertNull(fallback.customServerConfig)
        assertEquals(ServerConfig.STAGING, fallback.serverConfig)
        val inputField = CreateAccountCodeViewModel::class.java.getDeclaredField("input").apply { isAccessible = true }
        val customInput = inputField.get(custom) as CreateAccountCodeInput<*, *>
        assertEquals("alice@example.com", customInput.email)
        assertEquals("Alice", customInput.firstName)
        assertEquals("Wire", customInput.lastName)
        assertEquals("secret", customInput.password)
        assertEquals(true, customInput.isTeam)
        val field = CreateAccountCodeViewModel::class.java.getDeclaredField("resendCodeTimer").apply { isAccessible = true }
        assertNotSame(field.get(custom), field.get(fallback))
    }

    @Test
    fun `android timer adapter delegates seconds and callbacks exactly`() = runTest {
        val countdown = mockk<CountdownTimer>()
        val update: (String) -> Unit = {}
        val finish: () -> Unit = {}
        coEvery { countdown.start(300L, update, finish) } returns Unit

        AndroidCreateAccountCodeResendTimer(countdown).start(300L, update, finish)

        coVerify(exactly = 1) { countdown.start(300L, update, finish) }
    }

    private fun personalRequest(code: () -> String = { "123456" }) = CreateAccountRegistrationRequest.Personal(
        "Alice", "Wire", "secret", "alice@example.com", code
    )

    private fun credentials(): KaliumCreateAccountCredentials {
        val result = mockk<RegisterResult.Success>()
        every { result.authData } returns mockk<AccountTokens>()
        every { result.ssoID } returns null
        every { result.serverConfigId } returns "server"
        every { result.proxyCredentials } returns null
        return KaliumCreateAccountCredentials(result)
    }

    private class Arrangement(
        defaultServerConfig: ServerConfig.Links = ServerConfig.STAGING,
        defaultWebSocket: Boolean = false,
        buildFlags: CreateAccountCodeBuildFlags = CreateAccountCodeBuildFlags(false, "prod", "debug"),
    ) {
        val coreLogic = mockk<CoreLogic>()
        val autoVersionAuthScope = mockk<AutoVersionAuthScopeUseCase>()
        val authenticationScope = mockk<AuthenticationScope>()
        val requestActivationCode = mockk<RequestActivationCodeUseCase>()
        val register = mockk<RegisterAccountUseCase>()
        val addAuthenticatedUser = mockk<AddAuthenticatedUserUseCase>()
        val clientScopeProviderFactory = mockk<ClientScopeProvider.Factory>()
        val clientScopeProvider = mockk<ClientScopeProvider>()
        val clientScope = mockk<ClientScope>()
        val getOrRegister = mockk<GetOrRegisterClientUseCase>()
        val gateway = KaliumCreateAccountCodeGateway(
            coreLogic,
            addAuthenticatedUser,
            clientScopeProviderFactory,
            defaultWebSocket,
            buildFlags,
        )
        val hostFactory = CreateAccountCodeViewModelHostFactory(
            coreLogic,
            addAuthenticatedUser,
            clientScopeProviderFactory,
            defaultServerConfig,
            defaultWebSocket,
        )

        init {
            coEvery { coreLogic.versionedAuthenticationScope(any()) } returns autoVersionAuthScope
            every { authenticationScope.registerScope.requestActivationCode } returns requestActivationCode
            every { authenticationScope.registerScope.register } returns register
            every { clientScopeProviderFactory.create(any()) } returns clientScopeProvider
            every { clientScopeProvider.clientScope } returns clientScope
            every { clientScope.getOrRegister } returns getOrRegister
        }

        fun withAuthScope() = apply {
            coEvery { autoVersionAuthScope(null) } returns AutoVersionAuthScopeUseCase.Result.Success(authenticationScope)
        }
    }
}
