package com.wire.android.ui.authentication.create.code

import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.register.RegisterParam
import com.wire.kalium.logic.feature.register.RegisterResult
import com.wire.kalium.logic.feature.register.RequestActivationCodeResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateAccountCodeGatewayRegistrationTest {
    @Test
    fun `scope failures map to unavailable without invoking operations`() = runTest {
        val a = createAccountCodeArrangement()
        listOf(
            AutoVersionAuthScopeUseCase.Result.Failure.UnknownServerVersion,
            AutoVersionAuthScopeUseCase.Result.Failure.TooNewVersion,
            AutoVersionAuthScopeUseCase.Result.Failure.Generic(NetworkFailure.NoNetworkConnection(null)),
        ).forEach { failure ->
            coEvery { a.autoVersionAuthScope(null) } returns failure
            assertEquals(
                ActivationCodeRequestResult.AuthScopeUnavailable,
                a.gateway.requestActivationCode(ServerConfig.PRODUCTION, "alice@example.com")
            )
            assertEquals(
                AccountRegistrationResult.AuthScopeUnavailable,
                a.gateway.register(ServerConfig.PRODUCTION, personalRequest()),
            )
        }
        coVerify(exactly = 0) { a.requestActivationCode(any()) }
        coVerify(exactly = 0) { a.register(any()) }
    }

    @Test
    fun `activation request maps every result preserving generic identity`() = runTest {
        val a = createAccountCodeArrangement().withAuthScope()
        val failure = NetworkFailure.NoNetworkConnection(null)
        listOf(
            RequestActivationCodeResult.Success to ActivationCodeRequestResult.Sent,
            RequestActivationCodeResult.Failure.AlreadyInUse to ActivationCodeRequestResult.AlreadyInUse,
            RequestActivationCodeResult.Failure.BlacklistedEmail to ActivationCodeRequestResult.Blacklisted,
            RequestActivationCodeResult.Failure.DomainBlocked to ActivationCodeRequestResult.DomainBlocked,
            RequestActivationCodeResult.Failure.InvalidEmail to ActivationCodeRequestResult.InvalidEmail,
            RequestActivationCodeResult.Failure.Generic(failure) to ActivationCodeRequestResult.Generic(failure),
        ).forEach { (kalium, feature) ->
            coEvery { a.requestActivationCode("alice@example.com") } returns kalium
            val actual = a.gateway.requestActivationCode(ServerConfig.PRODUCTION, "alice@example.com")
            assertEquals(feature, actual)
            if (actual is ActivationCodeRequestResult.Generic) {
                assertSame(failure, actual.failure)
            }
        }
    }

    @Test
    fun `register resolves scope before reading activation code and maps exact parameters`() = runTest {
        val a = createAccountCodeArrangement()
        val requested = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val events = mutableListOf<String>()
        coEvery { a.autoVersionAuthScope(null) } coAnswers {
            events += "scope"
            requested.complete(Unit)
            release.await()
            AutoVersionAuthScopeUseCase.Result.Success(a.authenticationScope)
        }
        val parameter = slot<RegisterParam>()
        coEvery { a.register(capture(parameter)) } returns RegisterResult.Failure.InvalidActivationCode
        val result = async {
            a.gateway.register(
                ServerConfig.PRODUCTION,
                personalRequest {
                    events += "code"
                    "654321"
                },
            )
        }
        runCurrent()
        assertEquals(listOf("scope"), events)
        release.complete(Unit)
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
        val a = createAccountCodeArrangement().withAuthScope()
        val parameter = slot<RegisterParam>()
        coEvery { a.register(capture(parameter)) } returns RegisterResult.Failure.InvalidActivationCode
        a.gateway.register(
            ServerConfig.PRODUCTION,
            CreateAccountRegistrationRequest.Team(
                "Alice",
                "Wire",
                "secret",
                "alice@example.com",
                { "123456" },
                "Wire Team",
            ),
        )
        val team = parameter.captured as RegisterParam.Team
        assertEquals("Alice", team.firstName)
        assertEquals("Wire", team.lastName)
        assertEquals("secret", team.password)
        assertEquals("alice@example.com", team.email)
        assertEquals("123456", team.emailActivationCode)
        assertEquals("Wire Team", team.teamName)
        assertEquals("default", team.teamIcon)
        val success = mockk<RegisterResult.Success>()
        coEvery { a.register(any()) } returns success
        assertSame(
            success,
            (
                a.gateway.register(
                    ServerConfig.PRODUCTION,
                    personalRequest(),
                ) as AccountRegistrationResult.Success
            ).credentials.result,
        )
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
            coEvery { a.register(any()) } returns kalium
            val actual = a.gateway.register(ServerConfig.PRODUCTION, personalRequest())
            assertEquals(feature, actual)
            if (actual is AccountRegistrationResult.Generic) {
                assertSame(failure, actual.failure)
            }
        }
    }
}
