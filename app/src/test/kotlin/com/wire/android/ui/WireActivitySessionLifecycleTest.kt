/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WireActivitySessionLifecycleTest {

    @Test
    fun givenSessionErrorBlocksTheUi_whenCheckingSessionGraphInvalidation_thenGraphIsInvalidated() {
        assertEquals(
            true,
            shouldInvalidateWireActivitySessionGraph(
                isUserUiBlocked = true,
                sessionTransitionReason = null,
            ),
        )
    }

    @Test
    fun givenSelfLogoutIsInProgress_whenCheckingSessionGraphInvalidation_thenGraphIsInvalidated() {
        assertEquals(
            true,
            shouldInvalidateWireActivitySessionGraph(
                isUserUiBlocked = false,
                sessionTransitionReason = SessionTransitionReason.SELF_LOGOUT,
            ),
        )
    }

    @Test
    fun givenSessionIsActive_whenCheckingSessionGraphInvalidation_thenGraphIsRetained() {
        assertEquals(
            false,
            shouldInvalidateWireActivitySessionGraph(
                isUserUiBlocked = false,
                sessionTransitionReason = null,
            ),
        )
    }
}
