/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.authentication.login.sso

import android.database.sqlite.SQLiteException
import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.DefaultServerConfig
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.datastore.UserDataStore
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.ClientScopeProvider
import com.wire.android.framework.TestClient
import com.wire.android.framework.TestUser
import com.wire.android.ui.authentication.login.LoginNavArgs
import com.wire.android.ui.authentication.login.LoginPasswordPath
import com.wire.android.ui.authentication.login.LoginViewModelExtension
import com.wire.android.ui.authentication.login.SSOCodeAutoLogin
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.android.util.deeplink.SSOFailureCodes
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.session.StoreSessionParam
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.AuthenticationScope
import com.wire.kalium.logic.feature.auth.DomainLookupUseCase
import com.wire.kalium.logic.feature.auth.LogoutUseCase
import com.wire.kalium.logic.feature.auth.ValidateEmailUseCase
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.auth.sso.FetchSSOSettingsUseCase
import com.wire.kalium.logic.feature.auth.sso.SSOInitiateLoginResult
import com.wire.kalium.logic.feature.auth.sso.SSOLoginSessionResult
import com.wire.kalium.logic.feature.backup.RestoreCryptoStateResult
import com.wire.kalium.logic.feature.backup.RestoreCryptoStateUseCase
import com.wire.kalium.logic.feature.backup.SetLastDeviceIdResult
import com.wire.kalium.logic.feature.backup.SetLastDeviceIdUseCase
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.session.DeleteSessionUseCase
import com.wire.kalium.logic.feature.session.DoesValidSessionExistResult
import com.wire.kalium.logic.feature.session.DoesValidSessionExistUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginSSOViewModelHostFactoryTest {

    @Test
    fun `initiation maps every auth and SSO result preserving exact failures`() = runTest {
        val arrangement = Arrangement()
        val generic = NetworkFailure.NoNetworkConnection(null)
        val authFailures = listOf(
            AutoVersionAuthScopeUseCase.Result.Failure.TooNewVersion to LoginSSOFailure.ClientUpdateRequired,
            AutoVersionAuthScopeUseCase.Result.Failure.UnknownServerVersion to LoginSSOFailure.ServerVersionNotSupported,
            AutoVersionAuthScopeUseCase.Result.Failure.Generic(generic) to LoginSSOFailure.Generic(generic),
        )
        authFailures.forEach { (kalium, expected) ->
            coEvery { arrangement.ssoExtension.initiateSSO(any(), any(), any(), any(), any(), any()) } coAnswers {
                arg<(AutoVersionAuthScopeUseCase.Result.Failure) -> Unit>(3)(kalium)
            }
            val result = arrangement.gateway.initiateSSO(PRODUCTION, "wire-code", "shared") as LoginSSOInitiationResult.Failure
            assertEquals(expected, result.cause)
        }

        listOf(
            SSOInitiateLoginResult.Failure.InvalidCodeFormat to LoginSSOInitiationResult.InvalidCodeFormat,
            SSOInitiateLoginResult.Failure.InvalidCode to LoginSSOInitiationResult.InvalidCode,
            SSOInitiateLoginResult.Failure.Generic(generic) to LoginSSOInitiationResult.Failure(LoginSSOFailure.Generic(generic)),
        ).forEach { (kalium, expected) ->
            coEvery { arrangement.ssoExtension.initiateSSO(any(), any(), any(), any(), any(), any()) } coAnswers {
                arg<(SSOInitiateLoginResult.Failure) -> Unit>(4)(kalium)
            }
            assertEquals(expected, arrangement.gateway.initiateSSO(PRODUCTION, "wire-code", "shared"))
        }

        coEvery { arrangement.ssoExtension.initiateSSO(any(), any(), any(), any(), any(), any()) } coAnswers {
            arg<(SSOInitiateLoginResult.Failure) -> Unit>(4)(SSOInitiateLoginResult.Failure.InvalidRedirect)
        }
        val invalidRedirect = arrangement.gateway.initiateSSO(PRODUCTION, "wire-code", null) as LoginSSOInitiationResult.Failure
        val unknown = (invalidRedirect.cause as LoginSSOFailure.Generic).failure as CoreFailure.Unknown
        assertEquals("Invalid Redirect", unknown.rootCause?.message)

        coEvery { arrangement.ssoExtension.initiateSSO(any(), any(), any(), any(), any(), any()) } coAnswers {
            arg<suspend (String) -> Unit>(5)("https://sso")
        }
        assertEquals(
            LoginSSOInitiationResult.Success("https://sso"),
            arrangement.gateway.initiateSSO(PRODUCTION, "wire-code", "shared"),
        )
    }

    @Test
    fun `domain lookup resolves exact default config before reading latest email`() = runTest {
        val arrangement = Arrangement()
        val scopeRequested = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        every { arrangement.coreLogic.versionedAuthenticationScope(DefaultServerConfig) } returns arrangement.autoVersion
        coEvery { arrangement.autoVersion(null) } coAnswers {
            scopeRequested.complete(Unit)
            release.await()
            AutoVersionAuthScopeUseCase.Result.Success(arrangement.authenticationScope)
        }
        coEvery { arrangement.authenticationScope.domainLookup("latest@example.com") } returns
            DomainLookupUseCase.Result.Success(PRODUCTION)
        var email = "first@example.com"

        val result = async { arrangement.gateway.lookupDomain { email } }
        scopeRequested.await()
        email = "latest@example.com"
        release.complete(Unit)

        assertEquals(LoginSSODomainLookupResult.Success(PRODUCTION), result.await())
        coVerify(exactly = 1) { arrangement.authenticationScope.domainLookup("latest@example.com") }
    }

    @Test
    fun `domain auth failures collapse to unavailable and domain failure preserves identity`() = runTest {
        val arrangement = Arrangement()
        every { arrangement.coreLogic.versionedAuthenticationScope(any()) } returns arrangement.autoVersion
        listOf(
            AutoVersionAuthScopeUseCase.Result.Failure.TooNewVersion,
            AutoVersionAuthScopeUseCase.Result.Failure.UnknownServerVersion,
            AutoVersionAuthScopeUseCase.Result.Failure.Generic(NetworkFailure.NoNetworkConnection(null)),
        ).forEach {
            coEvery { arrangement.autoVersion(null) } returns it
            assertEquals(LoginSSODomainLookupResult.AuthenticationUnavailable, arrangement.gateway.lookupDomain { "email" })
        }

        val failure = NetworkFailure.NoNetworkConnection(null)
        coEvery { arrangement.autoVersion(null) } returns AutoVersionAuthScopeUseCase.Result.Success(arrangement.authenticationScope)
        coEvery { arrangement.authenticationScope.domainLookup("email") } returns DomainLookupUseCase.Result.Failure(failure)
        val result = arrangement.gateway.lookupDomain { "email" } as LoginSSODomainLookupResult.Failure
        assertSame(failure, result.failure)
    }

    @Test
    fun `default code maps success auth failure and silently unavailable fetch failure`() = runTest {
        val arrangement = Arrangement()
        coEvery { arrangement.ssoExtension.fetchDefaultSSOCode(any(), any(), any(), any()) } coAnswers {
            arg<suspend (String?) -> Unit>(3)("wire-default")
        }
        assertEquals(LoginSSODefaultCodeResult.Success("wire-default"), arrangement.gateway.fetchDefaultSSOCode(PRODUCTION))

        coEvery { arrangement.ssoExtension.fetchDefaultSSOCode(any(), any(), any(), any()) } coAnswers {
            arg<(FetchSSOSettingsUseCase.Result.Failure) -> Unit>(2)(
                FetchSSOSettingsUseCase.Result.Failure(CoreFailure.Unknown(Exception("fetch")))
            )
        }
        assertEquals(LoginSSODefaultCodeResult.Unavailable, arrangement.gateway.fetchDefaultSSOCode(PRODUCTION))

        coEvery { arrangement.ssoExtension.fetchDefaultSSOCode(any(), any(), any(), any()) } coAnswers {
            arg<(AutoVersionAuthScopeUseCase.Result.Failure) -> Unit>(1)(AutoVersionAuthScopeUseCase.Result.Failure.TooNewVersion)
        }
        assertEquals(
            LoginSSODefaultCodeResult.Failure<CoreFailure>(LoginSSOFailure.ClientUpdateRequired),
            arrangement.gateway.fetchDefaultSSOCode(PRODUCTION),
        )
    }

    @Test
    fun `session maps callbacks and retains opaque identity session nomad meaning`() = runTest {
        val arrangement = Arrangement()
        val retained = mockk<StoreSessionParam>()
        every { retained.nomadServiceUrl } returns "nomad"
        coEvery { arrangement.ssoExtension.establishSSOSession(any(), any(), any(), any(), any(), any(), any(), any(), any()) } coAnswers {
            arg<suspend (StoreSessionParam) -> Unit>(8)(retained)
        }
        assertEquals(
            LoginSSOSessionResult.IdentityChanged(retained, true),
            arrangement.gateway.establishSession("cookie", "config", { "nomad" }, { "shared" }),
        )
        every { retained.nomadServiceUrl } returns null
        assertEquals(
            LoginSSOSessionResult.IdentityChanged(retained, false),
            arrangement.gateway.establishSession("cookie", "config", { null }, { null }),
        )

        coEvery { arrangement.ssoExtension.establishSSOSession(any(), any(), any(), any(), any(), any(), any(), any(), any()) } coAnswers {
            arg<(SSOLoginSessionResult.Failure) -> Unit>(5)(SSOLoginSessionResult.Failure.InvalidCookie)
        }
        assertEquals(
            LoginSSOSessionResult.InvalidCookie,
            arrangement.gateway.establishSession("cookie", "config", { null }, { null }),
        )

        coEvery { arrangement.ssoExtension.establishSSOSession(any(), any(), any(), any(), any(), any(), any(), any(), any()) } coAnswers {
            arg<(AddAuthenticatedUserUseCase.Result.Failure) -> Unit>(6)(
                AddAuthenticatedUserUseCase.Result.Failure.NomadSingleUserViolation
            )
        }
        assertEquals(
            LoginSSOSessionResult.UserAlreadyExists,
            arrangement.gateway.establishSession("cookie", "config", { null }, { null }),
        )

        coEvery { arrangement.ssoExtension.establishSSOSession(any(), any(), any(), any(), any(), any(), any(), any(), any()) } coAnswers {
            arg<suspend (com.wire.kalium.logic.data.user.UserId) -> Unit>(7)(TestUser.SELF_USER_ID)
        }
        assertEquals(
            LoginSSOSessionResult.Success(TestUser.SELF_USER_ID),
            arrangement.gateway.establishSession("cookie", "config", { null }, { null }),
        )
    }

    @Test
    fun `replacement maps success user conflicts and generic identity`() = runTest {
        val arrangement = Arrangement()
        val session = mockk<StoreSessionParam>()
        coEvery { arrangement.ssoExtension.replaceRetainedSsoSession(session) } returns
            ReplaceRetainedSsoSessionResult.Success(TestUser.SELF_USER_ID)
        assertEquals(LoginSSOReplaceSessionResult.Success(TestUser.SELF_USER_ID), arrangement.gateway.replaceRetainedSession(session))

        coEvery { arrangement.ssoExtension.replaceRetainedSsoSession(session) } returns
            ReplaceRetainedSsoSessionResult.Failure(AddAuthenticatedUserUseCase.Result.Failure.SsoIdentityChanged)
        assertEquals(LoginSSOReplaceSessionResult.UserAlreadyExists, arrangement.gateway.replaceRetainedSession(session))

        val failure = NetworkFailure.NoNetworkConnection(null)
        coEvery { arrangement.ssoExtension.replaceRetainedSsoSession(session) } returns
            ReplaceRetainedSsoSessionResult.Failure(AddAuthenticatedUserUseCase.Result.Failure.Generic(failure))
        assertSame(failure, (arrangement.gateway.replaceRetainedSession(session) as LoginSSOReplaceSessionResult.Failure).failure)
    }

    @Test
    fun `client result includes sync and sets last device only for success`() = runTest {
        val arrangement = Arrangement()
        coEvery { arrangement.loginExtension.registerClient(TestUser.SELF_USER_ID, null) } returns
            RegisterClientResult.Success(TestClient.CLIENT)
        coEvery { arrangement.loginExtension.isInitialSyncCompleted(TestUser.SELF_USER_ID) } returns false
        every { arrangement.coreLogic.getSessionScope(TestUser.SELF_USER_ID).backup.setLastDeviceId } returns arrangement.setLastDevice
        coEvery { arrangement.setLastDevice(TestClient.CLIENT_ID.value) } returns SetLastDeviceIdResult.Success

        assertEquals(
            LoginSSORegisterClientResult.Success(false),
            arrangement.gateway.registerClient(TestUser.SELF_USER_ID, true),
        )
        coVerifyOrder {
            arrangement.loginExtension.registerClient(TestUser.SELF_USER_ID, null)
            arrangement.setLastDevice(TestClient.CLIENT_ID.value)
            arrangement.loginExtension.isInitialSyncCompleted(TestUser.SELF_USER_ID)
        }

        coEvery { arrangement.loginExtension.registerClient(TestUser.SELF_USER_ID, null) } returns
            mockk<RegisterClientResult.E2EICertificateRequired>()
        coEvery { arrangement.loginExtension.isInitialSyncCompleted(TestUser.SELF_USER_ID) } returns true
        assertEquals(
            LoginSSORegisterClientResult.E2EICertificateRequired(true),
            arrangement.gateway.registerClient(TestUser.SELF_USER_ID, true),
        )
        coVerify(exactly = 1) { arrangement.setLastDevice(any()) }

        listOf(
            RegisterClientResult.Failure.TooManyClients to LoginSSORegisterClientResult.TooManyDevices,
            RegisterClientResult.Failure.InvalidCredentials.InvalidPassword to LoginSSORegisterClientResult.InvalidCredentials,
            RegisterClientResult.Failure.PasswordAuthRequired to LoginSSORegisterClientResult.PasswordRequired,
        ).forEach { (kalium, expected) ->
            coEvery { arrangement.loginExtension.registerClient(TestUser.SELF_USER_ID, null) } returns kalium
            assertEquals(expected, arrangement.gateway.registerClient(TestUser.SELF_USER_ID, false))
        }
    }

    @Test
    fun `crypto restore maps success no backup synthetic failure and exact exception validity matrix`() = runTest {
        val arrangement = Arrangement()
        every { arrangement.coreLogic.getSessionScope(TestUser.SELF_USER_ID).backup.restoreCryptoState } returns arrangement.restore
        coEvery { arrangement.loginExtension.isInitialSyncCompleted(TestUser.SELF_USER_ID) } returns true

        coEvery { arrangement.restore() } returns RestoreCryptoStateResult.Success
        assertEquals(LoginSSORestoreResult.Success(true), arrangement.gateway.restoreCryptoState(TestUser.SELF_USER_ID))
        coEvery { arrangement.restore() } returns RestoreCryptoStateResult.NoBackupAvailable
        assertEquals(LoginSSORestoreResult.NoBackupAvailable, arrangement.gateway.restoreCryptoState(TestUser.SELF_USER_ID))
        coEvery { arrangement.restore() } returns RestoreCryptoStateResult.Failure
        val failure = arrangement.gateway.restoreCryptoState(TestUser.SELF_USER_ID) as LoginSSORestoreResult.Failure
        assertEquals("Failed to restore crypto state", (failure.failure as CoreFailure.Unknown).rootCause?.message)

        every { arrangement.coreLogic.getGlobalScope().doesValidSessionExist } returns arrangement.validSession
        val recoverable = listOf(
            IllegalStateException("closed"),
            IOException("gone"),
            mockk<SQLiteException> { every { message } returns "database gone" },
        )
        recoverable.forEach { exception ->
            coEvery { arrangement.restore() } throws exception
            coEvery { arrangement.validSession(TestUser.SELF_USER_ID) } returns DoesValidSessionExistResult.Success(false)
            assertEquals(LoginSSORestoreResult.SessionUnavailable, arrangement.gateway.restoreCryptoState(TestUser.SELF_USER_ID))
            coEvery { arrangement.validSession(TestUser.SELF_USER_ID) } returns DoesValidSessionExistResult.Success(true)
            assertSame(exception, thrownBy { arrangement.gateway.restoreCryptoState(TestUser.SELF_USER_ID) })
        }

        val cancellation = CancellationException("cancel")
        coEvery { arrangement.restore() } throws cancellation
        assertSame(cancellation, thrownBy { arrangement.gateway.restoreCryptoState(TestUser.SELF_USER_ID) })
        val unexpected = RuntimeException("unexpected")
        coEvery { arrangement.restore() } throws unexpected
        assertSame(unexpected, thrownBy { arrangement.gateway.restoreCryptoState(TestUser.SELF_USER_ID) })
    }

    @Test
    fun `revert orders hard logout before delete and preserves exact exception validity matrix`() = runTest {
        val arrangement = Arrangement()
        every { arrangement.coreLogic.getSessionScope(TestUser.SELF_USER_ID).logout } returns arrangement.logout
        every { arrangement.coreLogic.getGlobalScope().deleteSession } returns arrangement.deleteSession
        coEvery { arrangement.logout(LogoutReason.SELF_HARD_LOGOUT, true) } returns Unit
        coEvery { arrangement.deleteSession(TestUser.SELF_USER_ID) } returns DeleteSessionUseCase.Result.Success

        arrangement.gateway.revertSession(TestUser.SELF_USER_ID)
        coVerifyOrder {
            arrangement.logout(LogoutReason.SELF_HARD_LOGOUT, true)
            arrangement.deleteSession(TestUser.SELF_USER_ID)
        }

        every { arrangement.coreLogic.getGlobalScope().doesValidSessionExist } returns arrangement.validSession
        val recoverable = listOf(
            IllegalStateException("closed"),
            IOException("gone"),
            mockk<SQLiteException> { every { message } returns "database gone" },
        )
        recoverable.forEach { exception ->
            coEvery { arrangement.logout(any(), any()) } throws exception
            coEvery { arrangement.validSession(TestUser.SELF_USER_ID) } returns DoesValidSessionExistResult.Success(false)
            assertNull(thrownBy { arrangement.gateway.revertSession(TestUser.SELF_USER_ID) })
            coEvery { arrangement.validSession(TestUser.SELF_USER_ID) } returns DoesValidSessionExistResult.Success(true)
            assertSame(exception, thrownBy { arrangement.gateway.revertSession(TestUser.SELF_USER_ID) })
        }

        val cancellation = CancellationException("cancel")
        coEvery { arrangement.logout(any(), any()) } throws cancellation
        assertSame(cancellation, thrownBy { arrangement.gateway.revertSession(TestUser.SELF_USER_ID) })
        val unexpected = RuntimeException("unexpected")
        coEvery { arrangement.logout(any(), any()) } throws unexpected
        assertSame(unexpected, thrownBy { arrangement.gateway.revertSession(TestUser.SELF_USER_ID) })
    }

    @Test
    fun `host maps custom default saved state and deep links exactly`() = runTest {
        val arrangement = Arrangement()
        val factory = LoginSSOViewModelHostFactory(
            arrangement.validateEmail,
            arrangement.coreLogic,
            arrangement.addAuthenticatedUser,
            arrangement.clientScopeFactory,
            arrangement.userDataStoreProvider,
            STAGING,
            false,
            arrangement.dispatchers,
        )
        val saved = SavedStateHandle(mapOf("sso_code" to "saved"))
        val custom = factory.create(
            LoginNavArgs(
                loginPasswordPath = LoginPasswordPath(PRODUCTION),
                ssoCodeAutoLogin = SSOCodeAutoLogin("wire", false, "nomad", "shared"),
            ),
            saved,
        )
        val fallback = factory.create(LoginNavArgs(), SavedStateHandle())
        assertEquals(PRODUCTION, custom.serverConfig)
        assertEquals("saved", custom.ssoTextState.text.toString())
        assertEquals(STAGING, fallback.serverConfig)

        custom.handleSSOResult(DeepLinkResult.SSOLogin.Failure(SSOFailureCodes.Unknown))
        assertEquals(
            SSOFailureCodes.Unknown,
            (custom.loginState.flowState as com.wire.android.ui.authentication.login.LoginState.Error.DialogError.SSOResultError).result,
        )
        fallback.handleSSOResult(null)
        assertFalse(fallback.loginState.flowState is com.wire.android.ui.authentication.login.LoginState.Error<*, *, *>)

        val successMapperTarget = mockk<AppLoginSSOViewModel>(relaxed = true)
        successMapperTarget.handleSSOResult(DeepLinkResult.SSOLogin.Success("cookie", "server-config"))
        verify(exactly = 1) { successMapperTarget.establishSSOSession("cookie", "server-config") }
    }

    private class Arrangement {
        val validateEmail = mockk<ValidateEmailUseCase> { every { this@mockk(any()) } returns false }
        val coreLogic = mockk<CoreLogic>()
        val loginExtension = mockk<LoginViewModelExtension>()
        val ssoExtension = mockk<LoginSSOViewModelExtension>()
        val dispatchers = TestDispatcherProvider()
        val gateway = KaliumLoginSSOGateway(validateEmail, coreLogic, loginExtension, ssoExtension, dispatchers)
        val autoVersion = mockk<AutoVersionAuthScopeUseCase>()
        val authenticationScope = mockk<AuthenticationScope>()
        val setLastDevice = mockk<SetLastDeviceIdUseCase>()
        val restore = mockk<RestoreCryptoStateUseCase>()
        val validSession = mockk<DoesValidSessionExistUseCase>()
        val logout = mockk<LogoutUseCase>()
        val deleteSession = mockk<DeleteSessionUseCase>()
        val addAuthenticatedUser = mockk<AddAuthenticatedUserUseCase>()
        val clientScopeFactory = mockk<ClientScopeProvider.Factory>()
        val userDataStoreProvider = mockk<UserDataStoreProvider>()

        init {
            every { coreLogic.versionedAuthenticationScope(any()) } returns autoVersion
            every { userDataStoreProvider.getOrCreate(any()) } returns mockk<UserDataStore>(relaxed = true)
            every { clientScopeFactory.create(any()) } returns mockk(relaxed = true)
        }
    }

    private suspend fun thrownBy(block: suspend () -> Unit): Throwable? = try {
        block()
        null
    } catch (error: Throwable) {
        error
    }

    private companion object {
        val PRODUCTION = ServerConfig.PRODUCTION
        val STAGING = ServerConfig.STAGING
    }
}
