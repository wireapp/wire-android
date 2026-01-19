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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.kalium.logic.feature.client.FinalizeMLSClientAfterE2EIEnrollment
import com.wire.kalium.logic.feature.e2ei.usecase.FinalizeEnrollmentResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class E2EIEnrollmentState(
    val certificate: String = "null",
    val showCertificate: Boolean = false,
    val isLoading: Boolean = false,
    val isCertificateEnrollError: Boolean = false,
    val isCertificateEnrollSuccess: Boolean = false,
    val startGettingE2EICertificate: Boolean = false
)

@HiltViewModel
class E2EIEnrollmentViewModel @Inject constructor(
    private val finalizeMLSClientAfterE2EIEnrollment: FinalizeMLSClientAfterE2EIEnrollment,
) : ViewModel() {
    var state by mutableStateOf(E2EIEnrollmentState())

    fun finalizeMLSClient() {
        viewModelScope.launch {
            finalizeMLSClientAfterE2EIEnrollment.invoke()
        }
    }

    fun enrollE2EICertificate() {
        state = state.copy(isLoading = true, startGettingE2EICertificate = true)
    }

    fun handleE2EIEnrollmentResult(result: FinalizeEnrollmentResult) {
        state = when (result) {
            is FinalizeEnrollmentResult.Failure -> {
                state.copy(
                    isLoading = false,
                    isCertificateEnrollError = true,
                    startGettingE2EICertificate = false
                )
            }
            is FinalizeEnrollmentResult.Success -> {
                state.copy(
                    certificate = result.certificate,
                    isCertificateEnrollSuccess = true,
                    isCertificateEnrollError = false,
                    isLoading = false,
                    startGettingE2EICertificate = false
                )
            }
        }
    }

    fun dismissErrorDialog() {
        state = state.copy(
            isCertificateEnrollError = false,
        )
    }
}
