/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

import com.wire.android.di.KaliumCoreLogic
import dev.zacsweers.metro.Inject

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.feature.e2ei.OAuthUseCase
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.e2ei.E2EIAuthenticationRequest
import com.wire.kalium.logic.feature.e2ei.usecase.EnrollE2EIResult
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

internal data class E2EIOAuthRequest(
    val id: Long,
    val authenticationRequest: E2EIAuthenticationRequest,
)

internal class E2EIOAuthCoordinator {
    private val nextRequestId = AtomicLong()
    private val pendingRequests = ConcurrentHashMap<Long, CompletableDeferred<OAuthUseCase.OAuthResult>>()
    private val requestChannel = Channel<E2EIOAuthRequest>(Channel.BUFFERED)

    val requestFlow = requestChannel.receiveAsFlow()

    suspend fun authenticate(request: E2EIAuthenticationRequest): String {
        val requestId = nextRequestId.incrementAndGet()
        val result = CompletableDeferred<OAuthUseCase.OAuthResult>()
        pendingRequests[requestId] = result
        return try {
            requestChannel.send(E2EIOAuthRequest(requestId, request))
            when (val oAuthResult = result.await()) {
                is OAuthUseCase.OAuthResult.Success -> oAuthResult.idToken
                is OAuthUseCase.OAuthResult.Failed -> throw E2EIAuthenticationException(oAuthResult.reason)
            }
        } finally {
            pendingRequests.remove(requestId)?.cancel()
        }
    }

    fun handleResult(requestId: Long, oAuthResult: OAuthUseCase.OAuthResult) {
        pendingRequests[requestId]?.complete(oAuthResult)
    }
}

internal class E2EIAuthenticationException(reason: String) : Exception(reason)

class GetE2EICertificateViewModel @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val currentSession: CurrentSessionUseCase,
    val dispatcherProvider: DispatcherProvider
) : ViewModel() {

    private val oAuthCoordinator = E2EIOAuthCoordinator()

    internal val requestOAuthFlow = oAuthCoordinator.requestFlow
    val enrollmentResultFlow = MutableSharedFlow<EnrollE2EIResult>()

    internal fun handleOAuthResult(requestId: Long, oAuthResult: OAuthUseCase.OAuthResult) {
        oAuthCoordinator.handleResult(requestId, oAuthResult)
    }

    fun getCertificate(isNewClient: Boolean) {
        viewModelScope.launch(dispatcherProvider.default()) {
            val currentSessionResult = currentSession()
            if (currentSessionResult is CurrentSessionResult.Success && currentSessionResult.accountInfo.isValid()) {
                val result = coreLogic.getSessionScope(currentSessionResult.accountInfo.userId)
                    .users
                    .enrollE2EI
                    .invoke(
                        isNewClientRegistration = isNewClient,
                        authenticate = oAuthCoordinator::authenticate,
                    )
                enrollmentResultFlow.emit(result)
            }
        }
    }
}
