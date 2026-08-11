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
package com.wire.android.session

import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.PrepareUserSessionResult
import com.wire.kalium.logic.UserSessionPreparationFailure
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.UserSessionScope
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UserSessionPreparationGateTest {

    @Test
    fun givenKaliumReturnsReadySession_whenPreparing_thenReturnedScopeIsReused() = runTest {
        val coreLogic = mockk<CoreLogic>()
        val sessionScope = mockk<UserSessionScope>()
        coEvery { coreLogic.prepareUserSession(USER_ID) } returns success(sessionScope)

        val result = UserSessionPreparationGate(coreLogic).prepare(USER_ID)

        assertSame(sessionScope, (result as AppUserSessionPreparationResult.Ready).sessionScope)
    }

    @Test
    fun givenConcurrentAppCallers_whenPreparing_thenBothAwaitKaliumSharedResult() = runTest {
        val coreLogic = mockk<CoreLogic>()
        val sessionScope = mockk<UserSessionScope>()
        val kaliumResult = CompletableDeferred<PrepareUserSessionResult>()
        coEvery { coreLogic.prepareUserSession(USER_ID) } coAnswers { kaliumResult.await() }
        val gate = UserSessionPreparationGate(coreLogic)

        val foreground = async { gate.prepare(USER_ID) }
        val background = async { gate.prepare(USER_ID) }
        runCurrent()
        coVerify(exactly = 2) { coreLogic.prepareUserSession(USER_ID) }

        kaliumResult.complete(success(sessionScope))

        assertSame(
            sessionScope,
            (foreground.await() as AppUserSessionPreparationResult.Ready).sessionScope,
        )
        assertSame(
            sessionScope,
            (background.await() as AppUserSessionPreparationResult.Ready).sessionScope,
        )
    }

    @Test
    fun givenPublicPreparationFailures_whenMapping_thenRetryabilityIsExplicit() {
        val failures = listOf(
            UserSessionPreparationFailure.InsufficientStorage to true,
            UserSessionPreparationFailure.TemporarilyUnavailable to true,
            UserSessionPreparationFailure.ApplicationUpdateRequired to false,
            UserSessionPreparationFailure.SupportRequired to false,
        )

        failures.forEach { (reason, expectedCanRetry) ->
            val result = failure(reason).toAppResult() as AppUserSessionPreparationResult.Failed
            assertEquals(reason, result.reason)
            if (expectedCanRetry) assertTrue(result.canRetry) else assertFalse(result.canRetry)
        }
    }

    private fun success(sessionScope: UserSessionScope): PrepareUserSessionResult.Success =
        mockk<PrepareUserSessionResult.Success>().also {
            every { it.sessionScope } returns sessionScope
        }

    private fun failure(reason: UserSessionPreparationFailure): PrepareUserSessionResult.Failure =
        mockk<PrepareUserSessionResult.Failure>().also {
            every { it.reason } returns reason
        }

    private companion object {
        val USER_ID = UserId("user", "wire.test")
    }
}
