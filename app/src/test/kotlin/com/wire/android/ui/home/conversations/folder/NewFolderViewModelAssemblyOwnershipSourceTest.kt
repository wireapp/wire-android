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

class NewFolderViewModelAssemblyOwnershipSourceTest {

    @Test
    fun newFolderBindingIsFeatureOwnedAndInstalledOnceByAppComposition() {
        val appGraph = sourceFile(
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/ConversationSearchFolderViewModelGraph.kt",
        )
        val featureGraph = sourceFile(
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/NewFolderViewModelGraph.kt",
        )
        val sessionGraph = sourceFile("app/src/main/kotlin/com/wire/android/di/metro/AppSessionViewModelGraph.kt")
        val auxEntries = sourceFile(
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/ConversationAuxNavigation3Entries.kt",
        )
        val removedAppBindings = File(
            repositoryRoot(),
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/ConversationSearchFolderMetroViewModelBindings.kt",
        )

        assertFalse(appGraph.contains("newFolderViewModel"))
        assertFalse(appGraph.contains("NewFolderViewModel"))
        assertFalse(removedAppBindings.exists(), "The app-only NewFolder binding file must be removed.")
        assertTrue(featureGraph.contains("object NewFolderMetroViewModelBindings"))
        assertTrue(featureGraph.contains("@ViewModelKey(NewFolderViewModel::class)"))
        assertTrue(featureGraph.contains("fun newFolderViewModel(): NewFolderViewModel ="))
        assertEquals(
            1,
            Regex("\\bNewFolderMetroViewModelBindings::class\\b").findAll(sessionGraph).count(),
        )
        assertFalse(sessionGraph.contains("ConversationSearchFolderMetroViewModelBindings::class"))
        assertTrue(auxEntries.contains("NewConversationFolderNavigation3Entry(runtime, newFolderViewModel())"))
    }

    private fun sourceFile(relativePath: String): String =
        File(repositoryRoot(), relativePath).also { file ->
            assertTrue(file.isFile, "Missing ${file.path}")
        }.readText()

    private fun repositoryRoot(): File =
        generateSequence(File(requireNotNull(System.getProperty("user.dir"))).absoluteFile) { it.parentFile }
            .first { File(it, "settings.gradle.kts").isFile }
}
