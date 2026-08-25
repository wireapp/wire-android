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

package com.wire.android.navigation.navigation3

import com.wire.navigation.WireSessionId
import com.wire.navigation.WireViewModelOwner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WireViewModelOwnershipTrackerTest {

    @Test
    fun givenTwoEntriesShareFlow_whenFirstEntryIsPopped_thenOwnerIsRetained() {
        val released = mutableListOf<String>()
        val tracker = WireViewModelOwnershipTracker(released::add)
        val owner = WireViewModelOwner.Flow("authentication")
        tracker.registerEntry("email", setOf(owner))
        tracker.registerEntry("password", setOf(owner))

        tracker.onEntryPopped("email")

        assertEquals(emptyList<String>(), released)
    }

    @Test
    fun givenLastFlowEntryIsPopped_whenTrackingOwnership_thenFlowOwnerIsReleased() {
        val released = mutableListOf<String>()
        val tracker = WireViewModelOwnershipTracker(released::add)
        val owner = WireViewModelOwner.Flow("authentication")
        tracker.registerEntry("email", setOf(owner))
        tracker.registerEntry("password", setOf(owner))

        tracker.onEntryPopped("email")
        tracker.onEntryPopped("password")

        assertEquals(listOf("flow:authentication"), released)
    }

    @Test
    fun givenEntryChangesFlowOwner_whenRegisteredAgain_thenUnreferencedOldOwnerIsReleased() {
        val released = mutableListOf<String>()
        val tracker = WireViewModelOwnershipTracker(released::add)
        tracker.registerEntry("entry", setOf(WireViewModelOwner.Flow("old")))

        tracker.registerEntry("entry", setOf(WireViewModelOwner.Flow("new")))

        assertEquals(listOf("flow:old"), released)
    }

    @Test
    fun givenSessionEntryIsPopped_whenTrackingOwnership_thenSessionOwnerRemainsRetained() {
        val released = mutableListOf<String>()
        val tracker = WireViewModelOwnershipTracker(released::add)
        tracker.registerEntry(
            contentKey = "conversation",
            owners = setOf(
                WireViewModelOwner.Session(WireSessionId("user", "wire.example"))
            ),
        )

        tracker.onEntryPopped("conversation")

        assertEquals(emptyList<String>(), released)
    }

    @Test
    fun givenApplicationEntryIsPopped_whenTrackingOwnership_thenApplicationOwnerRemainsRetained() {
        val released = mutableListOf<String>()
        val tracker = WireViewModelOwnershipTracker(released::add)
        tracker.registerEntry("coordinator", setOf(WireViewModelOwner.Application))

        tracker.onEntryPopped("coordinator")

        assertEquals(emptyList<String>(), released)
    }

    @Test
    fun givenRouteHasMultipleSharedOwners_whenPopped_thenOnlyFlowOwnerIsReleasedAutomatically() {
        val released = mutableListOf<String>()
        val tracker = WireViewModelOwnershipTracker(released::add)
        tracker.registerEntry(
            contentKey = "login",
            owners = setOf(
                WireViewModelOwner.Flow("login"),
                WireViewModelOwner.Session(WireSessionId("user", "wire.example")),
                WireViewModelOwner.Application,
            ),
        )

        tracker.onEntryPopped("login")

        assertEquals(listOf("flow:login"), released)
    }
}
