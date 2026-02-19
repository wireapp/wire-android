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

package com.wire.android.navigation

import com.wire.android.navigation.style.AuthSlideNavigationAnimation
import com.wire.android.navigation.style.DialogNavigation
import com.wire.android.navigation.style.SlideNavigationAnimation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class TabletDialogRoutePolicyTest {

    @Test
    fun `given route policy, when checking members, then contains all 17 pre migration routes`() {
        val expectedRoutes = setOf(
            "app/service_details_screen",
            "app/other_user_profile_screen",
            "app/self_user_profile_screen",
            "app/device_details_screen",
            "app/change_display_name_screen",
            "app/change_handle_screen",
            "app/change_email_screen",
            "app/avatar_picker_screen",
            "app/group_conversation_details_screen",
            "app/edit_conversation_name_screen",
            "app/edit_guest_access_screen",
            "app/update_apps_access_screen",
            "app/channel_access_on_update_screen",
            "app/edit_self_deleting_messages_screen",
            "app/conversation_folders_screen",
            "app/new_conversation_folder_screen",
            "app/change_user_color_screen",
        )

        assertEquals(17, TabletDialogRoutePolicy.destinationBaseRoutes.size)
        assertEquals(expectedRoutes, TabletDialogRoutePolicy.destinationBaseRoutes)
    }

    @Test
    fun `given tablet and listed route, when resolving style, then dialog style is used`() {
        val resolvedStyle = resolveTabletDialogParityStyle(
            destinationRoute = "app/change_user_color_screen/{arg}?foo=bar",
            originalStyle = SlideNavigationAnimation,
            manualAnimation = null,
            isTablet = true,
        )

        assertEquals(DialogNavigation, resolvedStyle)
    }

    @Test
    fun `given phone and listed route, when resolving style, then original phone style is used`() {
        val resolvedStyle = resolveTabletDialogParityStyle(
            destinationRoute = "app/change_user_color_screen/{arg}",
            originalStyle = SlideNavigationAnimation,
            manualAnimation = null,
            isTablet = false,
        )

        assertEquals(SlideNavigationAnimation, resolvedStyle)
    }

    @Test
    fun `given non listed route, when resolving style, then parity does not force dialog and manual animation is respected`() {
        assertFalse(TabletDialogRoutePolicy.shouldShowAsDialog("app/non_listed_screen"))
        assertTrue(TabletDialogRoutePolicy.shouldShowAsDialog("app/edit_guest_access_screen"))

        val resolvedStyle = resolveTabletDialogParityStyle(
            destinationRoute = "app/non_listed_screen/{id}",
            originalStyle = SlideNavigationAnimation,
            manualAnimation = AuthSlideNavigationAnimation,
            isTablet = true,
        )

        assertEquals(AuthSlideNavigationAnimation, resolvedStyle)
    }
}
