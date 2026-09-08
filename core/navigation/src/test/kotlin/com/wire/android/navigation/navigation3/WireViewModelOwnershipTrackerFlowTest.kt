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

import androidx.lifecycle.viewmodel.ViewModelStoreProvider
import com.wire.navigation.WireViewModelOwner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class WireViewModelOwnershipTrackerFlowTest {

    @Test
    fun givenTwoEntriesInFlow_whenFirstEntryIsPopped_thenFlowOwnerIsRetained() {
        val releasedFlows = mutableListOf<String>()
        val tracker = WireViewModelOwnershipTracker {
            releasedFlows += it.removePrefix("flow:")
        }
        val owner = WireViewModelOwner.Flow("new-conversation")
        tracker.registerEntry("first", setOf(owner))
        tracker.registerEntry("second", setOf(owner))

        tracker.onEntryPopped("first")

        assertEquals(emptyList<String>(), releasedFlows)
    }

    @Test
    fun givenLastEntryInFlow_whenEntryIsPopped_thenFlowOwnerIsReleased() {
        val releasedFlows = mutableListOf<String>()
        val tracker = WireViewModelOwnershipTracker {
            releasedFlows += it.removePrefix("flow:")
        }
        val owner = WireViewModelOwner.Flow("new-conversation")
        tracker.registerEntry("first", setOf(owner))
        tracker.registerEntry("second", setOf(owner))

        tracker.onEntryPopped("first")
        tracker.onEntryPopped("second")

        assertEquals(listOf("new-conversation"), releasedFlows)
    }

    @Test
    fun givenEntriesInDifferentFlows_whenOneFlowEnds_thenOnlyItsOwnerIsReleased() {
        val releasedFlows = mutableListOf<String>()
        val tracker = WireViewModelOwnershipTracker {
            releasedFlows += it.removePrefix("flow:")
        }
        tracker.registerEntry(
            "conversation",
            setOf(WireViewModelOwner.Flow("new-conversation")),
        )
        tracker.registerEntry(
            "meeting",
            setOf(WireViewModelOwner.Flow("new-meeting")),
        )

        tracker.onEntryPopped("conversation")

        assertEquals(listOf("new-conversation"), releasedFlows)
    }

    @Test
    fun givenSameFlow_whenStoreIsRequestedAgain_thenStoreIsReused() {
        val holder = ViewModelStoreProvider(parentStore = null)

        val first = holder.getOrCreate("flow:new-conversation")
        val second = holder.getOrCreate("flow:new-conversation")

        assertSame(first, second)
    }

    @Test
    fun givenDifferentFlows_whenStoresAreRequested_thenStoresAreIsolated() {
        val holder = ViewModelStoreProvider(parentStore = null)

        val conversation = holder.getOrCreate("flow:new-conversation")
        val meeting = holder.getOrCreate("flow:new-meeting")

        assertNotSame(conversation, meeting)
    }

    @Test
    fun givenReleasedFlow_whenItsStoreIsRequestedAgain_thenFreshStoreIsCreated() {
        val holder = ViewModelStoreProvider(parentStore = null)
        val original = holder.getOrCreate("flow:new-conversation")

        holder.clearKey("flow:new-conversation")
        val recreated = holder.getOrCreate("flow:new-conversation")

        assertNotSame(original, recreated)
    }
}
