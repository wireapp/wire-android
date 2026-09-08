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

import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireRoute
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WireResponsivePresentationPolicyTest {

    @Test
    fun givenRegisteredRouteOnPhone_whenEntryIsResolved_thenOriginalEntryIsUnchanged() {
        val route = TabletRoute()
        val original = entry(route, metadata = mapOf("feature" to "metadata"))
        val provider = responsiveEntryProvider(
            delegate = { original },
            policy = routeTypePolicy(),
            isTablet = false,
        )

        assertSame(original, provider(route))
    }

    @Test
    fun givenUnregisteredRouteOnTablet_whenEntryIsResolved_thenOriginalEntryIsUnchanged() {
        val route = PhoneRoute()
        val original = entry(route)
        val provider = responsiveEntryProvider(
            delegate = { original },
            policy = routeTypePolicy(),
            isTablet = true,
        )

        assertSame(original, provider(route))
    }

    @Test
    fun givenRegisteredRouteOnTablet_whenEntryIsResolved_thenLegacyDialogPropertiesAreApplied() {
        val route = TabletRoute()
        val original = entry(route, metadata = mapOf("feature" to "metadata"))
        val provider = responsiveEntryProvider(
            delegate = { original },
            policy = routeTypePolicy(),
            isTablet = true,
        )

        val resolved = provider(route)

        assertEquals("metadata", resolved.metadata["feature"])
        assertTrue(
            DialogSceneStrategy.dialog(WireTabletDialogProperties).all { (key, value) ->
                resolved.metadata[key] == value
            }
        )
        assertEquals(original.contentKey, resolved.contentKey)
    }

    @Test
    fun givenExplicitDialogOnTablet_whenResponsivePolicyAlsoMatches_thenExplicitPropertiesWin() {
        val explicitProperties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        )
        val route = TabletRoute()
        val original = entry(
            route,
            metadata = WireEntryPresentation.dialog(explicitProperties).navEntryMetadata,
        )
        val provider = responsiveEntryProvider(
            delegate = { original },
            policy = routeTypePolicy(),
            isTablet = true,
        )

        val resolved = provider(route)

        assertTrue(
            DialogSceneStrategy.dialog(explicitProperties).all { (key, value) ->
                resolved.metadata[key] == value
            }
        )
    }

    @Test
    fun givenTypedPredicate_whenRouteDataMatches_thenOnlyMatchingInstanceUsesDialog() {
        val policy = WireResponsivePresentationPolicy.tabletDialogsWhere { route ->
            route is TabletRoute && route.useDialog
        }

        assertTrue(
            policy.shouldPresentAsTabletDialog(
                TabletRoute(useDialog = true),
                isTablet = true,
            )
        )
        assertFalse(
            policy.shouldPresentAsTabletDialog(
                TabletRoute(useDialog = false),
                isTablet = true,
            )
        )
    }

    @Test
    fun givenPhoneLayout_whenPresentationIsResolved_thenExplicitPresentationIsPreserved() {
        val route = TabletRoute()

        assertSame(
            WireEntryPresentation.PopUp,
            routeTypePolicy().resolve(
                route = route,
                isTablet = false,
                defaultPresentation = WireEntryPresentation.PopUp,
            ),
        )
    }

    private fun routeTypePolicy(): WireResponsivePresentationPolicy =
        WireResponsivePresentationPolicy.tabletDialogsFor(setOf(TabletRoute::class))

    private fun entry(
        route: TestRoute,
        metadata: Map<String, Any> = emptyMap(),
    ): NavEntry<NavKey> = NavEntry<NavKey>(
        key = route,
        contentKey = route.entryId.value,
        metadata = metadata,
    ) { }

    private sealed interface TestRoute : WireRoute

    @Serializable
    private data class TabletRoute(
        val useDialog: Boolean = true,
        override val entryId: WireNavEntryId = WireNavEntryId("tablet"),
    ) : TestRoute {
        override val routeId: String = "tablet"
    }

    @Serializable
    private data class PhoneRoute(
        override val entryId: WireNavEntryId = WireNavEntryId("phone"),
    ) : TestRoute {
        override val routeId: String = "phone"
    }
}
