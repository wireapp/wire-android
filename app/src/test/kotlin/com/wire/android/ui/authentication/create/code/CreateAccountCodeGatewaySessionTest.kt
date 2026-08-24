package com.wire.android.ui.authentication.create.code

import com.wire.android.framework.TestUser
import com.wire.android.util.WillNeverOccurError
import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.logic.data.session.StoreSessionParam
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.client.RegisterClientParam
import com.wire.kalium.logic.feature.client.RegisterClientResult
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class CreateAccountCodeGatewaySessionTest {
    @Test fun `store uses exact session parameter replace false and maps all failures`() = runTest {
        val a = createAccountCodeArrangement(defaultWebSocket = true); val credentials = credentials(); val parameter = slot<StoreSessionParam>()
        coEvery { a.addAuthenticatedUser(capture(parameter), replace = false) } returns AddAuthenticatedUserUseCase.Result.Success(TestUser.SELF_USER_ID)
        assertEquals(StoreAccountSessionResult.Success(TestUser.SELF_USER_ID), a.gateway.storeSession(credentials))
        assertSame(credentials.result.authData, parameter.captured.accountTokens); assertEquals(credentials.result.ssoID, parameter.captured.ssoId)
        assertEquals("server", parameter.captured.serverConfigId); assertEquals(true, parameter.captured.isPersistentWebSocketEnabled)
        val failure = NetworkFailure.NoNetworkConnection(null)
        listOf(AddAuthenticatedUserUseCase.Result.Failure.UserAlreadyExists, AddAuthenticatedUserUseCase.Result.Failure.SsoIdentityChanged, AddAuthenticatedUserUseCase.Result.Failure.NomadSingleUserViolation).forEach { kalium ->
            coEvery { a.addAuthenticatedUser(any(), replace = false) } returns kalium
            assertEquals(StoreAccountSessionResult.UserAlreadyExists, a.gateway.storeSession(credentials))
        }
        coEvery { a.addAuthenticatedUser(any(), replace = false) } returns AddAuthenticatedUserUseCase.Result.Failure.Generic(failure)
        assertSame(failure, (a.gateway.storeSession(credentials) as StoreAccountSessionResult.Generic).failure)
    }

    @Test fun `client uses exact password model postfix maps results and preserves impossible exceptions`() = runTest {
        val a = createAccountCodeArrangement(buildFlags = CreateAccountCodeBuildFlags(true, "internal", "debug")); val parameter = slot<RegisterClientParam>()
        coEvery { a.getOrRegister(capture(parameter)) } returns RegisterClientResult.Failure.TooManyClients
        assertEquals(CreateAccountClientResult.TooManyDevices, a.gateway.registerClient(TestUser.SELF_USER_ID, "secret"))
        assertEquals("secret", parameter.captured.password); assertEquals(" [internal_debug]", parameter.captured.modelPostfix); assertNull(parameter.captured.capabilities)
        coEvery { a.getOrRegister(any()) } returns mockk<RegisterClientResult.Success>()
        assertEquals(CreateAccountClientResult.Success, a.gateway.registerClient(TestUser.SELF_USER_ID, "secret"))
        coEvery { a.getOrRegister(any()) } returns mockk<RegisterClientResult.E2EICertificateRequired>()
        assertEquals(CreateAccountClientResult.E2EICertificateRequired, a.gateway.registerClient(TestUser.SELF_USER_ID, "secret"))
        val failure = NetworkFailure.NoNetworkConnection(null); coEvery { a.getOrRegister(any()) } returns RegisterClientResult.Failure.Generic(failure)
        assertSame(failure, (a.gateway.registerClient(TestUser.SELF_USER_ID, "secret") as CreateAccountClientResult.Generic).failure)
        listOf(RegisterClientResult.Failure.InvalidCredentials.InvalidPassword to "RegisterClient: wrong password when register client after creating a new account", RegisterClientResult.Failure.PasswordAuthRequired to "RegisterClient: password required to register client after creating new account with email").forEach { (kalium, message) ->
            coEvery { a.getOrRegister(any()) } returns kalium
            val thrown = try { a.gateway.registerClient(TestUser.SELF_USER_ID, "secret"); null } catch (error: WillNeverOccurError) { error }
            assertEquals(message, thrown?.message)
        }
        val public = createAccountCodeArrangement(buildFlags = CreateAccountCodeBuildFlags(false, "prod", "release")); val publicParameter = slot<RegisterClientParam>()
        coEvery { public.getOrRegister(capture(publicParameter)) } returns RegisterClientResult.Failure.TooManyClients
        public.gateway.registerClient(TestUser.SELF_USER_ID, "secret"); assertNull(publicParameter.captured.modelPostfix)
    }
}
