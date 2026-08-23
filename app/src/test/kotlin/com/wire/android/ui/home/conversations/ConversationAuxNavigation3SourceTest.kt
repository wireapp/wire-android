/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.home.conversations

import java.io.File
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConversationAuxNavigation3SourceTest {
    @Test
    fun givenRouteContracts_whenInspectingSource_thenTheyStayKmpPure() {
        val source = source("ui/home/conversations/ConversationAuxNavigation3.kt")
        listOf("android.", "androidx.", "com.ramcosta.", "com.wire.kalium.").forEach { prefix ->
            assertFalse(source.lineSequence().any { it.startsWith("import $prefix") })
        }
        listOf("Bundle", "SavedStateHandle", "NavArgs", "Parcelable").forEach {
            assertFalse(source.contains(it))
        }
    }

    @Test
    fun givenContribution_whenInspectingSource_thenAllSevenEntriesAreTyped() {
        val source = source("ui/home/conversations/ConversationAuxNavigation3Entries.kt")
        listOf(
            "BrowseChannelsRoute",
            "ConversationFoldersRoute",
            "NewConversationFolderRoute",
            "SearchConversationMessagesRoute",
            "PromoteAdminRoute",
            "AddMembersSearchRoute",
            "DebugConversationRoute",
        ).forEach { assertTrue(source.contains("wireEntry<$it>")) }
        listOf("ScreenDestination", "SavedStateHandle", "Bundle", "NavController").forEach {
            assertFalse(source.contains(it))
        }
    }

    @Test
    fun givenArgumentOwningViewModels_whenInspectingSources_thenArgumentsAreTyped() {
        listOf(
            "ui/home/conversations/search/messages/SearchConversationMessagesViewModel.kt",
            "ui/debug/conversation/DebugConversationViewModel.kt",
        ).forEach { path ->
            val source = source(path)
            assertFalse(source.contains("SavedStateHandle"))
            assertFalse(source.contains("generated.app.navArgs"))
        }
        val promoteAdminSource = featureSource(
            "ui/home/conversations/promoteadmin/PromoteAdminViewModel.kt",
        )
        assertFalse(promoteAdminSource.contains("SavedStateHandle"))
        assertFalse(promoteAdminSource.contains("generated.app.navArgs"))
        val addMembersSource = featureSource(
            "ui/home/conversations/search/adddembertoconversation/AddMembersToConversationViewModel.kt",
        )
        assertFalse(addMembersSource.contains("SavedStateHandle"))
        assertFalse(addMembersSource.contains("generated.app.navArgs"))
    }

    @Test
    fun givenAddMembersCompletes_whenInspectingScreen_thenBackStackMutationRunsAsAnEffect() {
        val source = source(
            "ui/home/conversations/search/adddembertoconversation/AddMembersSearchScreen.kt"
        )

        assertTrue(source.contains("LaunchedEffect(viewModel.newGroupState.isCompleted)"))
        assertTrue(source.contains("onNavigateBack()"))
    }

    private fun source(path: String): String {
        val userDir = requireNotNull(System.getProperty("user.dir"))
        val root = generateSequence(File(userDir)) { it.parentFile }
            .first { File(it, "app/src/main/kotlin").isDirectory }
        return File(root, "app/src/main/kotlin/com/wire/android/$path")
            .also { assertTrue(it.isFile) }
            .readText()
    }

    private fun featureSource(path: String): String {
        val userDir = requireNotNull(System.getProperty("user.dir"))
        val root = generateSequence(File(userDir)) { it.parentFile }
            .first { File(it, "app/src/main/kotlin").isDirectory }
        return File(root, "features/conversation/src/main/kotlin/com/wire/android/$path")
            .also { assertTrue(it.isFile) }
            .readText()
    }
}
