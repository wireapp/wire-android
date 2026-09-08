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
