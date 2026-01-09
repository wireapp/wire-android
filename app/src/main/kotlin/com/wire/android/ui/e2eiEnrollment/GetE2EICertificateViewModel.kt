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

import androidx.lifecycle.ViewModel
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.feature.e2ei.OAuthUseCase
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.feature.e2ei.usecase.E2EIEnrollmentResult
import com.wire.kalium.logic.feature.e2ei.usecase.FinalizeEnrollmentResult
import com.wire.kalium.logic.feature.e2ei.usecase.InitialEnrollmentResult
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GetE2EICertificateViewModel @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val currentSession: CurrentSessionUseCase,
    val dispatcherProvider: DispatcherProvider
) : ViewModel() {

    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.default())

    val requestOAuthFlow = MutableSharedFlow<E2EIEnrollmentResult.Initialized>()
    val enrollmentResultFlow = MutableSharedFlow<FinalizeEnrollmentResult>()

    fun handleOAuthResult(oAuthResult: OAuthUseCase.OAuthResult, initialEnrollmentResult: E2EIEnrollmentResult.Initialized) {
        scope.launch {
            when (oAuthResult) {
                is OAuthUseCase.OAuthResult.Success -> finalizeEnrollment(oAuthResult, initialEnrollmentResult)

                is OAuthUseCase.OAuthResult.Failed -> enrollmentResultFlow.emit(
                    FinalizeEnrollmentResult.Failure.OAuthError(oAuthResult.reason)
                )
            }
        }
    }

    fun getCertificate(isNewClient: Boolean) {
        scope.launch {
            val currentSessionResult = currentSession()
            if (currentSessionResult is CurrentSessionResult.Success && currentSessionResult.accountInfo.isValid()) {
                when (val result = coreLogic.getSessionScope(currentSessionResult.accountInfo.userId)
                    .users
                    .enrollE2EI
                    .initialEnrollment(isNewClientRegistration = isNewClient)) {
                    is InitialEnrollmentResult.Failure -> {
                        enrollmentResultFlow.emit(FinalizeEnrollmentResult.Failure.Generic(result.toE2EIFailure()))
                    }
                    is InitialEnrollmentResult.Success -> {
                        requestOAuthFlow.emit(result.initializationResult)
                    }
                }
            }
        }
    }

    private suspend fun finalizeEnrollment(
        oAuthResult: OAuthUseCase.OAuthResult.Success,
        initialEnrollmentResult: E2EIEnrollmentResult.Initialized
    ) {
        val currentSessionResult = currentSession()

        if (currentSessionResult is CurrentSessionResult.Success && currentSessionResult.accountInfo.isValid()) {
            val enrollmentResult = coreLogic.getSessionScope(currentSessionResult.accountInfo.userId)
                .users
                .enrollE2EI.finalizeEnrollment(
                    oAuthResult.idToken,
                    oAuthResult.authState,
                    initialEnrollmentResult
                )
            enrollmentResultFlow.emit(enrollmentResult)
        }
    }

    private fun InitialEnrollmentResult.Failure.toE2EIFailure() = when (this) {
        is InitialEnrollmentResult.Failure.E2EIDisabled -> com.wire.kalium.common.error.E2EIFailure.Disabled
        is InitialEnrollmentResult.Failure.MissingTeamSettings -> com.wire.kalium.common.error.E2EIFailure.MissingTeamSettings
        is InitialEnrollmentResult.Failure.Generic -> this.e2EIFailure
    }
}
