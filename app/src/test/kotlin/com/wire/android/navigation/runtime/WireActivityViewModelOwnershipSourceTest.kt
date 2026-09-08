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

import java.io.File
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WireActivityViewModelOwnershipSourceTest {

    @Test
    fun givenActivityChromeViewModels_whenInspectingWireActivity_thenOwnershipIsDelegated() {
        val activity = sourceFile("WireActivity.kt").readText()
        val host = sourceFile("WireActivityNavigation3Host.kt").readText()
        val owner = sourceFile("WireActivityScopedViewModels.kt").readText()

        assertTrue(host.contains("wireActivityScopedViewModels(activitySessionViewModelStoreOwner)"))
        assertFalse(activity.contains("CallFeedbackViewModel:"))
        assertFalse(activity.contains("CommonTopAppBarViewModel:"))
        assertFalse(activity.contains(".callingViewModelFactory.callFeedbackViewModel()"))
        assertFalse(activity.contains(".miscViewModelFactory.legalHoldRequestedViewModel()"))
        assertFalse(host.contains(".callingViewModelFactory.callFeedbackViewModel()"))
        assertFalse(host.contains(".miscViewModelFactory.legalHoldRequestedViewModel()"))

        assertTrue(owner.contains("wireMetroViewModel(owner = owner)"))
        assertFalse(owner.contains("sessionKeyedMetroViewModel"))
        assertTrue(owner.contains("commonTopAppBarViewModel("))
        assertTrue(host.contains("teardownWireActivitySession("))
        assertTrue(host.contains("sessionGraphStore.markAvailable(confirmedUserId)"))
        assertFalse(host.contains("sessionGraphStore::invalidate"))
        assertFalse(host.contains("invalidateActive()"))
        assertFalse(host.contains("retainedGraphContext"))
        assertFalse(host.contains("sessionGraph?.currentAccount"))
        assertFalse(host.contains("imageLoaderSessionGraph"))
        assertTrue(host.contains("retainedRouteSessionId"))
        assertFalse(activity.contains("LastKnownCurrentAccount"))
        assertFalse(host.contains("LastKnownCurrentAccount"))
        assertFalse(activity.contains("WireNavigationCommand(NewLoginRoute.start()"))
        assertTrue(activity.contains("authenticationRouter.openLoginFromActivity()"))
    }

    private fun sourceFile(name: String): File {
        val relative = "src/main/kotlin/com/wire/android/ui/$name"
        return sequenceOf(
            File(relative),
            File("app/$relative"),
            File("../app/$relative"),
        ).first(File::isFile)
    }
}
