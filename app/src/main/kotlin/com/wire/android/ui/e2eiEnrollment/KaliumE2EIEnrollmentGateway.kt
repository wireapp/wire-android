/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */
package com.wire.android.ui.e2eiEnrollment

import com.wire.kalium.logic.feature.client.FinalizeMLSClientAfterE2EIEnrollmentUseCase
import dev.zacsweers.metro.Inject

/** App adapter: Kalium is deliberately not visible to the feature state machine. */
class KaliumE2EIEnrollmentGateway @Inject constructor(
    private val finalizeMLSClientAfterE2EIEnrollment: FinalizeMLSClientAfterE2EIEnrollmentUseCase,
) : E2EIEnrollmentGateway {
    override suspend fun finalizeEnrollment() {
        finalizeMLSClientAfterE2EIEnrollment.invoke()
    }
}
