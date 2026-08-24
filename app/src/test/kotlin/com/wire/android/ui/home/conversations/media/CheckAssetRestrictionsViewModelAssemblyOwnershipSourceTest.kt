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

package com.wire.android.ui.home.conversations.media

import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CheckAssetRestrictionsViewModelAssemblyOwnershipSourceTest {

    @Test
    fun checkAssetRestrictionsViewModelBindingAndGatewayAreFeatureOwnedAndInstalledOnce() {
        val root = repositoryRoot()
        val graph = source(
            root,
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/" +
                    "CheckAssetRestrictionsViewModelGraph.kt",
        )
        val appBindings = source(root, "app/src/main/kotlin/com/wire/android/di/metro/WireMetroViewModelBindings.kt")
        val sessionGraph = source(root, "app/src/main/kotlin/com/wire/android/di/metro/AppSessionViewModelGraph.kt")

        legacyAppPaths.forEach { relativePath ->
            assertFalse(File(root, relativePath).exists(), "$relativePath must not remain app-owned.")
        }
        featurePaths.forEach { relativePath ->
            assertTrue(File(root, relativePath).isFile, "Missing feature-owned $relativePath.")
        }
        assertTrue(graph.contains("object CheckAssetRestrictionsMetroViewModelBindings"))
        assertTrue(graph.contains("@ViewModelKey(CheckAssetRestrictionsViewModel::class)"))
        assertTrue(graph.contains("fun checkAssetRestrictionsViewModel(): CheckAssetRestrictionsViewModel"))
        assertTrue(graph.contains("wireMetroViewModel()"))
        assertFalse(appBindings.contains("CheckAssetRestrictionsViewModel"))
        assertEquals(
            1,
            Regex("\\bCheckAssetRestrictionsMetroViewModelBindings::class\\b")
                .findAll(sessionGraph)
                .count(),
        )
    }

    private fun source(root: File, relativePath: String): String =
        File(root, relativePath).also { file ->
            assertTrue(file.isFile, "Missing ${file.path}")
        }.readText()

    private fun repositoryRoot(): File =
        generateSequence(File(requireNotNull(System.getProperty("user.dir"))).absoluteFile) { it.parentFile }
            .first { File(it, "settings.gradle.kts").isFile }

    private companion object {
        val legacyAppPaths = listOf(
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/ConversationDetailsViewModelGraph.kt",
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/media/CheckAssetRestrictionsViewModel.kt",
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/model/AssetBundle.kt",
            "app/src/main/kotlin/com/wire/android/ui/sharing/ImportedMediaAsset.kt",
        )
        val featurePaths = listOf(
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/" +
                    "CheckAssetRestrictionsViewModelGraph.kt",
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/AssetTooLargeDialogState.kt",
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/media/" +
                    "CheckAssetRestrictionsViewModel.kt",
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/model/AssetBundle.kt",
            "features/conversation/src/main/kotlin/com/wire/android/ui/sharing/ImportedMediaAsset.kt",
        )
    }
}
