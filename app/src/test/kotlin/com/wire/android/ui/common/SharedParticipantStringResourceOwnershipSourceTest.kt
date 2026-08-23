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

package com.wire.android.ui.common

import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SharedParticipantStringResourceOwnershipSourceTest {

    @Test
    fun sharedParticipantStringsAreOwnedByCoreUiCommonWithTheirFullLocalizedCoverage() {
        assertResourceOwnership("conversation_participant_you_label", expectedYouLabels)
        assertResourceOwnership("temporary_user_label", expectedTemporaryUserLabels)
    }

    @Test
    fun independentConsumersUseTheNeutralCoreUiCommonResources() {
        val conversationParticipantItem = sourceFile(
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/participants/ConversationParticipantItem.kt",
        )
        val callingParticipantItem = sourceFile(
            "app/src/main/kotlin/com/wire/android/ui/calling/ongoing/participantslist/ParticipantItem.kt",
        )
        val userProfileInfo = sourceFile(
            "app/src/main/kotlin/com/wire/android/ui/userprofile/common/UserProfileInfo.kt",
        )

        assertTrue(conversationParticipantItem.contains("commonR.string.conversation_participant_you_label"))
        assertTrue(conversationParticipantItem.contains("commonR.string.temporary_user_label"))
        assertTrue(conversationParticipantItem.contains("commonR.string.content_description_empty"))
        assertTrue(callingParticipantItem.contains("commonR.string.conversation_participant_you_label"))
        assertTrue(userProfileInfo.contains("commonR.string.temporary_user_label"))
    }

    private fun assertResourceOwnership(resourceName: String, expectedValues: Map<String, String>) {
        val appQualifiers = qualifiersContaining(resourceDirectory("app"), resourceName)
        val coreQualifiers = qualifiersContaining(resourceDirectory("core/ui-common"), resourceName)

        assertTrue(appQualifiers.isEmpty(), "$resourceName must not remain in :app resources.")
        assertEquals(expectedValues.keys, coreQualifiers, "$resourceName must preserve its qualifier coverage.")
        expectedValues.forEach { (qualifier, expectedValue) ->
            val definition = stringDefinition(resourceDirectory("core/ui-common"), qualifier, resourceName)

            assertEquals("", definition.attributes.trim(), "$resourceName must preserve its translatable semantics.")
            assertEquals(expectedValue, definition.value, "$resourceName has an unexpected value in $qualifier.")
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

    private fun sourceFile(relativePath: String): String =
        File(repositoryRoot(), relativePath).also { file ->
            assertTrue(file.isFile, "Missing ${file.path}")
        }.readText()

    private fun resourceDirectory(module: String): File = File(repositoryRoot(), "$module/src/main/res")

    private fun repositoryRoot(): File =
        generateSequence(File(requireNotNull(System.getProperty("user.dir"))).absoluteFile) { it.parentFile }
            .first { File(it, "settings.gradle.kts").isFile }

    private data class StringDefinition(
        val attributes: String,
        val value: String,
    )

    private companion object {
        val expectedYouLabels = mapOf(
            "values" to "(You)",
            "values-de" to "(Sie)",
            "values-es" to "(Tú)",
            "values-fr" to "(Vous)",
            "values-hr" to "(Vi)",
            "values-hu" to "(Ön)",
            "values-it" to "(Tu)",
            "values-pl" to "(Ty)",
            "values-pt" to "(Você)",
            "values-ru" to "(Вы)",
            "values-si" to "(ඔබ)",
            "values-sv" to "(Du)",
        )
        val expectedTemporaryUserLabels = mapOf(
            "values" to "%s left",
            "values-de" to "%s hat die Unterhaltung verlassen",
            "values-hu" to "%s kilépett",
            "values-pt" to "%s restante(s)",
            "values-ru" to "%s покинул(-а)",
            "values-si" to "%s ඉතිරියි",
        )
    }
}
