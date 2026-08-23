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

class ConversationMigrationViewModelAssemblyOwnershipSourceTest {

    @Test
    fun conversationMigrationFactoryIsFeatureOwnedAndAdaptedByAppComposition() {
        val root = repositoryRoot()
        val coreGraph = source(
            root,
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/ConversationCoreViewModelGraph.kt",
        )
        val appAdapter = source(
            root,
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/ConversationMigrationViewModelAppAdapter.kt",
        )
        val featureGraph = source(
            root,
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/" +
                "ConversationMigrationViewModelGraph.kt",
        )
        val featureViewModel = source(
            root,
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/migration/" +
                "ConversationMigrationViewModel.kt",
        )
        val sessionGraph = source(root, "app/src/main/kotlin/com/wire/android/di/metro/AppSessionViewModelGraph.kt")
        val conversationModule = source(
            root,
            "app/src/main/kotlin/com/wire/android/di/accountScoped/ConversationModule.kt",
        )
        val navigationEntries = source(
            root,
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/ConversationNavigation3Entries.kt",
        )

        assertFalse(
            File(
                root,
                "app/src/main/kotlin/com/wire/android/ui/home/conversations/migration/ConversationMigrationViewModel.kt",
            ).exists(),
        )
        assertFalse(coreGraph.contains("conversationMigrationViewModel"))
        assertFalse(coreGraph.contains("ConversationMigrationViewModel"))
        assertTrue(featureGraph.contains("object ConversationMigrationManualViewModelFactoryGroup"))
        assertTrue(featureGraph.contains("fun conversationMigrationViewModel(conversationId: ConversationId)"))
        assertTrue(featureViewModel.contains("ConversationMigrationManualViewModelFactoryGroup::class"))
        assertTrue(featureViewModel.contains("factoryMethod = \"conversationMigrationViewModel\""))
        assertTrue(featureViewModel.contains("@Assisted conversationId: ConversationId"))
        assertTrue(appAdapter.contains("fun conversationMigrationViewModel(args: ConversationNavArgs)"))
        assertTrue(appAdapter.contains("conversationMigrationViewModel(args.conversationId)"))
        assertFalse(featureGraph.contains("ConversationNavArgs"))
        assertFalse(featureViewModel.contains("ConversationNavArgs"))
        assertFalse(conversationModule.contains("ConversationMigrationViewModel"))
        assertEquals(
            1,
            Regex("\\bConversationMigrationManualViewModelFactoryMetroBindings::class\\b")
                .findAll(sessionGraph)
                .count(),
        )
        assertTrue(
            navigationEntries.contains("conversationMigrationViewModel = conversationMigrationViewModel(viewModelArgs)"),
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
