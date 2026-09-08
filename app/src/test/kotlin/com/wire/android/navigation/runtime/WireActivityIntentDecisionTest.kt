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

package com.wire.android.navigation.runtime

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WireActivityIntentDecisionTest {

    @Test
    fun givenEligibleIntent_whenDecidingHandling_thenNonDeepLinkAndDeepLinkHandlersAreAllowed() {
        val result = decideWireActivityIntentHandling(WireActivityIntentEvidence())

        assertEquals(WireActivityIntentHandling.NON_DEEP_LINK_THEN_DEEP_LINK, result)
    }

    @Test
    fun givenAnyDeepLinkExclusion_whenDecidingHandling_thenOnlyNonDeepLinkHandlerIsAllowed() {
        val exclusions = listOf(
            WireActivityIntentEvidence(isMissing = true),
            WireActivityIntentEvidence(isLauncherIntent = true),
            WireActivityIntentEvidence(isLaunchedFromHistory = true),
            WireActivityIntentEvidence(isRestoredOriginalIntent = true),
            WireActivityIntentEvidence(wasDeepLinkHandled = true),
        )

        exclusions.forEach { evidence ->
            assertEquals(
                WireActivityIntentHandling.NON_DEEP_LINK_ONLY,
                decideWireActivityIntentHandling(evidence),
                "Expected exclusion for $evidence",
            )
        }
    }
}
