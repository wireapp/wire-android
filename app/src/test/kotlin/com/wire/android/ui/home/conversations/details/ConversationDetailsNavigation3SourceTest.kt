/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.home.conversations.details

import java.io.File
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConversationDetailsNavigation3SourceTest {
    @Test
    fun givenRouteContracts_whenInspectingSource_thenTheyStayKmpPure() {
        val source = source("ConversationDetailsNavigation3.kt")
        listOf("android.", "androidx.", "com.ramcosta.", "com.wire.kalium.").forEach {
            assertFalse(source.lineSequence().any { line -> line.startsWith("import $it") })
        }
        listOf("Bundle", "SavedStateHandle", "NavArgs", "Parcelable").forEach {
            assertFalse(source.contains(it))
        }
    }

    @Test
    fun givenContribution_whenInspectingSource_thenAllEightEntriesUseTypedFactories() {
        val source = source("ConversationDetailsNavigation3Entries.kt")
        listOf(
            "GroupConversationDetailsRoute",
            "EditConversationNameRoute",
            "EditSelfDeletingMessagesRoute",
            "GroupConversationAllParticipantsRoute",
            "UpdateAppsAccessRoute",
            "ChannelAccessOnUpdateRoute",
            "EditGuestAccessRoute",
            "CreatePasswordProtectedGuestLinkRoute",
        ).forEach { assertTrue(source.contains("wireEntry<$it>")) }
        assertFalse(source.contains("ScreenDestination"))
        assertFalse(source.contains("SavedStateHandle"))
        assertFalse(source.contains("Bundle"))
        assertTrue(source.contains("toViewModelArgs()"))
    }

    @Test
    fun givenMigratedViewModels_whenInspectingSources_thenArgumentsAreTyped() {
        viewModelPaths.forEach { path ->
            val source = source(path)
            assertFalse(source.contains("SavedStateHandle"))
            assertFalse(source.contains("generated.app.navArgs"))
            assertTrue(source.contains("navigationArgs:"))
        }
    }

    private fun source(path: String): String {
        val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "app/src/main/kotlin").isDirectory }
        val file = if (path.startsWith("features/")) {
            File(root, path)
        } else {
            File(root, "app/src/main/kotlin/com/wire/android/ui/home/conversations/details/$path")
        }
        return file
            .also { assertTrue(it.isFile) }
            .readText()
    }

    private companion object {
        val viewModelPaths = listOf(
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/GroupConversationDetailsViewModel.kt",
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/metadata/EditConversationMetadataViewModel.kt",
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/editselfdeletingmessages/EditSelfDeletingMessagesViewModel.kt",
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/participants/GroupConversationParticipantsViewModel.kt",
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/updateappsaccess/UpdateAppsAccessViewModel.kt",
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/updatechannelaccess/UpdateChannelAccessViewModel.kt",
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/editguestaccess/EditGuestAccessViewModel.kt",
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/editguestaccess/createPasswordProtectedGuestLink/CreatePasswordGuestLinkViewModel.kt",
        )
    }
}
