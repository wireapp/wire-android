/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */
package com.wire.android.ui.e2eiEnrollment

/** Host boundary for the final, platform-specific MLS operation. */
fun interface E2EIEnrollmentGateway {
    suspend fun finalizeEnrollment()
}

/** Result produced by the host-owned OAuth/certificate flow. */
sealed interface E2EIEnrollmentResult {
    data class Success(val certificate: String) : E2EIEnrollmentResult
    data object Failure : E2EIEnrollmentResult
}
