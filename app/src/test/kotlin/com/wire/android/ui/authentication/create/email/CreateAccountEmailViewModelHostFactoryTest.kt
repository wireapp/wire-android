/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.authentication.create.email

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.SnapshotExtension
import com.wire.android.navigation.routes.auth.CreateAccountRouteFlowType
import com.wire.android.navigation.routes.auth.toAuthenticationServerLinks
import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.feature.auth.AuthenticationScope
import com.wire.kalium.logic.feature.auth.ValidateEmailUseCase
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.register.RequestActivationCodeResult
import com.wire.kalium.logic.feature.register.RequestActivationCodeUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class, SnapshotExtension::class)
class CreateAccountEmailViewModelHostFactoryTest {

    @Test
    fun `gateway forwards exact normalized candidate to local validator`() {
        val arrangement = Arrangement()
        every { arrangement.validateEmail("alice@example.com") } returns true
        every { arrangement.validateEmail("bad") } returns false

        assertEquals(true, arrangement.gateway.isEmailValid("alice@example.com"))
        assertEquals(false, arrangement.gateway.isEmailValid("bad"))
        verify(exactly = 1) { arrangement.validateEmail("alice@example.com") }
        verify(exactly = 1) { arrangement.validateEmail("bad") }
    }

    @Test
    fun `all auth scope failures map to unavailable`() = runTest {
        val arrangement = Arrangement()
        listOf(
            AutoVersionAuthScopeUseCase.Result.Failure.UnknownServerVersion,
            AutoVersionAuthScopeUseCase.Result.Failure.TooNewVersion,
            AutoVersionAuthScopeUseCase.Result.Failure.Generic(NetworkFailure.NoNetworkConnection(null)),
        ).forEach { authResult ->
            coEvery { arrangement.autoVersionAuthScope(null) } returns authResult

            assertEquals(
                ActivationCodeResult.AuthScopeUnavailable,
                arrangement.gateway.requestActivationCode(ServerConfig.PRODUCTION) { "alice@example.com" },
            )
        }
        coVerify(exactly = 0) { arrangement.requestActivationCode(any()) }
    }

    @Test
    fun `successful auth scope maps every activation result preserving generic identity`() = runTest {
        val arrangement = Arrangement()
        val failure = NetworkFailure.NoNetworkConnection(null)
        coEvery { arrangement.autoVersionAuthScope(null) } returns
            AutoVersionAuthScopeUseCase.Result.Success(arrangement.authenticationScope)
        listOf(
            RequestActivationCodeResult.Success to ActivationCodeResult.Sent,
            RequestActivationCodeResult.Failure.AlreadyInUse to ActivationCodeResult.AlreadyInUse,
            RequestActivationCodeResult.Failure.BlacklistedEmail to ActivationCodeResult.Blacklisted,
            RequestActivationCodeResult.Failure.DomainBlocked to ActivationCodeResult.DomainBlocked,
            RequestActivationCodeResult.Failure.InvalidEmail to ActivationCodeResult.InvalidEmail,
            RequestActivationCodeResult.Failure.Generic(failure) to ActivationCodeResult.Generic(failure),
        ).forEach { (kaliumResult, expected) ->
            coEvery { arrangement.requestActivationCode("alice@example.com") } returns kaliumResult

            val actual = arrangement.gateway.requestActivationCode(ServerConfig.PRODUCTION) { "alice@example.com" }
            assertEquals(expected, actual)
            if (actual is ActivationCodeResult.Generic) assertSame(failure, actual.failure)
        }
        coVerify(exactly = 6) { arrangement.requestActivationCode("alice@example.com") }
    }

    @Test
    fun `gateway reads email provider only after authentication scope resolves`() = runTest {
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
        coEvery { arrangement.requestActivationCode("latest@example.com") } returns RequestActivationCodeResult.Success

        val request = async {
            arrangement.gateway.requestActivationCode(ServerConfig.PRODUCTION) {
                events += "email"
                "latest@example.com"
            }
        }
        runCurrent()
        assertEquals(listOf("scope"), events)
        assertEquals(true, scopeRequested.isCompleted)

        releaseScope.complete(Unit)

        assertEquals(ActivationCodeResult.Sent, request.await())
        assertEquals(listOf("scope", "email"), events)
    }

    @Test
    fun `host factory maps flow custom default links and tos`() {
        val arrangement = Arrangement(defaultServerConfig = ServerConfig.STAGING)
        val custom = arrangement.hostFactory.create(
            CreateAccountRouteFlowType.TEAM,
            ServerConfig.PRODUCTION.toAuthenticationServerLinks(),
        )
        val fallback = arrangement.hostFactory.create(
            CreateAccountRouteFlowType.PERSONAL,
            null,
        )

        assertEquals(CreateAccountRouteFlowType.TEAM, custom.flowType)
        assertEquals(ServerConfig.PRODUCTION, custom.customServerConfig)
        assertEquals(ServerConfig.PRODUCTION, custom.serverConfig)
        assertEquals(ServerConfig.PRODUCTION.tos, custom.tosUrl())
        assertNull(fallback.customServerConfig)
        assertEquals(ServerConfig.STAGING, fallback.serverConfig)
        assertEquals(ServerConfig.STAGING.tos, fallback.tosUrl())
    }

    private class Arrangement(defaultServerConfig: ServerConfig.Links = ServerConfig.STAGING) {
        val validateEmail = mockk<ValidateEmailUseCase>()
        val coreLogic = mockk<CoreLogic>()
        val autoVersionAuthScope = mockk<AutoVersionAuthScopeUseCase>()
        val authenticationScope = mockk<AuthenticationScope>()
        val requestActivationCode = mockk<RequestActivationCodeUseCase>()
        val gateway = KaliumCreateAccountEmailGateway(validateEmail, coreLogic)
        val hostFactory = CreateAccountEmailViewModelHostFactory(validateEmail, coreLogic, defaultServerConfig)

        init {
            coEvery { coreLogic.versionedAuthenticationScope(any()) } returns autoVersionAuthScope
            every { authenticationScope.registerScope.requestActivationCode } returns requestActivationCode
        }
    }
}
