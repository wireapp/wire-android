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

import com.wire.android.navigation.navigation3.WireResponsivePresentationPolicy
import com.wire.android.ui.home.conversations.ConversationFoldersRoute
import com.wire.android.ui.home.conversations.NewConversationFolderRoute
import com.wire.android.ui.home.conversations.details.ChannelAccessOnUpdateRoute
import com.wire.android.ui.home.conversations.details.EditConversationNameRoute
import com.wire.android.ui.home.conversations.details.EditGuestAccessRoute
import com.wire.android.ui.home.conversations.details.EditSelfDeletingMessagesRoute
import com.wire.android.ui.home.conversations.details.GroupConversationDetailsRoute
import com.wire.android.ui.home.conversations.details.UpdateAppsAccessRoute
import com.wire.android.ui.home.settings.ChangeDisplayNameRoute
import com.wire.android.ui.home.settings.ChangeEmailRoute
import com.wire.android.ui.home.settings.ChangeHandleRoute
import com.wire.android.ui.home.settings.ChangeUserColorRoute
import com.wire.android.ui.settings.devices.DeviceDetailsRoute
import com.wire.android.ui.userprofile.avatarpicker.AvatarPickerRoute
import com.wire.android.ui.userprofile.other.OtherUserProfileRoute
import com.wire.android.ui.userprofile.self.SelfUserProfileRoute
import com.wire.android.ui.userprofile.service.ServiceDetailsRoute
import com.wire.navigation.WireRoute
import kotlin.reflect.KClass

/** Navigation 3 route types presented as dialogs on tablets. */
internal val wireNavigation3TabletDialogRouteTypes: Set<KClass<out WireRoute>> =
    setOf(
        ServiceDetailsRoute::class,
        OtherUserProfileRoute::class,
        SelfUserProfileRoute::class,
        DeviceDetailsRoute::class,
        ChangeDisplayNameRoute::class,
        ChangeHandleRoute::class,
        ChangeEmailRoute::class,
        AvatarPickerRoute::class,
        GroupConversationDetailsRoute::class,
        EditConversationNameRoute::class,
        EditGuestAccessRoute::class,
        UpdateAppsAccessRoute::class,
        ChannelAccessOnUpdateRoute::class,
        EditSelfDeletingMessagesRoute::class,
        ConversationFoldersRoute::class,
        NewConversationFolderRoute::class,
        ChangeUserColorRoute::class,
    )

/**
 * Production tablet presentation policy.
 */
internal val WireNavigation3ResponsivePresentationPolicy: WireResponsivePresentationPolicy =
    WireResponsivePresentationPolicy.tabletDialogsFor(wireNavigation3TabletDialogRouteTypes)
