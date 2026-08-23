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
package com.wire.android.ui.calling

import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CallingViewModelAssemblyOwnershipSourceTest {

    @Test
    fun callingAssemblyDoesNotOwnConversationListOrMeetingsViewModels() {
        val callingGroup = sourceFile("com/wire/android/ui/calling/CallingManualViewModelFactoryGroup.kt").readText()
        val callingBindings = sourceFile("com/wire/android/ui/calling/CallingMetroViewModelBindings.kt").readText()

        assertFalse(callingGroup.contains("ui.home.conversations"))
        assertFalse(callingGroup.contains("ui.home.conversationslist"))
        assertFalse(callingGroup.contains("ui.home.meetings"))
        assertFalse(callingBindings.contains("MeetingsCallViewModel"))
        assertFalse(callingBindings.contains("ConversationListCallViewModelImpl"))
        assertEquals(1, Regex("@ViewModelKey").findAll(callingBindings).count())
    }

    @Test
    fun ownerSpecificFactoriesAndBindingsStayInTheSessionGraph() {
        val conversationCall = conversationFeatureSourceFile(
            "com/wire/android/ui/home/conversations/call/ConversationCallViewModelGraph.kt",
        ).readText()
        val conversationCallViewModel = conversationFeatureSourceFile(
            "com/wire/android/ui/home/conversations/call/ConversationCallViewModel.kt",
        ).readText()
        val conversationCallAppAdapter = sourceFile(
            "com/wire/android/ui/home/conversations/call/ConversationCallViewModelAppAdapter.kt",
        ).readText()
        val conversationList = sourceFile(
            "com/wire/android/ui/home/conversationslist/ConversationListCallViewModelGraph.kt",
        ).readText()
        val meetings = sourceFile("com/wire/android/ui/home/meetings/MeetingsCallViewModelGraph.kt").readText()
        val sessionGraph = sourceFile("com/wire/android/di/metro/AppSessionViewModelGraph.kt").readText()

        assertTrue(conversationCall.contains("ConversationCallManualViewModelFactoryGroup"))
        assertTrue(conversationCallViewModel.contains("ConversationCallManualViewModelFactoryGroup::class"))
        assertTrue(conversationCall.contains("conversationCallViewModel(conversationId: ConversationId)"))
        assertFalse(conversationCall.contains("ConversationNavArgs"))
        assertFalse(conversationCallViewModel.contains("ConversationNavArgs"))
        assertTrue(conversationCallAppAdapter.contains("conversationCallViewModel(args: ConversationNavArgs)"))
        assertTrue(conversationCallAppAdapter.contains("conversationCallViewModel(args.conversationId)"))
        assertFalse(conversationCallAppAdapter.contains("@WireAssistedViewModelFactoryGroup"))
        assertTrue(conversationList.contains("instanceKey = \"call_\$conversationsSource\""))
        assertTrue(meetings.contains("fun meetingsCallViewModel(): MeetingsCallViewModel = wireMetroViewModel()"))

        listOf(
            "CallingManualViewModelFactoryMetroBindings",
            "ConversationCallManualViewModelFactoryMetroBindings",
            "CallingMetroViewModelBindings",
            "ConversationListCallMetroViewModelBindings",
            "MeetingsCallMetroViewModelBindings",
        ).forEach { bindingContainer ->
            assertEquals(1, Regex("\\b$bindingContainer::class\\b").findAll(sessionGraph).count(), bindingContainer)
        }
    }

    @Test
    fun conversationCallImplementationIsFeatureOwnedWhileRuntimeAdaptersStayInApp() {
        val repositoryRoot = repositoryRoot()

        assertFalse(
            File(
                repositoryRoot,
                "app/src/main/kotlin/com/wire/android/ui/home/conversations/call/ConversationCallViewModel.kt",
            ).exists(),
        )
        assertFalse(
            File(
                repositoryRoot,
                "app/src/main/kotlin/com/wire/android/ui/home/conversations/call/ConversationCallViewModelGraph.kt",
            ).exists(),
        )
        assertTrue(
            sourceFile("com/wire/android/ui/home/conversations/call/JoinOrStartCallRuntimeActions.kt").isFile,
        )
        assertTrue(
            sourceFile("com/wire/android/ui/home/conversations/call/JoinOrStartCallRuntimeDialogs.kt").isFile,
        )
    }

    @Test
    fun callingInstanceKeysArePreserved() {
        val callingGroup = sourceFile("com/wire/android/ui/calling/CallingManualViewModelFactoryGroup.kt").readText()

        listOf(
            "instanceKey = \"incoming_\$conversationId\"",
            "instanceKey = \"outgoing_\$conversationId\"",
            "instanceKey = \"ongoing_\$conversationId\"",
            "instanceKey = \"shared_\$conversationId\"",
        ).forEach { key -> assertTrue(callingGroup.contains(key), key) }
    }

    private fun sourceFile(relativePath: String): File =
        File(repositoryRoot(), "app/src/main/kotlin/$relativePath").also {
            assertTrue(it.isFile, "Missing ${it.path}")
        }

    private fun conversationFeatureSourceFile(relativePath: String): File =
        File(repositoryRoot(), "features/conversation/src/main/kotlin/$relativePath").also {
            assertTrue(it.isFile, "Missing ${it.path}")
        }

    private fun repositoryRoot(): File =
        generateSequence(File(requireNotNull(System.getProperty("user.dir"))).absoluteFile) { it.parentFile }
            .first { File(it, "settings.gradle.kts").isFile }
}
