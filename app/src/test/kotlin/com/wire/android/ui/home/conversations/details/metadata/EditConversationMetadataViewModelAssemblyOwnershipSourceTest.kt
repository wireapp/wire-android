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

package com.wire.android.ui.home.conversations.details.metadata

import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EditConversationMetadataViewModelAssemblyOwnershipSourceTest {

    @Test
    fun editConversationMetadataFactoryIsFeatureOwnedAndInstalledOnceByAppComposition() {
        val root = repositoryRoot()
        val appGraph = source(
            root,
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/ConversationDetailsViewModelGraph.kt",
        )
        val featureGraph = source(
            root,
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/" +
                    "EditConversationMetadataViewModelGraph.kt",
        )
        val featureViewModel = source(
            root,
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/metadata/" +
                    "EditConversationMetadataViewModel.kt",
        )
        val sessionGraph = source(root, "app/src/main/kotlin/com/wire/android/di/metro/AppSessionViewModelGraph.kt")

        assertFalse(
            File(
                root,
                "app/src/main/kotlin/com/wire/android/ui/home/conversations/details/metadata/" +
                        "EditConversationMetadataViewModel.kt",
            ).exists(),
        )
        assertTrue(appGraph.contains("fun checkAssetRestrictionsViewModel()"))
        assertTrue(appGraph.contains("wireMetroViewModel()"))
        assertFalse(appGraph.contains("editConversationMetadataViewModel"))
        assertFalse(appGraph.contains("wireAssistedMetroViewModel"))
        assertFalse(appGraph.contains("ConversationDetailsManualViewModelFactoryGroup"))
        assertTrue(featureGraph.contains("object EditConversationMetadataManualViewModelFactoryGroup"))
        assertTrue(featureGraph.contains("fun editConversationMetadataViewModel()"))
        assertTrue(featureGraph.contains("fun editConversationMetadataViewModel(args: EditConversationNameNavArgs)"))
        assertTrue(
            featureGraph.contains(
                "wireAssistedMetroViewModel<EditConversationMetadataViewModel, " +
                        "EditConversationMetadataManualViewModelFactory>",
            ),
        )
        assertTrue(featureViewModel.contains("EditConversationMetadataManualViewModelFactoryGroup::class"))
        assertTrue(featureViewModel.contains("factoryMethod = \"editConversationMetadataViewModel\""))
        assertEquals(
            1,
            Regex("\\bEditConversationMetadataManualViewModelFactoryMetroBindings::class\\b")
                .findAll(sessionGraph)
                .count(),
        )
        assertFalse(
            Regex("\\bConversationDetailsManualViewModelFactoryMetroBindings\\b").containsMatchIn(sessionGraph),
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
