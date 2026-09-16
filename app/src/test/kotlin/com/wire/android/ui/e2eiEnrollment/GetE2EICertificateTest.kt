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

package com.wire.android.ui.e2eiEnrollment

import com.wire.android.feature.e2ei.OAuthUseCase
import com.wire.kalium.logic.data.e2ei.E2EIAuthenticationRequest
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetE2EICertificateTest {

    @Test
    fun givenEnrollmentStartsBeforeOAuthCollector_whenCollectorStarts_thenRequestIsDelivered() = runTest {
        val coordinator = E2EIOAuthCoordinator()
        val authentication = async(start = CoroutineStart.UNDISPATCHED) {
            coordinator.authenticate(AUTHENTICATION_REQUEST)
        }

        val request = coordinator.requestFlow.first()
        coordinator.handleResult(request.id, OAuthUseCase.OAuthResult.Success(FIRST_ID_TOKEN, AUTH_STATE))

        assertEquals(FIRST_ID_TOKEN, authentication.await())
    }

    @Test
    fun givenConcurrentOAuthRequests_whenResultsArriveOutOfOrder_thenEachEnrollmentReceivesItsOwnToken() = runTest {
        val coordinator = E2EIOAuthCoordinator()
        val firstRequest = async(start = CoroutineStart.UNDISPATCHED) { coordinator.requestFlow.first() }
        val firstAuthentication = async(start = CoroutineStart.UNDISPATCHED) {
            coordinator.authenticate(AUTHENTICATION_REQUEST)
        }
        val emittedFirstRequest = firstRequest.await()

        val secondRequest = async(start = CoroutineStart.UNDISPATCHED) { coordinator.requestFlow.first() }
        val secondAuthentication = async(start = CoroutineStart.UNDISPATCHED) {
            coordinator.authenticate(AUTHENTICATION_REQUEST)
        }
        val emittedSecondRequest = secondRequest.await()

        coordinator.handleResult(
            emittedSecondRequest.id,
            OAuthUseCase.OAuthResult.Success(SECOND_ID_TOKEN, AUTH_STATE),
        )
        coordinator.handleResult(
            emittedFirstRequest.id,
            OAuthUseCase.OAuthResult.Success(FIRST_ID_TOKEN, AUTH_STATE),
        )

        assertEquals(FIRST_ID_TOKEN, firstAuthentication.await())
        assertEquals(SECOND_ID_TOKEN, secondAuthentication.await())
    }

    @Test
    fun givenOAuthFailure_whenAuthenticating_thenEnrollmentReceivesTheFailure() = runTest {
        val coordinator = E2EIOAuthCoordinator()
        val request = async(start = CoroutineStart.UNDISPATCHED) { coordinator.requestFlow.first() }
        val authentication = async(start = CoroutineStart.UNDISPATCHED) {
            runCatching { coordinator.authenticate(AUTHENTICATION_REQUEST) }
        }

        coordinator.handleResult(request.await().id, OAuthUseCase.OAuthResult.Failed(FAILURE_REASON))

        val exception = authentication.await().exceptionOrNull()
        assertTrue(exception is E2EIAuthenticationException)
        assertEquals(FAILURE_REASON, exception?.message)
    }

    @Test
    fun givenAuthenticationRequest_whenCreatingOAuthClaims_thenRequiredClaimsArePreserved() {
        val expected = Json.parseToJsonElement(
            """{"id_token":{"keyauth":{"essential":true,"value":"key-auth"},"acme_aud":{"essential":true,"value":"audience"}}}"""
        )

        assertEquals(expected, AUTHENTICATION_REQUEST.toOAuthClaims())
    }

    private companion object {
        val AUTHENTICATION_REQUEST = E2EIAuthenticationRequest(
            target = "https://example.com/authorize",
            keyAuth = "key-auth",
            acmeAudience = "audience",
        )
        const val FIRST_ID_TOKEN = "first-id-token"
        const val SECOND_ID_TOKEN = "second-id-token"
        const val AUTH_STATE = "auth-state"
        const val FAILURE_REASON = "OAuth failed"
    }
}
