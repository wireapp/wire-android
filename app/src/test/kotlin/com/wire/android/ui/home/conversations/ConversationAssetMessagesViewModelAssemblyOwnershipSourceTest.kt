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

class ConversationAssetMessagesViewModelAssemblyOwnershipSourceTest {

    @Test
    fun conversationAssetMessagesFactoryIsFeatureOwnedAndInstalledByAppSessionGraph() {
        val root = repositoryRoot()
        val coreGraph = source(
            root,
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/ConversationCoreViewModelGraph.kt",
        )
        val featureGraph = source(
            root,
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/" +
                "ConversationAssetMessagesViewModelGraph.kt",
        )
        val featureViewModel = source(
            root,
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/media/" +
                "ConversationAssetMessagesViewModel.kt",
        )
        val featureState = source(
            root,
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/media/" +
                "ConversationAssetMessagesViewState.kt",
        )
        val sessionGraph = source(root, "app/src/main/kotlin/com/wire/android/di/metro/AppSessionViewModelGraph.kt")
        val navigationEntries = source(
            root,
            "app/src/main/kotlin/com/wire/android/navigation/routes/media/MediaNavigation3Entries.kt",
        )

        assertFalse(
            File(
                root,
                "app/src/main/kotlin/com/wire/android/ui/home/conversations/media/ConversationAssetMessagesViewModel.kt",
            ).exists(),
        )
        assertFalse(
            File(
                root,
                "app/src/main/kotlin/com/wire/android/ui/home/conversations/media/ConversationAssetMessagesViewState.kt",
            ).exists(),
        )
        assertFalse(coreGraph.contains("conversationAssetMessagesViewModel"))
        assertFalse(coreGraph.contains("ConversationAssetMessagesViewModel"))
        assertTrue(featureGraph.contains("object ConversationAssetMessagesManualViewModelFactoryGroup"))
        assertTrue(featureGraph.contains("fun conversationAssetMessagesViewModel(args: ConversationMediaNavArgs)"))
        assertTrue(featureViewModel.contains("ConversationAssetMessagesManualViewModelFactoryGroup::class"))
        assertTrue(featureViewModel.contains("factoryMethod = \"conversationAssetMessagesViewModel\""))
        assertTrue(featureViewModel.contains("@Assisted navigationArgs: ConversationMediaNavArgs"))
        assertTrue(featureState.contains("data class ConversationAssetMessagesViewState"))
        assertFalse(featureViewModel.contains("com.wire.android.R"))
        assertFalse(featureViewModel.contains("ConversationCoreManualViewModelFactoryGroup"))
        assertEquals(
            1,
            Regex("\\bConversationAssetMessagesManualViewModelFactoryMetroBindings::class\\b")
                .findAll(sessionGraph)
                .count(),
        )
        assertTrue(
            navigationEntries.contains("conversationAssetMessagesViewModel(route.toViewModelArgs())"),
        )
    }

    private fun source(root: File, relativePath: String): String =
        File(root, relativePath).also { file ->
            assertTrue(file.isFile, "Missing ${file.path}")
        }.readText()

    private fun repositoryRoot(): File =
        generateSequence(File(requireNotNull(System.getProperty("user.dir"))).absoluteFile) { it.parentFile }
            .first { File(it, "settings.gradle.kts").isFile }
}
