/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.newauthentication.login

import android.database.sqlite.SQLiteException
import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.SnapshotExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.ClientScopeProvider
import com.wire.android.framework.TestClient
import com.wire.android.framework.TestUser
import com.wire.android.ui.authentication.login.DomainClaimedByOrg
import com.wire.android.ui.authentication.login.LoginNavArgs
import com.wire.android.ui.authentication.login.LoginPasswordPath
import com.wire.android.ui.authentication.login.LoginViewModelExtension
import com.wire.android.ui.authentication.login.PreFilledUserIdentifierType
import com.wire.android.ui.authentication.login.SSOCodeAutoLogin
import com.wire.android.ui.authentication.login.sso.LoginSSOViewModelExtension
import com.wire.android.ui.authentication.login.sso.ReplaceRetainedSsoSessionResult
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
import com.wire.kalium.logic.feature.auth.EnterpriseLoginResult
import com.wire.kalium.logic.feature.auth.LoginRedirectPath
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.auth.sso.FetchSSOSettingsUseCase
import com.wire.kalium.logic.feature.auth.sso.SSOInitiateLoginResult
import com.wire.kalium.logic.feature.backup.RestoreCryptoStateResult
import com.wire.kalium.logic.feature.backup.RestoreCryptoStateUseCase
import com.wire.kalium.logic.feature.backup.SetLastDeviceIdResult
import com.wire.kalium.logic.feature.backup.SetLastDeviceIdUseCase
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.server.GetServerConfigResult
import com.wire.kalium.logic.feature.server.GetServerConfigUseCase
import com.wire.kalium.logic.feature.session.DeleteSessionUseCase
import com.wire.kalium.logic.feature.session.DoesValidSessionExistResult
import com.wire.kalium.logic.feature.session.DoesValidSessionExistUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class, SnapshotExtension::class)
class NewLoginViewModelHostFactoryTest {
    @Test
    fun `saved state and host mappers preserve identifier IDP deep link and claimed domain`() {
        val handle = SavedStateHandle()
        val store = SavedStateNewLoginStore(handle)
        store.userIdentifier = "user@example.com"
        store.pendingSsoIdentityProviderId = "idp"
        assertEquals("user@example.com", store.userIdentifier)
        assertEquals("idp", store.consumePendingSsoIdentityProviderId())
        assertNull(store.pendingSsoIdentityProviderId)

        assertEquals(
            NewLoginSsoCallback.Success("cookie", "config"),
            DeepLinkResult.SSOLogin.Success("cookie", "config").toNewLoginSsoCallback(),
        )
        assertEquals(
            NewLoginSsoCallback.Failure(SSOFailureCodes.Unknown),
            DeepLinkResult.SSOLogin.Failure(SSOFailureCodes.Unknown).toNewLoginSsoCallback(),
        )
        val password = NewLoginAction.EmailPassword("user", PRODUCTION, false, "wire.example").toLoginPasswordPath()
        assertEquals(PRODUCTION, password.customServerConfig)
        assertEquals(false, password.isCloudAccountCreationPossible)
        assertEquals(DomainClaimedByOrg.Claimed("wire.example"), password.isDomainClaimedByOrg)
    }

    @Test
    fun `enterprise mapping uses current server and preserves SSO raw IDP and auth failures`() = runTest {
        val arrangement = Arrangement()
        coEvery { arrangement.ssoExtension.withAuthenticationScope(any(), any(), any()) } coAnswers {
            arg<suspend (AuthenticationScope) -> Unit>(2)(arrangement.authenticationScope)
        }
        coEvery { arrangement.authenticationScope.getLoginFlowForDomainUseCase("user@example.com") } returns
            EnterpriseLoginResult.Success(LoginRedirectPath.SSO("raw-idp"))
        assertEquals(
            NewLoginEnterpriseResult.Sso("wire-raw-idp", "raw-idp"),
            arrangement.gateway.enterpriseLogin(STAGING, "user@example.com"),
        )
        coEvery { arrangement.ssoExtension.withAuthenticationScope(any(), any(), any()) } coAnswers {
            arg<(AutoVersionAuthScopeUseCase.Result.Failure) -> Unit>(1)(
                AutoVersionAuthScopeUseCase.Result.Failure.TooNewVersion
            )
        }
        assertEquals(
            NewLoginEnterpriseResult.Failure<CoreFailure>(NewLoginFailure.ClientUpdateRequired),
            arrangement.gateway.enterpriseLogin(STAGING, "user@example.com"),
        )
        coVerify { arrangement.ssoExtension.withAuthenticationScope(STAGING, any(), any()) }
    }

    @Test
    fun `SSO initiation and default settings preserve exact result mapping`() = runTest {
        val arrangement = Arrangement()
        coEvery { arrangement.ssoExtension.initiateSSO(any(), any(), any(), any(), any(), any()) } coAnswers {
            arg<(SSOInitiateLoginResult.Failure) -> Unit>(4)(SSOInitiateLoginResult.Failure.InvalidRedirect)
        }
        val invalid = arrangement.gateway.initiateSso(PRODUCTION, "wire-code", "shared") as NewLoginSsoInitiationResult.Failure
        val unknown = (invalid.cause as NewLoginFailure.Generic).failure as CoreFailure.Unknown
        assertEquals("Invalid Redirect", unknown.rootCause?.message)

        coEvery { arrangement.ssoExtension.fetchDefaultSSOCode(any(), any(), any(), any()) } coAnswers {
            arg<suspend (String?) -> Unit>(3)("wire-default")
        }
        assertEquals(
            NewLoginDefaultSsoCodeResult.Success("wire-default"),
            arrangement.gateway.fetchDefaultSsoCode(PRODUCTION),
        )
        val failure = NetworkFailure.NoNetworkConnection(null)
        coEvery { arrangement.ssoExtension.fetchDefaultSSOCode(any(), any(), any(), any()) } coAnswers {
            arg<(FetchSSOSettingsUseCase.Result.Failure) -> Unit>(2)(FetchSSOSettingsUseCase.Result.Failure(failure))
        }
        val mapped = arrangement.gateway.fetchDefaultSsoCode(PRODUCTION) as NewLoginDefaultSsoCodeResult.Failure
        assertSame(failure, (mapped.cause as NewLoginFailure.Generic).failure)
    }

    @Test
    fun `session and replacement preserve identity changed Nomad meaning and conflicts`() = runTest {
        val arrangement = Arrangement()
        val retained = mockk<StoreSessionParam>()
        every { retained.nomadServiceUrl } returns "nomad"
        coEvery {
            arrangement.ssoExtension.establishSSOSession(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } coAnswers { arg<suspend (StoreSessionParam) -> Unit>(8)(retained) }
        assertEquals(
            NewLoginSessionResult.IdentityChanged(retained, true),
            arrangement.gateway.establishSession("cookie", "config", "idp", { "nomad" }, { "shared" }),
        )

        coEvery { arrangement.ssoExtension.replaceRetainedSsoSession(retained) } returns
            ReplaceRetainedSsoSessionResult.Failure(AddAuthenticatedUserUseCase.Result.Failure.SsoIdentityChanged)
        assertEquals(NewLoginReplaceSessionResult.SsoIdentityChanged, arrangement.gateway.replaceRetainedSession(retained))

        coEvery { arrangement.ssoExtension.replaceRetainedSsoSession(retained) } returns
            ReplaceRetainedSsoSessionResult.Failure(AddAuthenticatedUserUseCase.Result.Failure.UserAlreadyExists)
        assertEquals(NewLoginReplaceSessionResult.UserAlreadyExists, arrangement.gateway.replaceRetainedSession(retained))
        coVerify {
            arrangement.ssoExtension.establishSSOSession(
                "cookie", "config", any(), any(), any(), any(), any(), any(), any(), "idp"
            )
        }
    }

    @Test
    fun `client mapping sets last device only on successful Nomad fallback and keeps impossible results synthetic`() = runTest {
        val arrangement = Arrangement()
        every {
            arrangement.coreLogic.getSessionScope(TestUser.SELF_USER_ID).backup.setLastDeviceId
        } returns arrangement.setLastDevice
        coEvery { arrangement.loginExtension.registerClient(TestUser.SELF_USER_ID, null) } returns
            RegisterClientResult.Success(TestClient.CLIENT)
        coEvery { arrangement.loginExtension.isInitialSyncCompleted(TestUser.SELF_USER_ID) } returns false
        coEvery { arrangement.setLastDevice(TestClient.CLIENT_ID.value) } returns SetLastDeviceIdResult.Success
        assertEquals(
            NewLoginRegisterClientResult.Success(false),
            arrangement.gateway.registerClient(TestUser.SELF_USER_ID, true),
        )
        coVerifyOrder {
            arrangement.loginExtension.registerClient(TestUser.SELF_USER_ID, null)
            arrangement.setLastDevice(TestClient.CLIENT_ID.value)
            arrangement.loginExtension.isInitialSyncCompleted(TestUser.SELF_USER_ID)
        }

        coEvery { arrangement.loginExtension.registerClient(TestUser.SELF_USER_ID, null) } returns
            mockk<RegisterClientResult.E2EICertificateRequired>()
        assertEquals(
            NewLoginRegisterClientResult.E2EICertificateRequired,
            arrangement.gateway.registerClient(TestUser.SELF_USER_ID, true),
        )
        coVerify(exactly = 1) { arrangement.setLastDevice(any()) }

        coEvery { arrangement.loginExtension.registerClient(TestUser.SELF_USER_ID, null) } returns
            RegisterClientResult.Failure.PasswordAuthRequired
        val impossible = arrangement.gateway.registerClient(TestUser.SELF_USER_ID, false) as NewLoginRegisterClientResult.Failure
        assertEquals("PasswordAuthRequired", (impossible.failure as CoreFailure.Unknown).rootCause?.message)
    }

    @Test
    fun `restore and revert preserve synthetic failure ordering and exception validity matrix`() = runTest {
        val arrangement = Arrangement()
        every {
            arrangement.coreLogic.getSessionScope(TestUser.SELF_USER_ID).backup.restoreCryptoState
        } returns arrangement.restore
        coEvery { arrangement.restore() } returns RestoreCryptoStateResult.Failure
        val failure = arrangement.gateway.restoreCryptoState(TestUser.SELF_USER_ID) as NewLoginRestoreResult.Failure
        assertEquals("Failed to restore crypto state", (failure.failure as CoreFailure.Unknown).rootCause?.message)

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
        listOf(
            IllegalStateException("closed"),
            IOException("gone"),
            mockk<SQLiteException> { every { message } returns "database gone" },
        ).forEach { exception ->
            coEvery { arrangement.restore() } throws exception
            coEvery { arrangement.validSession(TestUser.SELF_USER_ID) } returns DoesValidSessionExistResult.Success(false)
            assertEquals(NewLoginRestoreResult.SessionUnavailable, arrangement.gateway.restoreCryptoState(TestUser.SELF_USER_ID))
            coEvery { arrangement.validSession(TestUser.SELF_USER_ID) } returns DoesValidSessionExistResult.Success(true)
            assertSame(exception, thrownBy { arrangement.gateway.restoreCryptoState(TestUser.SELF_USER_ID) })
        }
        val cancellation = CancellationException("cancel")
        coEvery { arrangement.restore() } throws cancellation
        assertSame(cancellation, thrownBy { arrangement.gateway.restoreCryptoState(TestUser.SELF_USER_ID) })
    }

    @Test
    fun `host factory maps custom prefill managed code and pending auto-login values`() = runTest {
        val arrangement = Arrangement()
        val factory = NewLoginViewModelHostFactory(
            arrangement.validator,
            arrangement.coreLogic,
            mockk<AddAuthenticatedUserUseCase>(),
            mockk<ClientScopeProvider.Factory>(),
            mockk<UserDataStoreProvider>(),
            arrangement.dispatchers,
            STAGING,
            "managed",
            true,
            false,
            lazy { mockk<GetServerConfigUseCase>() },
            lazy { mockk<GlobalDataStore>() },
        )
        val custom = factory.create(
            LoginNavArgs(
                userHandle = PreFilledUserIdentifierType.PreFilled("prefilled"),
                loginPasswordPath = LoginPasswordPath(PRODUCTION),
                ssoCodeAutoLogin = SSOCodeAutoLogin("wire-auto", nomadServiceUrl = "nomad", cookieLabel = "shared"),
            ),
            SavedStateHandle(),
        )
        assertEquals(PRODUCTION, custom.serverConfig)
        assertEquals("prefilled", custom.userIdentifierTextState.text.toString())

        val managed = factory.create(LoginNavArgs(), SavedStateHandle())
        assertEquals(STAGING, managed.serverConfig)
        assertEquals("wire-managed", managed.userIdentifierTextState.text.toString())
        val updated = LoginNavArgs(loginPasswordPath = LoginPasswordPath(PRODUCTION)).toNewLoginNavigationInput()
        assertEquals(PRODUCTION, updated.customServerConfig)
        assertTrue(updated.isCustomServerConfigured)
    }

    @Test
    fun `backend adapter maps null failure and success after applying host side effects`() = runTest {
        assertEquals(
            NewLoginBackendResult.Failure,
            AndroidNewLoginBackendGateway(null, null, TestDispatcherProvider()).configure("https://config"),
        )

        val useCase = mockk<GetServerConfigUseCase>()
        val dataStore = mockk<GlobalDataStore>()
        coEvery { useCase("https://config") } returns GetServerConfigResult.Success(PRODUCTION)
        coEvery { dataStore.setBackendSupportEmail(PRODUCTION.api, PRODUCTION.supportEmail) } returns Unit
        val gateway = AndroidNewLoginBackendGateway(lazy { useCase }, lazy { dataStore }, TestDispatcherProvider())
        assertEquals(NewLoginBackendResult.Success(PRODUCTION), gateway.configure("https://config"))
        coVerifyOrder {
            useCase("https://config")
            dataStore.setBackendSupportEmail(PRODUCTION.api, PRODUCTION.supportEmail)
        }

        val failure = NetworkFailure.NoNetworkConnection(null)
        coEvery { useCase("https://config") } returns GetServerConfigResult.Failure.Generic(failure)
        assertEquals(NewLoginBackendResult.Failure, gateway.configure("https://config"))
    }

    private class Arrangement {
        val validator = mockk<ValidateEmailOrSSOCodeUseCase> {
            every { this@mockk(any()) } returns ValidateEmailOrSSOCodeUseCase.Result.ValidEmail
        }
        val coreLogic = mockk<CoreLogic>()
        val loginExtension = mockk<LoginViewModelExtension>()
        val ssoExtension = mockk<LoginSSOViewModelExtension>()
        val dispatchers = TestDispatcherProvider()
        val gateway = KaliumNewLoginGateway(validator, coreLogic, loginExtension, ssoExtension, dispatchers)
        val authenticationScope = mockk<AuthenticationScope>()
        val setLastDevice = mockk<SetLastDeviceIdUseCase>()
        val restore = mockk<RestoreCryptoStateUseCase>()
        val logout = mockk<com.wire.kalium.logic.feature.auth.LogoutUseCase>()
        val deleteSession = mockk<DeleteSessionUseCase>()
        val validSession = mockk<DoesValidSessionExistUseCase>()
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
