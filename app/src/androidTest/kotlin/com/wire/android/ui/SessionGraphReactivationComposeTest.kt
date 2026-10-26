/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.wire.android.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wire.android.di.metro.AppSessionViewModelGraph
import com.wire.android.navigation.navigation3.clearWireViewModelStoreOwner
import com.wire.android.navigation.navigation3.rememberWireSharedViewModelStoreOwner
import com.wire.android.navigation.navigation3.rememberWireViewModelStoreProvider
import com.wire.android.navigation.runtime.SessionGraphStoreViewModel
import com.wire.android.navigation.runtime.SessionGraphStoreViewModel.Lifecycle
import com.wire.android.navigation.runtime.MetroWireEntryGraphResolver
import com.wire.android.navigation.runtime.rememberWireNavigation3ActivityGraphContext
import com.wire.android.navigation.runtime.startup.HomeRoute
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.LogoutCallback
import com.wire.navigation.WireSessionId
import com.wire.navigation.WireViewModelOwner
import com.wire.navigation.stableKey
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SessionGraphReactivationComposeTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun inactiveLogoutWaitsForConsumersAndRecoversAfterHostRecreationWithoutReplacingOtherAccount() {
        with(Scenario()) {
            compose.setContent { Content() }
            compose.onNodeWithText("a:1").assertExists()
            val oldA = graphs.getValue(userA)
            val oldViewModelA = viewModels.getValue(userA)
            compose.runOnIdle { select(userB) }
            compose.onNodeWithText("b:1").assertExists()
            val graphB = graphs.getValue(userB)
            val viewModelB = viewModels.getValue(userB)

            compose.runOnIdle { runBlocking { logout(userA, LogoutReason.REMOVED_CLIENT) } }
            compose.runOnIdle {
                assertEquals(Lifecycle.INVALIDATING, store.lifecycle(userA))
                assertEquals(0, clearRequests)
                assertFalse(events.contains("graph:a:1"))
                assertSame(graphB, graphs[userB])
                assertSame(viewModelB, viewModels[userB])
                select(userA)
            }
            compose.runOnIdle {
                assertEquals(1, clearRequests)
                assertEquals(Lifecycle.INVALIDATING, store.lifecycle(userA))
                assertFalse(events.contains("graph:a:1"))
            }
            compose.activityRule.scenario.recreate()
            compose.activityRule.scenario.onActivity { activity ->
                assertEquals(1, clearRequests)
                holdOutgoingOwner.value = false
                activity.setContent { Content() }
            }
            compose.onNodeWithText("a:2").assertExists()
            compose.runOnIdle {
                assertSame(graphB, graphs[userB])
                assertSame(viewModelB, viewModels[userB])
                assertEquals(1, events.count { it == "graph:a:1" })
                assertTrue(events.contains("entry:a"))
                assertTrue(events.contains("viewModel:a:1"))
                assertTrue(events.indexOf("entry:a") < events.indexOf("graph:a:1"))
                assertTrue(events.indexOf("viewModel:a:1") < events.indexOf("graph:a:1"))
                assertFalse(events.contains("viewModel:b:1"))
                assertNotSame(oldA, graphs[userA])
                assertNotSame(oldViewModelA, viewModels[userA])
                assertEquals(Lifecycle.ACTIVE, store.lifecycle(userA))
                select(userB)
            }
            compose.onNodeWithText("b:1").assertExists()
            compose.runOnIdle {
                assertSame(graphB, graphs[userB])
                assertSame(viewModelB, viewModels[userB])
            }
        }
    }

    private inner class Scenario {
        val userA = UserId("a", "wire.test")
        val userB = UserId("b", "wire.test")
        val selected = mutableStateOf(userA)
        val confirmationGeneration = mutableLongStateOf(1L)
        val holdOutgoingOwner = mutableStateOf(true)
        val events = mutableListOf<String>()
        val generations = mutableMapOf<UserId, Int>()
        val graphs = mutableMapOf<UserId, AppSessionViewModelGraph>()
        val viewModels = mutableMapOf<UserId, TrackedViewModel>()
        var clearRequests = 0
        lateinit var logout: LogoutCallback
        val store = SessionGraphStoreViewModel(
            createSessionGraph = { userId ->
                val generation = (generations[userId] ?: 0) + 1
                generations[userId] = generation
                val loader = mockk<WireSessionImageLoader> {
                    every { shutdown() } answers { events += "graph:${userId.value}:$generation" }
                }
                mockk<AppSessionViewModelGraph> {
                    every { wireSessionImageLoader } returns loader
                    every { metroViewModelFactory } returns mockk(relaxed = true)
                }.also { graphs[userId] = it }
            },
            registerLogoutCallback = { logout = it },
        )

        fun select(userId: UserId) {
            selected.value = userId
            confirmationGeneration.longValue++
        }

        @Composable
        fun Content() {
            val provider = rememberWireViewModelStoreProvider()
            val userId = selected.value
            val availability = rememberConfirmedSessionGraphAvailability(
                confirmedSessionGeneration = confirmationGeneration.longValue,
                currentUserId = userId,
                currentRouteSessionId = WireSessionId(userId.value, userId.domain),
                invalidated = false,
                sessionGraphStore = store,
                clearOwner = { invalidatedUser, onCleared ->
                    clearRequests++
                    clearWireViewModelStoreOwner(provider, ownerKey(invalidatedUser), onCleared)
                },
                confirmCurrentSession = { selected.value == it },
                isStillCurrent = { id, generation -> selected.value == id && confirmationGeneration.longValue == generation },
            )
            val resolver = remember(availability.appliedGeneration) { MetroWireEntryGraphResolver(mockk(), mockk(), store) }
            // Model an outgoing navigation decorator retaining its owner through an exit animation.
            if (holdOutgoingOwner.value) {
                rememberWireSharedViewModelStoreOwner(ownerKey(userA), provider)
                DisposableEffect(Unit) {
                    onDispose { events += "entry:a" }
                }
            }
            val route = remember(userId) { HomeRoute(WireSessionId(userId.value, userId.domain)) }
            val graphContext = rememberWireNavigation3ActivityGraphContext(route, resolver, isUserUiBlocked = false)
                .takeUnless { availability.blocked }
            if (graphContext?.sessionGraph != null) {
                val owner = rememberWireSharedViewModelStoreOwner(ownerKey(userId), provider)
                val generation = generations.getValue(userId)
                val vm = ViewModelProvider.create(
                    owner,
                    viewModelFactory {
                        initializer { TrackedViewModel { events += "viewModel:${userId.value}:$generation" } }
                    },
                )[TrackedViewModel::class]
                viewModels[userId] = vm
                Text("${userId.value}:$generation")
            }
        }
    }

    private fun ownerKey(userId: UserId) =
        WireViewModelOwner.Session(WireSessionId(userId.value, userId.domain)).stableKey()

    private class TrackedViewModel(private val onClear: () -> Unit) : ViewModel() {
        override fun onCleared() = onClear()
    }
}
