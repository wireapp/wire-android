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

package com.wire.android.ui.home.conversations.folder

import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MoveConversationToFolderViewModelAssemblyOwnershipSourceTest {

    @Test
    fun moveConversationToFolderFactoryIsFeatureOwnedAndInstalledOnceByAppComposition() {
        val appGraph = sourceFile(
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/ConversationSearchFolderViewModelGraph.kt",
        )
        val featureGraph = sourceFile(
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/MoveConversationToFolderViewModelGraph.kt",
        )
        val sessionGraph = sourceFile("app/src/main/kotlin/com/wire/android/di/metro/AppSessionViewModelGraph.kt")
        val auxEntries = sourceFile(
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/ConversationAuxNavigation3Entries.kt",
        )

        assertFalse(appGraph.contains("moveConversationToFolderViewModel"))
        assertFalse(appGraph.contains("MoveConversationToFolderVM"))
        assertTrue(featureGraph.contains("object MoveConversationToFolderManualViewModelFactoryGroup"))
        assertTrue(featureGraph.contains("fun moveConversationToFolderViewModel("))
        assertTrue(
            featureGraph.contains(
                "instanceKey = \"move_conversation_to_folder_\${args.conversationId}_\${args.currentFolderId}\"",
            ),
        )
        assertTrue(featureGraph.contains("previewProvider = ViewModelScopedPreviews"))
        assertEquals(
            1,
            Regex("\\bMoveConversationToFolderManualViewModelFactoryMetroBindings::class\\b")
                .findAll(sessionGraph)
                .count(),
        )
        assertTrue(auxEntries.contains("val moveViewModel = moveConversationToFolderViewModel("))
        assertTrue(
            auxEntries.contains(
                "MoveConversationToFolderArgs(args.conversationId, args.conversationName, args.currentFolderId)",
            ),
        )
    }

    private fun sourceFile(relativePath: String): String =
        File(repositoryRoot(), relativePath).also { file ->
            assertTrue(file.isFile, "Missing ${file.path}")
        }.readText()

    private fun repositoryRoot(): File =
        generateSequence(File(requireNotNull(System.getProperty("user.dir"))).absoluteFile) { it.parentFile }
            .first { File(it, "settings.gradle.kts").isFile }
}
