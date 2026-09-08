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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConversationNavigation3SourceTest {

    @Test
    fun givenConversationEntry_whenRegistered_thenItRetainsRootHorizontalPresentation() {
        val source = File(conversationSourceDirectory(), "ConversationNavigation3Entries.kt").readText()

        assertTrue(
            "wireEntry<ConversationRoute>(presentation = WireEntryPresentation.Slide)" in source
        )
    }

    @Test
    fun givenConversationSources_whenCheckingNavigationDependencies_thenOnlyNavigation3ContractsAreUsed() {
        val sourceDirectory = conversationSourceDirectory()
        val forbiddenReferences = listOf(
            "com.ramcosta",
            "WireNavigator",
            "com.wire.android.navigation.NavigationCommand",
            "ResultRecipient",
            "ResultBackNavigator",
            "androidx.navigation",
            "SavedStateHandle",
            ".navArgs",
        )
        val violations = sourceDirectory
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file ->
                file.readLines()
                    .mapIndexedNotNull { index, line ->
                        forbiddenReferences
                            .firstOrNull(line::contains)
                            ?.let { reference -> "${file.relativeTo(sourceDirectory)}:${index + 1}: $reference" }
                    }
                    .asSequence()
            }
            .toList()

        assertTrue(
            violations.isEmpty(),
            "Conversation sources still contain legacy navigation references:\n${violations.joinToString("\n")}",
        )
    }

    private fun conversationSourceDirectory(): File =
        generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
            .map { File(it, "app/src/main/kotlin/com/wire/android/ui/home/conversations") }
            .firstOrNull(File::isDirectory)
            ?: error("Unable to locate the conversations production source directory")
}
