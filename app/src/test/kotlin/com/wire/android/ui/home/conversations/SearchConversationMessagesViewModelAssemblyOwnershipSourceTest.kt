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

package com.wire.android.ui.home.conversations

import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SearchConversationMessagesViewModelAssemblyOwnershipSourceTest {

    @Test
    fun searchConversationMessagesFactoryIsFeatureOwnedAndInstalledByAppSessionGraph() {
        val root = repositoryRoot()
        val featureGraph = source(
            root,
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/" +
                "SearchConversationMessagesViewModelGraph.kt",
        )
        val featureViewModel = source(
            root,
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/search/messages/" +
                "SearchConversationMessagesViewModel.kt",
        )
        val featureState = source(
            root,
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/search/messages/" +
                "SearchConversationMessagesState.kt",
        )
        val sessionGraph = source(root, "app/src/main/kotlin/com/wire/android/di/metro/AppSessionViewModelGraph.kt")
        val navigationEntries = source(
            root,
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/ConversationAuxNavigation3Entries.kt",
        )

        assertFalse(
            File(
                root,
                "app/src/main/kotlin/com/wire/android/ui/home/conversations/search/messages/" +
                    "SearchConversationMessagesViewModel.kt",
            ).exists(),
        )
        assertFalse(
            File(
                root,
                "app/src/main/kotlin/com/wire/android/ui/home/conversations/search/messages/" +
                    "SearchConversationMessagesState.kt",
            ).exists(),
        )
        assertFalse(
            File(
                root,
                "app/src/main/kotlin/com/wire/android/ui/home/conversations/" +
                    "ConversationSearchFolderViewModelGraph.kt",
            ).exists(),
        )
        assertTrue(featureGraph.contains("object SearchConversationMessagesManualViewModelFactoryGroup"))
        assertTrue(featureGraph.contains("fun searchConversationMessagesViewModel("))
        assertTrue(featureViewModel.contains("SearchConversationMessagesManualViewModelFactoryGroup::class"))
        assertTrue(featureViewModel.contains("factoryMethod = \"searchConversationMessagesViewModel\""))
        assertTrue(featureViewModel.contains("@Assisted searchConversationMessagesNavArgs: SearchConversationMessagesNavArgs"))
        assertTrue(featureState.contains("data class SearchConversationMessagesState"))
        assertEquals(
            1,
            Regex("\\bSearchConversationMessagesManualViewModelFactoryMetroBindings::class\\b")
                .findAll(sessionGraph)
                .count(),
        )
        assertFalse(sessionGraph.contains("ConversationSearchFolderManualViewModelFactoryMetroBindings"))
        assertTrue(navigationEntries.contains("searchConversationMessagesViewModel(it.toViewModelArgs())"))
    }

    private fun source(root: File, relativePath: String): String =
        File(root, relativePath).also { file ->
            assertTrue(file.isFile, "Missing ${file.path}")
        }.readText()

    private fun repositoryRoot(): File =
        generateSequence(File(requireNotNull(System.getProperty("user.dir"))).absoluteFile) { it.parentFile }
            .first { File(it, "settings.gradle.kts").isFile }
}
