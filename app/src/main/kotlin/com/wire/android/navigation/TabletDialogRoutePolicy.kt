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

import com.ramcosta.composedestinations.spec.DestinationStyle
import com.wire.android.navigation.style.DialogNavigation

internal object TabletDialogRoutePolicy {
    internal val destinationBaseRoutes: Set<String> = setOf(
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

    internal fun shouldShowAsDialog(baseRoute: String): Boolean =
        baseRoute in destinationBaseRoutes
}

internal fun resolveTabletDialogParityStyle(
    destinationRoute: String,
    originalStyle: DestinationStyle,
    manualAnimation: DestinationStyle.Animated?,
    isTablet: Boolean,
): DestinationStyle {
    val destinationBaseRoute = destinationRoute.getBaseRoute()
    return if (isTablet && TabletDialogRoutePolicy.shouldShowAsDialog(destinationBaseRoute)) {
        DialogNavigation
    } else {
        manualAnimation ?: originalStyle
    }
}
