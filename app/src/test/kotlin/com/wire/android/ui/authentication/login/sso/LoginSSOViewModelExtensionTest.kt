/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.common.error.StorageFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.auth.AccountTokens
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.session.StoreSessionParam
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.LogoutUseCase
import com.wire.kalium.logic.feature.session.DeleteSessionUseCase
import com.wire.kalium.logic.feature.auth.AuthenticationScope
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.auth.sso.GetSSOLoginSessionUseCase
import com.wire.kalium.logic.feature.auth.sso.SSOLoginSessionResult
import io.mockk.coVerify
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LoginSSOViewModelExtensionTest {

    @Test
    fun `email SSO bypasses capability and preserves its existing IdP`() = runTest {
        val arrangement = EstablishArrangement(enabled = false)
        arrangement.establish(PendingSsoLogin("canonical-idp", "server", false), emailIdp = "email-idp")

        coVerify(exactly = 1) { arrangement.getLoginSession("cookie", false) }
        coVerify(exactly = 1) {
            arrangement.addAuthenticatedUser(match { it.ssoIdentityProviderId == "email-idp" }, false)
        }
    }

    @Test
    fun `code SSO supplies IdP only when capability is enabled`() = runTest {
        for (enabled in listOf(true, false)) {
            val arrangement = EstablishArrangement(enabled)
            arrangement.establish(PendingSsoLogin("code-idp", "server", true))

            coVerify(exactly = 1) { arrangement.getLoginSession("cookie", true) }
            coVerify(exactly = 1) {
                arrangement.addAuthenticatedUser(
                    match { it.ssoIdentityProviderId == if (enabled) "code-idp" else null },
                    false
                )
            }
        }
    }

    @Test
    fun `settings failure stops before storing or opening a session`() = runTest {
        val arrangement = EstablishArrangement(true)
        coEvery { arrangement.getLoginSession("cookie", true) } returns
            SSOLoginSessionResult.Failure.Generic(CoreFailure.Unknown(IllegalStateException("settings unavailable")))

        arrangement.establish(PendingSsoLogin("idp", "server", true))

        assertTrue(arrangement.failed)
        coVerify(exactly = 0) { arrangement.addAuthenticatedUser(any(), any()) }
    }

    @Test
    fun `different backend context cannot complete login`() = runTest {
        val arrangement = EstablishArrangement(true)
        arrangement.establish(PendingSsoLogin("idp", "other-server", true))

        assertTrue(arrangement.failed)
        coVerify(exactly = 0) { arrangement.getLoginSession(any(), any()) }
        coVerify(exactly = 0) { arrangement.addAuthenticatedUser(any(), any()) }
    }

    private class EstablishArrangement(enabled: Boolean) {
        val addAuthenticatedUser = mockk<AddAuthenticatedUserUseCase>()
        val getLoginSession = mockk<GetSSOLoginSessionUseCase>()
        private val authScope = mockk<AuthenticationScope>()
        private val coreLogic = mockk<CoreLogic>()
        private val userId = UserId("user", "domain")
        private val extension = LoginSSOViewModelExtension(addAuthenticatedUser, coreLogic, false)
        var failed = false

        init {
            coEvery { coreLogic.authenticationScopeForConfigId("server") } returns
                AutoVersionAuthScopeUseCase.Result.Success(authScope)
            every { authScope.ssoLoginScope.getLoginSession } returns getLoginSession
            coEvery { getLoginSession(any(), any()) } returns SSOLoginSessionResult.Success(
                AccountTokens(userId, "access", "refresh", "Bearer", null), null, null, null, enabled
            )
            coEvery { addAuthenticatedUser(any(), false) } returns AddAuthenticatedUserUseCase.Result.Success(userId)
        }

        suspend fun establish(pending: PendingSsoLogin, emailIdp: String? = null) {
            extension.establishSSOSession(
                cookie = "cookie",
                serverConfigId = "server",
                pendingSsoLogin = pending,
                ssoIdentityProviderId = emailIdp,
                onAuthScopeFailure = { error("Unexpected auth scope failure") },
                onSSOLoginFailure = { failed = true },
                onAddAuthenticatedUserFailure = { error("Unexpected storage failure") },
                onSuccess = {},
            )
        }
    }

    @Test
    fun `given retained SSO session, when replacement is confirmed, then wipe data before storing new session`() = runTest {
        val (arrangement, extension) = Arrangement().arrange()

        val actual = extension.replaceRetainedSsoSession(arrangement.session)

        assertEquals(ReplaceRetainedSsoSessionResult.Success(arrangement.userId), actual)
        coVerifyOrder {
            arrangement.logout(LogoutReason.SELF_HARD_LOGOUT, true)
            arrangement.deleteSession(arrangement.userId)
            arrangement.addAuthenticatedUser(arrangement.session, false)
        }
    }

    @Test
    fun `given session deletion fails, when replacement is confirmed, then return typed failure`() = runTest {
        val failure = StorageFailure.DataNotFound
        val (arrangement, extension) = Arrangement()
            .withDeleteSessionResult(DeleteSessionUseCase.Result.Failure(failure))
            .arrange()

        val actual = extension.replaceRetainedSsoSession(arrangement.session)

        assertEquals(
            ReplaceRetainedSsoSessionResult.Failure(
                AddAuthenticatedUserUseCase.Result.Failure.Generic(failure)
            ),
            actual
        )
    }

    @Test
    fun `given replacement throws, when replacement is confirmed, then return typed failure`() = runTest {
        val exception = IllegalStateException("Session scope unavailable")
        val (arrangement, extension) = Arrangement()
            .withLogoutFailure(exception)
            .arrange()

        val actual = extension.replaceRetainedSsoSession(arrangement.session)

        assertEquals(
            ReplaceRetainedSsoSessionResult.Failure(
                AddAuthenticatedUserUseCase.Result.Failure.Generic(CoreFailure.Unknown(exception))
            ),
            actual
        )
    }

    @Test
    fun `given replacement is cancelled, when replacement is confirmed, then propagate cancellation`() = runTest {
        val (arrangement, extension) = Arrangement()
            .withLogoutFailure(CancellationException("Cancelled"))
            .arrange()

        var cancellationPropagated = false
        try {
            extension.replaceRetainedSsoSession(arrangement.session)
        } catch (_: CancellationException) {
            cancellationPropagated = true
        }

        assertTrue(cancellationPropagated)
    }

    private class Arrangement {
        val userId = UserId("user-id", "domain")
        val session = StoreSessionParam(
            serverConfigId = "server-config-id",
            ssoId = null,
            accountTokens = AccountTokens(userId, "access", "refresh", "Bearer", null),
            proxyCredentials = null,
            isPersistentWebSocketEnabled = false,
        )
        val addAuthenticatedUser = mockk<AddAuthenticatedUserUseCase>()
        val coreLogic = mockk<CoreLogic>()
        val logout = mockk<LogoutUseCase>()
        val deleteSession = mockk<DeleteSessionUseCase>()

        init {
            every { coreLogic.getSessionScope(userId).logout } returns logout
            every { coreLogic.getGlobalScope().deleteSession } returns deleteSession
            coEvery { logout(LogoutReason.SELF_HARD_LOGOUT, true) } returns Unit
            coEvery { deleteSession(userId) } returns DeleteSessionUseCase.Result.Success
            coEvery {
                addAuthenticatedUser(session, false)
            } returns AddAuthenticatedUserUseCase.Result.Success(userId)
        }

        fun withDeleteSessionResult(result: DeleteSessionUseCase.Result) = apply {
            coEvery { deleteSession(userId) } returns result
        }

        fun withLogoutFailure(exception: Exception) = apply {
            coEvery { logout(LogoutReason.SELF_HARD_LOGOUT, true) } throws exception
        }

        fun arrange() = this to LoginSSOViewModelExtension(addAuthenticatedUser, coreLogic, false)
    }
}
