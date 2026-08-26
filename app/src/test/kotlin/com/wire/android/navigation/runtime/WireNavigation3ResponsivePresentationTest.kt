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

import com.wire.android.navigation.navigation3.WireEntryPresentation
import com.wire.android.ui.home.settings.ChangeUserColorRoute
import com.wire.android.feature.cells.navigation.ConversationFilesRoute
import com.wire.navigation.SessionRoute
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireSessionId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

internal class WireNavigation3ResponsivePresentationTest {

    @Test
    fun `given tablet dialog routes, when inspecting typed policy mapping, then all routes are unique`() {
        assertEquals(17, wireNavigation3TabletDialogRouteTypes.size)
        assertEquals(
            setOf(
                "ServiceDetailsRoute",
                "OtherUserProfileRoute",
                "SelfUserProfileRoute",
                "DeviceDetailsRoute",
                "ChangeDisplayNameRoute",
                "ChangeHandleRoute",
                "ChangeEmailRoute",
                "AvatarPickerRoute",
                "GroupConversationDetailsRoute",
                "EditConversationNameRoute",
                "EditGuestAccessRoute",
                "UpdateAppsAccessRoute",
                "ChannelAccessOnUpdateRoute",
                "EditSelfDeletingMessagesRoute",
                "ConversationFoldersRoute",
                "NewConversationFolderRoute",
                "ChangeUserColorRoute",
            ),
            wireNavigation3TabletDialogRouteTypes.mapNotNull { it.simpleName }.toSet(),
        )
        assertFalse(ConversationFilesRoute::class in wireNavigation3TabletDialogRouteTypes)
    }

    @Test
    fun `given tablet dialog typed route, when classifying presentation, then only tablet uses dialog`() {
        val route = ChangeUserColorRoute(TestSessionId)

        assertNotSame(
            WireEntryPresentation.Default,
            WireNavigation3ResponsivePresentationPolicy.resolve(
                route = route,
                isTablet = true,
            ),
        )
        assertSame(
            WireEntryPresentation.Default,
            WireNavigation3ResponsivePresentationPolicy.resolve(
                route = route,
                isTablet = false,
            ),
        )
    }

    @Test
    fun `given route outside tablet dialog list, when classifying presentation, then it remains default`() {
        val route = NonDialogRoute(TestSessionId)

        assertSame(
            WireEntryPresentation.Default,
            WireNavigation3ResponsivePresentationPolicy.resolve(
                route = route,
                isTablet = true,
            ),
        )
    }

    private data class NonDialogRoute(
        override val sessionId: WireSessionId,
        override val entryId: WireNavEntryId = WireNavEntryId("non-dialog-entry"),
    ) : SessionRoute {
        override val routeId: String = "test/non-dialog"
    }

    private companion object {
        val TestSessionId = WireSessionId(value = "user", domain = "wire.test")
    }
}
