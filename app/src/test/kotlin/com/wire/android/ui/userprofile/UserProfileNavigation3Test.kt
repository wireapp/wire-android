/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.userprofile

import com.wire.kalium.logic.data.id.QualifiedID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class UserProfileNavigation3Test {

    @Test
    fun givenLegacyQualifiedId_whenMappingToNavigationAndBack_thenValueIsUnchanged() {
        val legacy = QualifiedID("user", "wire.example")

        assertEquals(legacy, legacy.toUserProfileQualifiedId().toQualifiedId())
    }

    @Test
    fun givenBlankQualifiedIdPart_whenCreatingNavigationId_thenItIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            UserProfileQualifiedId("", "wire.example")
        }
        assertThrows(IllegalArgumentException::class.java) {
            UserProfileQualifiedId("user", "")
        }
    }
}
