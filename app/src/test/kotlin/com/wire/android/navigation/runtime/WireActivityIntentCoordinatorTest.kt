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

import android.app.Application
import android.content.Intent
import android.os.Bundle
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class WireActivityIntentCoordinatorTest {

    private val coordinator = WireActivityIntentCoordinator()

    @Test
    fun givenIntentsArriveBeforeCollection_whenCollectingRequests_thenArrivalOrderIsPreserved() = runTest {
        val initial = Intent("initial")
        val firstNew = Intent("first-new")
        val secondNew = Intent("second-new")
        coordinator.enqueue(
            WireActivityIntentRequest(
                intent = initial,
                savedInstanceState = Bundle(),
                hasTrustedWireShareCaller = false,
            )
        )
        coordinator.enqueue(
            WireActivityIntentRequest(
                intent = firstNew,
                savedInstanceState = null,
                hasTrustedWireShareCaller = true,
            )
        )
        coordinator.enqueue(secondNew)

        val requests = coordinator.requests.take(3).toList()

        assertSame(initial, requests[0].intent)
        assertSame(firstNew, requests[1].intent)
        assertSame(secondNew, requests[2].intent)
        assertFalse(requests[0].hasTrustedWireShareCaller)
        assertTrue(requests[1].hasTrustedWireShareCaller)
        assertFalse(requests[2].hasTrustedWireShareCaller)
    }

    @Test
    fun givenActivityStateIsSaved_whenRestoring_thenOriginalIntentIsReturned() {
        val original = Intent(Intent.ACTION_VIEW)
        val state = Bundle()

        coordinator.saveInstanceState(state, original)

        assertSame(original, coordinator.restoreActivityIntent(state))
        assertTrue(state.getBoolean("deeplink_handled_flag_key"))
    }

    @Test
    fun givenStateWrittenByLegacyActivity_whenRestoring_thenOriginalIntentIsReturned() {
        val original = Intent(Intent.ACTION_VIEW)
        val legacyState = Bundle().apply {
            putParcelable("original_saved_intent", original)
        }

        assertSame(original, coordinator.restoreActivityIntent(legacyState))
    }

    @Test
    fun givenEligibleDeepLink_whenNotHandledAsAnotherIntent_thenDeepLinkIsHandledOnce() = runTest {
        val intent = Intent(Intent.ACTION_VIEW)
        var deepLinkCalls = 0

        val firstEffect = coordinator.handle(
            request = WireActivityIntentRequest(intent, null),
            isEmptyWelcomeStartDestination = { false },
            handleNonDeepLinkIntent = { false },
            handleDeepLink = { deepLinkCalls++ },
        )
        val secondEffect = coordinator.handle(
            request = WireActivityIntentRequest(intent, null),
            isEmptyWelcomeStartDestination = { false },
            handleNonDeepLinkIntent = { false },
            handleDeepLink = { deepLinkCalls++ },
        )

        assertEquals(WireActivityIntentEffect.NONE, firstEffect)
        assertEquals(WireActivityIntentEffect.NONE, secondEffect)
        assertEquals(1, deepLinkCalls)
    }

    @Test
    fun givenEligibleIntentHandledAsNonDeepLink_whenHandling_thenDeepLinkIsNotInvoked() = runTest {
        var deepLinkHandled = false

        coordinator.handle(
            request = WireActivityIntentRequest(Intent(Intent.ACTION_VIEW), null),
            isEmptyWelcomeStartDestination = { false },
            handleNonDeepLinkIntent = { true },
            handleDeepLink = { deepLinkHandled = true },
        )

        assertFalse(deepLinkHandled)
    }

    @Test
    fun givenRestoredOriginalIntent_whenNothingElseHandlesIt_thenLoginFallbackIsRequested() = runTest {
        val original = Intent(Intent.ACTION_VIEW)
        val savedState = Bundle().also { coordinator.saveInstanceState(it, original) }
        var deepLinkHandled = false

        val effect = coordinator.handle(
            request = WireActivityIntentRequest(original, savedState),
            isEmptyWelcomeStartDestination = { true },
            handleNonDeepLinkIntent = { false },
            handleDeepLink = { deepLinkHandled = true },
        )

        assertEquals(WireActivityIntentEffect.OPEN_LOGIN, effect)
        assertFalse(deepLinkHandled)
    }

    @Test
    fun givenDeepLinkEligibleIntent_whenHandling_thenWelcomeFallbackIsNotInspected() = runTest {
        var welcomeWasInspected = false

        coordinator.handle(
            request = WireActivityIntentRequest(Intent(Intent.ACTION_VIEW), null),
            isEmptyWelcomeStartDestination = {
                welcomeWasInspected = true
                true
            },
            handleNonDeepLinkIntent = { false },
            handleDeepLink = {},
        )

        assertFalse(welcomeWasInspected)
    }

    @Test
    fun givenLauncherIntentHandledAsNonDeepLink_whenHandling_thenWelcomeFallbackIsNotRequested() = runTest {
        var deepLinkHandled = false

        val effect = coordinator.handle(
            request = WireActivityIntentRequest(Intent(Intent.ACTION_MAIN), null),
            isEmptyWelcomeStartDestination = { true },
            handleNonDeepLinkIntent = { true },
            handleDeepLink = { deepLinkHandled = true },
        )

        assertEquals(WireActivityIntentEffect.NONE, effect)
        assertFalse(deepLinkHandled)
    }

    @Test
    fun givenIntentFromHistory_whenHandling_thenOnlyNonDeepLinkPathRuns() = runTest {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            flags = Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY
        }
        var nonDeepLinkHandled = false
        var deepLinkHandled = false

        coordinator.handle(
            request = WireActivityIntentRequest(intent, null),
            isEmptyWelcomeStartDestination = { false },
            handleNonDeepLinkIntent = {
                nonDeepLinkHandled = true
                false
            },
            handleDeepLink = { deepLinkHandled = true },
        )

        assertTrue(nonDeepLinkHandled)
        assertFalse(deepLinkHandled)
    }
}
