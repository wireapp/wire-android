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

class ConversationBannerViewModelAssemblyOwnershipSourceTest {

    @Test
    fun conversationBannerFactoryIsFeatureOwnedAndAdaptedByAppComposition() {
        val root = repositoryRoot()
        val coreGraph = source(
            root,
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/ConversationCoreViewModelGraph.kt",
        )
        val appAdapter = source(
            root,
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/ConversationBannerViewModelAppAdapter.kt",
        )
        val featureGraph = source(
            root,
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/" +
                "ConversationBannerViewModelGraph.kt",
        )
        val sessionGraph = source(root, "app/src/main/kotlin/com/wire/android/di/metro/AppSessionViewModelGraph.kt")
        val navigationEntries = source(
            root,
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/ConversationNavigation3Entries.kt",
        )
        val conversationScreen = source(
            root,
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/ConversationScreen.kt",
        )
        val bannerComposable = source(
            root,
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/banner/ConversationBanner.kt",
        )

        assertFalse(
            File(
                root,
                "app/src/main/kotlin/com/wire/android/ui/home/conversations/banner/ConversationBannerViewModel.kt",
            ).exists(),
        )
        assertFalse(
            File(
                root,
                "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/banner/ConversationBanner.kt",
            ).exists(),
        )
        assertFalse(coreGraph.contains("conversationBannerViewModel"))
        assertFalse(coreGraph.contains("ConversationBannerViewModel"))

        assertTrue(appAdapter.contains("fun conversationBannerViewModel(args: ConversationNavArgs)"))
        assertTrue(appAdapter.contains("conversationBannerViewModel(args.conversationId)"))
        assertEquals(
            setOf("conversationId"),
            Regex("""\bargs\.([A-Za-z0-9_]+)""")
                .findAll(appAdapter)
                .map { it.groupValues[1] }
                .toSet(),
            "The app adapter must project only ConversationId from ConversationNavArgs.",
        )
        assertFalse(appAdapter.contains("wireAssistedMetroViewModel"))
        assertFalse(appAdapter.contains("com.wire.android.R"))

        assertTrue(featureGraph.contains("object ConversationBannerManualViewModelFactoryGroup"))
        assertTrue(featureGraph.contains("fun conversationBannerViewModel(): ConversationBannerViewModel"))
        assertTrue(featureGraph.contains("fun conversationBannerViewModel(conversationId: ConversationId)"))
        assertTrue(featureGraph.contains("ConversationBannerManualViewModelFactory"))
        assertFalse(featureGraph.contains("ConversationNavArgs"))
        assertFalse(featureGraph.contains("ConversationCoreManualViewModelFactory"))
        assertEquals(
            1,
            Regex("\\bConversationBannerManualViewModelFactoryMetroBindings::class\\b")
                .findAll(sessionGraph)
                .count(),
        )
        assertTrue(
            navigationEntries.contains("conversationBannerViewModel = conversationBannerViewModel(viewModelArgs)"),
        )

        val appSpanLabelIds = Regex(
            """R\.string\.(conversation_banner_(?:federated|externals|guests|services))\b""",
        ).findAll(conversationScreen).map { it.groupValues[1] }.toList()
        assertEquals(
            setOf(
                "conversation_banner_federated",
                "conversation_banner_externals",
                "conversation_banner_guests",
                "conversation_banner_services",
            ),
            appSpanLabelIds.toSet(),
        )
        assertEquals(4, appSpanLabelIds.size)
        assertTrue(conversationScreen.contains("ConversationBanner("))
        assertTrue(bannerComposable.contains("fun ConversationBanner("))
    }

    private fun source(root: File, relativePath: String): String =
        File(root, relativePath).also { file ->
            assertTrue(file.isFile, "Missing ${file.path}")
        }.readText()

    private fun repositoryRoot(): File =
        generateSequence(File(requireNotNull(System.getProperty("user.dir"))).absoluteFile) { it.parentFile }
            .first { File(it, "settings.gradle.kts").isFile }
}
