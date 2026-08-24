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

class ConversationInfoViewModelAssemblyOwnershipSourceTest {

    @Test
    fun conversationInfoFactoryIsFeatureOwnedAndAdaptedByAppComposition() {
        val root = repositoryRoot()
        val coreGraph = source(
            root,
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/ConversationCoreViewModelGraph.kt",
        )
        val appAdapter = source(
            root,
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/ConversationInfoViewModelAppAdapter.kt",
        )
        val featureGraph = source(
            root,
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/" +
                "ConversationInfoViewModelGraph.kt",
        )
        val featureViewModel = source(
            root,
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/info/" +
                "ConversationInfoViewModel.kt",
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
                "app/src/main/kotlin/com/wire/android/ui/home/conversations/info/ConversationInfoViewModel.kt",
            ).exists(),
        )
        assertFalse(coreGraph.contains("conversationInfoViewModel"))
        assertFalse(coreGraph.contains("ConversationInfoViewModel"))
        assertTrue(featureGraph.contains("object ConversationInfoManualViewModelFactoryGroup"))
        assertTrue(featureGraph.contains("fun conversationInfoViewModel(args: ConversationInfoViewModelArgs)"))
        assertTrue(featureViewModel.contains("ConversationInfoManualViewModelFactoryGroup::class"))
        assertTrue(featureViewModel.contains("factoryMethod = \"conversationInfoViewModel\""))
        assertTrue(appAdapter.contains("fun conversationInfoViewModel(args: ConversationNavArgs)"))
        assertTrue(appAdapter.contains("conversationId = args.conversationId"))
        assertTrue(appAdapter.contains("import com.wire.android.feature.conversation.R as conversationR"))
        assertTrue(appAdapter.contains("UIText.StringResource(conversationR.string.member_name_deleted_label)"))
        assertFalse(featureViewModel.contains("com.wire.android.R"))
        assertFalse(featureViewModel.contains("ConversationNavArgs"))
        assertFalse(conversationModule.contains("ConversationInfoViewModel"))
        assertEquals(
            1,
            Regex("\\bConversationInfoManualViewModelFactoryMetroBindings::class\\b").findAll(sessionGraph).count(),
        )
        assertTrue(
            navigationEntries.contains("conversationInfoViewModel = conversationInfoViewModel(viewModelArgs)"),
        )
    }

    @Test
    fun currentAccountQualifierIsCoreOwnedWithoutChangingItsFqn() {
        val root = repositoryRoot()
        val coreQualifier = source(root, "core/di/src/main/kotlin/com/wire/android/di/CurrentAccount.kt")
        val oldOwner = source(root, "app/src/main/kotlin/com/wire/android/di/CoreLogicModule.kt")

        assertTrue(coreQualifier.contains("package com.wire.android.di"))
        assertTrue(coreQualifier.contains("annotation class CurrentAccount"))
        assertFalse(oldOwner.contains("annotation class CurrentAccount"))
    }

    private fun source(root: File, relativePath: String): String =
        File(root, relativePath).also { file ->
            assertTrue(file.isFile, "Missing ${file.path}")
        }.readText()

    private fun repositoryRoot(): File =
        generateSequence(File(requireNotNull(System.getProperty("user.dir"))).absoluteFile) { it.parentFile }
            .first { File(it, "settings.gradle.kts").isFile }
}
