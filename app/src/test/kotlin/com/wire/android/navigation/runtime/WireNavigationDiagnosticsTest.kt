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
 */

package com.wire.android.navigation.runtime

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class WireNavigationDiagnosticsTest {

    @Test
    fun givenViewModelKeyContainsInstanceData_whenRedacting_thenOnlyTypeIsExposed() {
        val key = "com.wire.ConversationViewModel:conversation-id@backend.example"

        assertEquals("com.wire.ConversationViewModel", key.redactedViewModelKey())
    }

    @Test
    fun givenOneActiveOwnerKey_whenAnotherStoreClaimsIt_thenInvariantFails() {
        val firstOwner = TestOwner()
        val conflictingOwner = TestOwner()
        val ownerKey = "flow:diagnostics-collision-test"

        try {
            WireNavigationDiagnostics.ownerAvailable(firstOwner, ownerKey)

            assertThrows(IllegalStateException::class.java) {
                WireNavigationDiagnostics.ownerAvailable(conflictingOwner, ownerKey)
            }
        } finally {
            WireNavigationDiagnostics.ownerReleased(firstOwner, ownerKey)
            WireNavigationDiagnostics.ownerCleared(ownerKey)
        }
    }

    @Test
    fun givenOneOwnerInstance_whenItClaimsAnotherKey_thenInvariantFails() {
        val owner = TestOwner()
        val firstKey = "flow:diagnostics-first-key"

        try {
            WireNavigationDiagnostics.ownerAvailable(owner, firstKey)

            assertThrows(IllegalStateException::class.java) {
                WireNavigationDiagnostics.ownerAvailable(owner, "flow:diagnostics-second-key")
            }
        } finally {
            WireNavigationDiagnostics.ownerReleased(owner, firstKey)
            WireNavigationDiagnostics.ownerCleared(firstKey)
        }
    }

    private class TestOwner : ViewModelStoreOwner {
        override val viewModelStore = ViewModelStore()
    }
}
