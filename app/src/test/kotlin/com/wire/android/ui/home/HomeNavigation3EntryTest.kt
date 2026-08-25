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

package com.wire.android.ui.home

import com.wire.android.navigation.HomeDestination
import com.wire.android.navigation.runtime.startup.HomeRoute
import com.wire.android.ui.home.conversations.ConversationCompletionAction
import com.wire.android.ui.home.conversations.ConversationCompletionResult
import com.wire.android.ui.userprofile.other.ConnectionRequestIgnoredResult
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireSessionId
import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HomeNavigation3EntryTest {

    @Test
    fun givenHomeRoute_whenUsedByNavigation3_thenLegacyRouteIdentityIsExact() {
        val route = HomeRoute(
            sessionId = WireSessionId("user", "wire.test"),
            entryId = WireNavEntryId("home-entry"),
        )

        assertEquals("app/home_screen", route.routeId)
        assertEquals("app/home_screen", HomeRoute.ROUTE_ID)
    }

    @Test
    fun givenInternalDrawerItems_whenMapped_thenEveryItemHasStableTopLevelIdentity() {
        val expected = mapOf(
            HomeDestination.Conversations to HomeTopLevelDestination.CONVERSATIONS,
            HomeDestination.Settings to HomeTopLevelDestination.SETTINGS,
            HomeDestination.Vault to HomeTopLevelDestination.VAULT,
            HomeDestination.Archive to HomeTopLevelDestination.ARCHIVE,
            HomeDestination.WhatsNew to HomeTopLevelDestination.WHATS_NEW,
            HomeDestination.Cells to HomeTopLevelDestination.CELLS,
            HomeDestination.Meetings to HomeTopLevelDestination.MEETINGS,
        )

        expected.forEach { (legacy, typed) ->
            assertEquals(HomeNavigation3Target.TopLevel(typed), legacy.toNavigation3Target())
            assertEquals(legacy, typed.toHomeDestination())
        }
    }

    @Test
    fun givenExternalDrawerItems_whenMapped_thenTheyDoNotBecomeTopLevelSelection() {
        assertEquals(
            HomeNavigation3Target.External(HomeExternalDestination.SUPPORT),
            HomeDestination.Support.toNavigation3Target(),
        )
        assertEquals(
            HomeNavigation3Target.External(HomeExternalDestination.TEAM_MANAGEMENT),
            HomeDestination.TeamManagement.toNavigation3Target(),
        )
    }

    @Test
    fun givenSecondaryHomeDestination_whenBackIsPressed_thenConversationsIsSelected() {
        HomeTopLevelDestination.entries
            .filterNot { it == HomeTopLevelDestination.CONVERSATIONS }
            .forEach { destination ->
                assertEquals(
                    HomeTopLevelDestination.CONVERSATIONS,
                    destination.backDestination(),
                    "Unexpected back destination for $destination",
                )
            }
    }

    @Test
    fun givenConversationsHomeDestination_whenBackIsPressed_thenNavigation3MayHandleRootBack() {
        assertEquals(null, HomeTopLevelDestination.CONVERSATIONS.backDestination())
    }

    @Test
    fun givenLeaveConversationCompletion_whenMapped_thenHomeShowsSuccessMessage() {
        val message = ConversationCompletionResult(
            action = ConversationCompletionAction.LEAVE_GROUP,
            conversationName = "ignored for leave",
        ).toHomeSnackBarMessage()

        assertEquals(HomeSnackBarMessage.LeftConversationSuccess, message)
    }

    @Test
    fun givenDeleteConversationCompletion_whenMapped_thenHomeShowsConversationName() {
        val message = ConversationCompletionResult(
            action = ConversationCompletionAction.DELETE_GROUP,
            conversationName = "Design",
        ).toHomeSnackBarMessage()

        assertEquals(HomeSnackBarMessage.DeletedConversationGroupSuccess("Design"), message)
    }

    @Test
    fun givenIgnoredConnectionRequest_whenMapped_thenHomeShowsUserName() {
        val message = ConnectionRequestIgnoredResult("Alice").toHomeSnackBarMessage()

        assertEquals("Alice", (message as HomeSnackBarMessage.SuccessConnectionIgnoreRequest).userName)
    }

    @Test
    fun givenHomeEntry_whenInspectingResultFlow_thenNavigation3OwnsSnackbarParity() {
        val source = sourceFile().readText()

        assertTrue("ConversationCompletionNavigation3ResultType" in source)
        assertTrue("ConnectionRequestIgnoredNavigation3ResultType" in source)
        assertTrue("runtime.navigateForResult(" in source)
        assertTrue("runtime.consumeResult(" in source)
        assertTrue("toHomeSnackBarMessage()" in source)
        assertFalse("ResultRecipient" in source)
        assertFalse("com.ramcosta.composedestinations.result.NavResult" in source)
        assertFalse("com.ramcosta" in source)
    }

    private fun sourceFile(): File {
        val root = generateSequence(File(checkNotNull(System.getProperty("user.dir")))) { it.parentFile }
            .first { File(it, "app/src/main/kotlin").isDirectory }
        return File(root, "app/src/main/kotlin/com/wire/android/ui/home/HomeNavigation3Entry.kt")
    }
}
