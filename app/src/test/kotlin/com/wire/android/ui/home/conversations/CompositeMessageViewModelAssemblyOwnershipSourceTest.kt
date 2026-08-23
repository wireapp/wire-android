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

class CompositeMessageViewModelAssemblyOwnershipSourceTest {

    @Test
    fun compositeMessageFactoryAndResacaGatewayAreFeatureOwnedAndInstalledOnce() {
        val root = repositoryRoot()
        val oldGraph = source(
            root,
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/ScopedMessageViewModelGraph.kt",
        )
        val oldBindings = source(
            root,
            "app/src/main/kotlin/com/wire/android/di/metro/WireMetroViewModelBindings.kt",
        )
        val featureGraph = source(
            root,
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/" +
                "CompositeMessageViewModelGraph.kt",
        )
        val featureViewModel = source(
            root,
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/" +
                "CompositeMessageViewModel.kt",
        )
        val sessionGraph = source(
            root,
            "app/src/main/kotlin/com/wire/android/di/metro/AppSessionViewModelGraph.kt",
        )
        val messageTypes = source(
            root,
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/model/MessageTypes.kt",
        )

        assertFalse(
            File(
                root,
                "app/src/main/kotlin/com/wire/android/ui/home/conversations/CompositeMessageViewModel.kt",
            ).exists(),
        )
        assertFalse(
            File(
                root,
                "app/src/main/kotlin/com/wire/android/ui/home/conversations/model/CompositeMessageArgs.kt",
            ).exists(),
        )
        assertFalse(oldGraph.contains("CompositeMessage"))
        assertFalse(oldBindings.contains("CompositeMessage"))
        assertTrue(featureGraph.contains("object CompositeMessageManualViewModelFactoryGroup"))
        assertTrue(featureGraph.contains("fun compositeMessageViewModel("))
        assertTrue(featureGraph.contains("wireManualMetroViewModelScoped<"))
        assertTrue(featureGraph.contains("previewProvider = ConversationViewModelScopedPreviews"))
        assertTrue(featureViewModel.contains("CompositeMessageManualViewModelFactoryGroup::class"))
        assertTrue(featureViewModel.contains("factoryMethod = \"compositeMessageViewModel\""))
        assertEquals(
            1,
            Regex("\\bCompositeMessageManualViewModelFactoryMetroBindings::class\\b")
                .findAll(sessionGraph)
                .count(),
        )
        assertTrue(messageTypes.contains("compositeMessageViewModel("))
        assertTrue(
            messageTypes.contains("CompositeMessageArgs(conversationId, messageId)"),
            "MessageTypes must keep using the same public CompositeMessage helper and arguments.",
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
