/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.authentication.login.email

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.ClientScopeProvider
import com.wire.android.framework.TestUser
import com.wire.android.ui.authentication.login.DomainClaimedByOrg
import com.wire.android.ui.authentication.login.LoginNavArgs
import com.wire.android.ui.authentication.login.LoginPasswordPath
import com.wire.android.ui.authentication.login.LoginViewModelExtension
import com.wire.android.ui.authentication.login.PreFilledUserIdentifierType
import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.auth.login.ProxyCredentials
import com.wire.kalium.logic.data.auth.verification.VerifiableAction
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.AuthenticationResult
import com.wire.kalium.logic.feature.auth.AuthenticationScope
import com.wire.kalium.logic.feature.auth.LoginUseCase
import com.wire.kalium.logic.feature.auth.PersistSelfUserEmailResult
import com.wire.kalium.logic.feature.auth.ValidateEmailUseCase
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.auth.verification.RequestSecondFactorVerificationCodeUseCase
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.server.GetServerConfigResult
import com.wire.kalium.logic.feature.server.GetServerConfigUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginEmailViewModelHostFactoryTest {

    @Test
    fun `scope maps versions and reads current proxy credentials on IO`() = runTest {
        val arrangement = Arrangement()
        every { arrangement.coreLogic.versionedAuthenticationScope(ServerConfig.PRODUCTION) } returns arrangement.autoVersion
        val captured = slot<ProxyCredentials?>()
        val failure = NetworkFailure.NoNetworkConnection(null)
        listOf(
            AutoVersionAuthScopeUseCase.Result.Failure.UnknownServerVersion to LoginEmailScopeResult.UnknownServerVersion,
            AutoVersionAuthScopeUseCase.Result.Failure.TooNewVersion to LoginEmailScopeResult.ClientUpdateRequired,
            AutoVersionAuthScopeUseCase.Result.Failure.Generic(failure) to LoginEmailScopeResult.Failure(failure),
        ).forEach { (kalium, feature) ->
            coEvery { arrangement.autoVersion(captureNullable(captured)) } returns kalium
            assertEquals(
                feature,
                arrangement.gateway.resolveScope(ServerConfig.PRODUCTION) {
                LoginEmailProxyCredentials("proxy-user", "proxy-password")
            }
            )
            assertEquals("proxy-user", captured.captured?.username)
            assertEquals("proxy-password", captured.captured?.password)
        }

        coEvery { arrangement.autoVersion(any()) } returns AutoVersionAuthScopeUseCase.Result.Success(arrangement.scope)
        assertEquals(
            LoginEmailScopeResult.Success(arrangement.scope),
            arrangement.gateway.resolveScope(ServerConfig.PRODUCTION) { null },
        )
    }

    @Test
    fun `authentication reads credential providers and maps exact results`() = runTest {
        val arrangement = Arrangement(defaultWebSocket = true)
        var identifier = "first@example.com"
        var password = "first"
        identifier = "latest@example.com"
        password = "latest"
        coEvery { arrangement.login(any(), any(), true, any(), "123456") } returns
            AuthenticationResult.Failure.InvalidCredentials.InvalidPasswordIdentityCombination
        assertEquals(
            LoginEmailAuthenticationResult.InvalidCredentials,
            arrangement.gateway.authenticate(arrangement.scope, { identifier }, { password }, "123456"),
        )
        coVerify { arrangement.login("latest@example.com", "latest", true, any(), "123456") }

        listOf(
            AuthenticationResult.Failure.SocketError to LoginEmailAuthenticationResult.ProxyError,
            AuthenticationResult.Failure.InvalidCredentials.Missing2FA to LoginEmailAuthenticationResult.MissingSecondFactor,
            AuthenticationResult.Failure.InvalidCredentials.Invalid2FA to LoginEmailAuthenticationResult.InvalidSecondFactor,
            AuthenticationResult.Failure.InvalidUserIdentifier to LoginEmailAuthenticationResult.InvalidIdentifier,
            AuthenticationResult.Failure.AccountSuspended to LoginEmailAuthenticationResult.AccountSuspended,
            AuthenticationResult.Failure.AccountPendingActivation to LoginEmailAuthenticationResult.AccountPendingActivation,
        ).forEach { (kalium, feature) ->
            coEvery { arrangement.login(any(), any(), any(), any(), any()) } returns kalium
            assertEquals(feature, arrangement.gateway.authenticate(arrangement.scope, { "email" }, { "password" }, ""))
        }
    }

    @Test
    fun `session client and verification adapters preserve result and failure identity`() = runTest {
        val arrangement = Arrangement()
        val failure = NetworkFailure.NoNetworkConnection(null)
        val session = mockk<com.wire.kalium.logic.data.session.StoreSessionParam>()
        coEvery { arrangement.addAuthenticatedUser(session, replace = false) } returns
            AddAuthenticatedUserUseCase.Result.Success(TestUser.SELF_USER_ID)
        assertEquals(LoginEmailStoreResult.Success(TestUser.SELF_USER_ID), arrangement.gateway.storeSession(session))
        coEvery { arrangement.addAuthenticatedUser(session, replace = false) } returns
            AddAuthenticatedUserUseCase.Result.Failure.Generic(failure)
        assertSame(failure, (arrangement.gateway.storeSession(session) as LoginEmailStoreResult.Failure).failure)
        listOf(
            AddAuthenticatedUserUseCase.Result.Failure.UserAlreadyExists,
            AddAuthenticatedUserUseCase.Result.Failure.SsoIdentityChanged,
            AddAuthenticatedUserUseCase.Result.Failure.NomadSingleUserViolation,
        ).forEach {
            coEvery { arrangement.addAuthenticatedUser(session, replace = false) } returns it
            assertEquals(LoginEmailStoreResult.UserAlreadyExists, arrangement.gateway.storeSession(session))
        }

        coEvery { arrangement.loginExtension.registerClient(TestUser.SELF_USER_ID, "latest") } returns
            RegisterClientResult.Failure.TooManyClients
        var password = "first"
        password = "latest"
        assertEquals(
            LoginEmailClientResult.TooManyDevices,
            arrangement.gateway.registerClient(TestUser.SELF_USER_ID) { password },
        )
        coEvery {
            arrangement.loginExtension.registerClient(any(), any())
        } returns RegisterClientResult.Failure.Generic(failure)
        assertSame(
            failure,
            (arrangement.gateway.registerClient(TestUser.SELF_USER_ID) { "password" } as LoginEmailClientResult.Failure).failure,
        )

        coEvery {
            arrangement.scope.requestSecondFactorVerificationCode("email@example.com", VerifiableAction.LOGIN_OR_CLIENT_REGISTRATION)
        } returns RequestSecondFactorVerificationCodeUseCase.Result.Failure.TooManyRequests
        assertEquals(
            LoginEmailVerificationResult.TooManyRequests,
            arrangement.gateway.requestSecondFactorCode(arrangement.scope, "email@example.com"),
        )
        coEvery { arrangement.scope.requestSecondFactorVerificationCode(any(), any()) } returns
            RequestSecondFactorVerificationCodeUseCase.Result.Failure.Generic(failure)
        assertSame(
            failure,
            (arrangement.gateway.requestSecondFactorCode(arrangement.scope, "email") as LoginEmailVerificationResult.Failure).failure,
        )
    }

    @Test
    fun `email persistence validates and reads the latest identifier on IO`() = runTest {
        val arrangement = Arrangement()
        val validateEmail = mockk<ValidateEmailUseCase>()
        val sessionScope = arrangement.coreLogic.getSessionScope(TestUser.SELF_USER_ID)
        val users = sessionScope.users
        val events = mutableListOf<String>()
        every { arrangement.coreLogic.getGlobalScope().validateEmailUseCase } returns validateEmail
        coEvery { validateEmail("latest@example.com") } answers {
            events += "validate"
            true
        }
        coEvery { users.persistSelfUserEmail("latest@example.com") } answers {
            events += "persist"
            PersistSelfUserEmailResult.Success
        }
        var identifier = "first@example.com"
        identifier = "latest@example.com"

        assertEquals(
            LoginEmailPersistResult.Success,
            arrangement.gateway.persistEmailIfNeeded(TestUser.SELF_USER_ID) { identifier },
        )
        assertEquals(listOf("validate", "persist"), events)

        events.clear()
        coEvery { validateEmail("handle") } answers {
            events += "validate-handle"
            false
        }
        assertEquals(
            LoginEmailPersistResult.Success,
            arrangement.gateway.persistEmailIfNeeded(TestUser.SELF_USER_ID) { "handle" },
        )
        assertEquals(listOf("validate-handle"), events)
    }

    @Test
    fun `rollback performs hard logout delete and previous-session restore in order`() = runTest {
        val arrangement = Arrangement()
        val sessionScope = arrangement.coreLogic.getSessionScope(TestUser.SELF_USER_ID)
        coEvery { sessionScope.logout(LogoutReason.SELF_HARD_LOGOUT, waitUntilCompletes = true) } returns mockk()
        coEvery { arrangement.coreLogic.getGlobalScope().deleteSession(TestUser.SELF_USER_ID) } returns mockk()
        coEvery { arrangement.coreLogic.getGlobalScope().session.updateCurrentSession(TestUser.USER_ID) } returns mockk()

        arrangement.gateway.revertSession(TestUser.SELF_USER_ID, TestUser.USER_ID)

        coVerifyOrder {
            sessionScope.logout(LogoutReason.SELF_HARD_LOGOUT, waitUntilCompletes = true)
            arrangement.coreLogic.getGlobalScope().deleteSession(TestUser.SELF_USER_ID)
            arrangement.coreLogic.getGlobalScope().session.updateCurrentSession(TestUser.USER_ID)
        }
    }

    @Test
    fun `backend adapter rejects invalid input and maps null and successful configuration`() = runTest {
        val useCase = mockk<GetServerConfigUseCase>()
        val arrangement = Arrangement(getServerConfigUseCase = useCase)
        assertEquals(null, arrangement.gateway.parseBackendConfig("   "))
        coEvery { useCase("https://backend.example") } returns GetServerConfigResult.Success(ServerConfig.PRODUCTION)
        assertEquals(
            LoginEmailBackendResult.Success(ServerConfig.PRODUCTION),
            arrangement.gateway.configureBackend("https://backend.example"),
        )

        val unavailable = Arrangement(getServerConfigUseCase = null)
        assertEquals(LoginEmailBackendResult.Failure, unavailable.gateway.configureBackend("https://backend.example"))
    }

    @Test
    fun `host maps custom default prefill domain and creates isolated view models`() = runTest {
        val main = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(main)
        try {
            val custom = ServerConfig.PRODUCTION
            val domain = DomainClaimedByOrg.Claimed("wire.com")
            val arrangement = Arrangement()
            val factory = LoginEmailViewModelHostFactory(
                arrangement.addAuthenticatedUser,
                mockk<ClientScopeProvider.Factory>(relaxed = true),
                mockk<UserDataStoreProvider>(relaxed = true),
                arrangement.coreLogic,
                arrangement.dispatchers,
                ServerConfig.STAGING,
                true,
                false,
                lazyOf(mockk()),
                lazyOf(mockk()),
            )
            val first = factory.create(
                LoginNavArgs(
                    userHandle = PreFilledUserIdentifierType.PreFilled("alice@example.com", editable = false),
                    loginPasswordPath = LoginPasswordPath(customServerConfig = custom, isDomainClaimedByOrg = domain),
                ),
                SavedStateHandle(),
            )
            val second = factory.create(LoginNavArgs(), SavedStateHandle())

            assertEquals(custom, first.serverConfig)
            assertTrue(first.isBackendConfigured)
            assertEquals(domain, first.domainClaimedByOrg)
            assertEquals("alice@example.com", first.userIdentifierTextState.text.toString())
            assertFalse(first.loginState.userIdentifierEnabled)
            assertEquals(ServerConfig.STAGING, second.serverConfig)
            assertFalse(second.isBackendConfigured)
            assertEquals(null, second.domainClaimedByOrg)
            assertNotSame(first, second)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class Arrangement(
        defaultWebSocket: Boolean = false,
        getServerConfigUseCase: GetServerConfigUseCase? = null,
    ) {
        val addAuthenticatedUser = mockk<AddAuthenticatedUserUseCase>()
        val coreLogic = mockk<CoreLogic>(relaxed = true)
        val loginExtension = mockk<LoginViewModelExtension>()
        val autoVersion = mockk<AutoVersionAuthScopeUseCase>()
        val scope = mockk<AuthenticationScope>()
        val login = mockk<LoginUseCase>()
        val dispatchers = TestDispatcherProvider()
        val gateway = KaliumLoginEmailGateway(
            addAuthenticatedUser,
            coreLogic,
            loginExtension,
            dispatchers,
            defaultWebSocket,
            getServerConfigUseCase?.let { lazyOf(it) },
            null,
        )

        init {
            every { scope.login } returns login
            every { coreLogic.getSessionScope(any()) } returns mockk(relaxed = true)
            every { coreLogic.getGlobalScope() } returns mockk(relaxed = true)
        }
    }
}
