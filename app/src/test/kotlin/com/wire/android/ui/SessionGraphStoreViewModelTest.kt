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

package com.wire.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wire.android.di.metro.AppSessionViewModelGraph
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.user.UserId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SessionGraphStoreViewModelTest {

    @Test
    fun givenSessionGraphIsInvalidated_whenSameUserLogsInAgain_thenGraphAndViewModelsAreRecreated() {
        val oldImageLoader = mockk<WireSessionImageLoader>(relaxed = true)
        val oldGraph = graphWith(oldImageLoader)
        val newGraph = graphWith(mockk(relaxed = true))
        val graphs = ArrayDeque(listOf(oldGraph, newGraph))
        val store = SessionGraphStoreViewModel { graphs.removeFirst() }
        val oldRetainedGraph = store.retainedGraphFor(USER_ID)
        val oldViewModel = TrackingViewModel()
        ViewModelProvider(
            oldRetainedGraph,
            viewModelFactory { initializer { oldViewModel } }
        )[TrackingViewModel::class.java]

        store.invalidate(USER_ID)
        val newRetainedGraph = store.retainedGraphFor(USER_ID)

        assertNotSame(oldRetainedGraph, newRetainedGraph)
        assertSame(newGraph, newRetainedGraph.graph)
        assertTrue(oldViewModel.wasCleared)
        verify(exactly = 1) { oldImageLoader.shutdown() }
    }

    @Test
    fun givenSessionGraphIsStillValid_whenRequestedAgain_thenGraphIsReused() {
        val graph = graphWith(mockk(relaxed = true))
        val store = SessionGraphStoreViewModel { graph }

        val first = store.retainedGraphFor(USER_ID)
        val second = store.retainedGraphFor(USER_ID)

        assertSame(first, second)
    }

    @Test
    fun givenSameUserIsRemovedAndLogsInRepeatedly_whenGraphsAreInvalidated_thenEveryLoginGetsAFreshGraph() {
        val imageLoaders = List(RELOGIN_COUNT) { mockk<WireSessionImageLoader>(relaxed = true) }
        val graphs = ArrayDeque(imageLoaders.map(::graphWith))
        val store = SessionGraphStoreViewModel { graphs.removeFirst() }

        val retainedGraphs = buildList {
            repeat(RELOGIN_COUNT) { index ->
                add(store.retainedGraphFor(USER_ID))
                if (index < RELOGIN_COUNT - 1) {
                    store.invalidate(USER_ID)
                }
            }
        }

        retainedGraphs.zipWithNext().forEach { (oldGraph, newGraph) ->
            assertNotSame(oldGraph, newGraph)
            assertNotSame(oldGraph.graph, newGraph.graph)
        }
        imageLoaders.dropLast(1).forEach { imageLoader ->
            verify(exactly = 1) { imageLoader.shutdown() }
        }
        verify(exactly = 0) { imageLoaders.last().shutdown() }
    }

    @Test
    fun givenTwoRetainedUsers_whenRemovedUserIsInvalidated_thenOtherUsersGraphRemainsActive() {
        val removedUserImageLoader = mockk<WireSessionImageLoader>(relaxed = true)
        val otherUserImageLoader = mockk<WireSessionImageLoader>(relaxed = true)
        val graphsByUser = mapOf(
            USER_ID to graphWith(removedUserImageLoader),
            OTHER_USER_ID to graphWith(otherUserImageLoader),
        )
        val store = SessionGraphStoreViewModel { userId -> graphsByUser.getValue(userId) }
        val removedUsersGraph = store.retainedGraphFor(USER_ID)
        val otherUsersGraph = store.retainedGraphFor(OTHER_USER_ID)

        store.invalidate(USER_ID)

        assertSame(otherUsersGraph, store.retainedGraphFor(OTHER_USER_ID))
        assertNotSame(removedUsersGraph, store.retainedGraphFor(USER_ID))
        verify(exactly = 1) { removedUserImageLoader.shutdown() }
        verify(exactly = 0) { otherUserImageLoader.shutdown() }
    }

    private fun graphWith(imageLoader: WireSessionImageLoader) = mockk<AppSessionViewModelGraph> {
        every { wireSessionImageLoader } returns imageLoader
    }

    private class TrackingViewModel : ViewModel() {
        var wasCleared = false
            private set

        override fun onCleared() {
            wasCleared = true
        }
    }

    private companion object {
        const val RELOGIN_COUNT = 3
        val USER_ID = UserId("user", "domain")
        val OTHER_USER_ID = UserId("other-user", "domain")
    }
}
