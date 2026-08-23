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

package com.wire.android.feature.conversation

import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConversationParticipantRendererResourceOwnershipTest {

    @Test
    fun participantSectionStringsAreFeatureOwnedWithFullLocalizedCoverage() {
        participantSectionStrings.forEach { (resourceName, expectedValues) ->
            assertTrue(
                qualifiersContaining(appResources, resourceName).isEmpty(),
                "$resourceName must not remain in :app resources.",
            )
            assertEquals(
                expectedValues.keys,
                qualifiersContaining(featureResources, resourceName),
                "$resourceName must preserve its qualifier coverage.",
            )
            expectedValues.forEach { (qualifier, expectedValue) ->
                val definition = stringDefinition(featureResources, qualifier, resourceName)

                assertEquals("", definition.attributes.trim(), "$resourceName must preserve its translatable semantics.")
                assertEquals(expectedValue, definition.value, "$resourceName has an unexpected value in $qualifier.")
            }
        }
    }

    @Test
    fun renderersUseFeatureAndNeutralResourceNamespacesOnly() {
        val participantItem = sourceFile(conversationParticipantItemRelativePath)
        val participantList = sourceFile(groupConversationParticipantListRelativePath)

        assertFalse(participantItem.contains("com.wire.android.R"))
        assertFalse(participantList.contains("com.wire.android.R"))
        assertFalse(participantItem.contains("BuildConfig"))
        assertFalse(participantList.contains("BuildConfig"))
        assertTrue(participantItem.contains("commonR.string.conversation_participant_you_label"))
        assertTrue(participantItem.contains("commonR.string.temporary_user_label"))
        assertTrue(participantItem.contains("commonR.string.content_description_empty"))
        participantSectionStrings.keys.forEach { resourceName ->
            assertTrue(
                participantList.contains("conversationR.string.$resourceName"),
                "GroupConversationParticipantList must use the feature-owned $resourceName resource.",
            )
        }
    }

    private fun qualifiersContaining(resourceDirectory: File, resourceName: String): Set<String> =
        resourceDirectory.walkTopDown()
            .filter { it.isFile && it.name == "strings.xml" }
            .filter { it.readText().contains("name=\"$resourceName\"") }
            .map { requireNotNull(it.parentFile).name }
            .toSet()

    private fun stringDefinition(resourceDirectory: File, qualifier: String, resourceName: String): StringDefinition {
        val file = File(resourceDirectory, "$qualifier/strings.xml")
        assertTrue(file.isFile, "Missing ${file.path}")
        val match = Regex(
            """<string\s+name=\"$resourceName\"([^>]*)>(.*?)</string>""",
            setOf(RegexOption.DOT_MATCHES_ALL),
        ).find(file.readText())

        assertTrue(match != null, "Missing $resourceName in ${file.path}")
        return StringDefinition(
            attributes = requireNotNull(match).groupValues[1],
            value = match.groupValues[2],
        )
    }

    private fun sourceFile(relativePath: String): String = File(repositoryRoot(), relativePath).also { file ->
        assertTrue(file.isFile, "Missing ${file.path}")
    }.readText()

    private data class StringDefinition(
        val attributes: String,
        val value: String,
    )

    private companion object {
        private fun repositoryRoot(): File =
            generateSequence(File(requireNotNull(System.getProperty("user.dir"))).absoluteFile) { it.parentFile }
                .first { File(it, "settings.gradle.kts").isFile }

        const val conversationParticipantItemRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/participants/ConversationParticipantItem.kt"
        const val groupConversationParticipantListRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/participants/GroupConversationParticipantList.kt"
        val appResources = File(repositoryRoot(), "app/src/main/res")
        val featureResources = File(repositoryRoot(), "features/conversation/src/main/res")
        val participantSectionStrings = mapOf(
            "conversation_details_conversation_admins" to mapOf(
                "values" to "ADMINS (%d)",
                "values-de" to "ADMINS (%d)",
                "values-es" to "ADMINISTRADORES (%d)",
                "values-ru" to "АДМИНИСТРАТОРЫ (%d)",
            ),
            "conversation_details_conversation_members" to mapOf(
                "values" to "MEMBERS (%d)",
                "values-de" to "MITGLIEDER (%d)",
                "values-es" to "MIEMBROS (%d)",
                "values-ru" to "УЧАСТНИКИ (%d)",
            ),
            "conversation_details_conversation_apps" to mapOf(
                "values" to "APPS (%d)",
                "values-de" to "APPS (%d)",
                "values-es" to "APLICACIONES (%d)",
                "values-ru" to "ПРИЛОЖЕНИЯ (%d)",
            ),
        )
    }
}
