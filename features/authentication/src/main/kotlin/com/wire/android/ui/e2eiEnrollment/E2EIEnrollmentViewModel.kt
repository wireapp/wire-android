/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */
package com.wire.android.ui.e2eiEnrollment

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

data class E2EIEnrollmentState(
    val certificate: String = "",
    val isLoading: Boolean = false,
    val isFinalizing: Boolean = false,
    val isCertificateEnrollError: Boolean = false,
    val isCertificateEnrollSuccess: Boolean = false,
    val startGettingE2EICertificate: Boolean = false,
)

/** Feature state machine. OAuth, dialogs and Kalium remain host concerns. */
class E2EIEnrollmentViewModel(
    private val gateway: E2EIEnrollmentGateway,
) : ViewModel() {
    var state by mutableStateOf(E2EIEnrollmentState())
        private set

    fun finalizeMLSClient(onComplete: () -> Unit) {
        state = state.copy(isFinalizing = true)
        viewModelScope.launch {
            gateway.finalizeEnrollment()
            state = state.copy(isFinalizing = false)
            onComplete()
        }
    }

    fun enrollE2EICertificate() {
        state = state.copy(isLoading = true, startGettingE2EICertificate = true)
    }

    fun handleE2EIEnrollmentResult(result: E2EIEnrollmentResult) {
        state = when (result) {
            E2EIEnrollmentResult.Failure -> state.copy(
                isLoading = false,
                isCertificateEnrollError = true,
                startGettingE2EICertificate = false,
            )
            is E2EIEnrollmentResult.Success -> state.copy(
                certificate = result.certificate,
                isCertificateEnrollSuccess = true,
                isCertificateEnrollError = false,
                isLoading = false,
                startGettingE2EICertificate = false,
            )
        }
    }

    fun dismissErrorDialog() {
        state = state.copy(isCertificateEnrollError = false)
    }
}
