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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.wire.android.feature.conversation

import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConversationReactionPresentationResourceOwnershipTest {

    @Test
    fun reactionAccessibilityStringsAreFeatureOwnedWithUnchangedLocalizedValues() {
        reactionStrings.forEach { (resourceName, expectedValues) ->
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

                assertEquals("", definition.attributes.trim(), "$resourceName must preserve its attributes.")
                assertEquals(expectedValue, definition.value, "$resourceName has an unexpected value in $qualifier.")
            }
        }
    }

    @Test
    fun reactionRenderersRemainFeatureOwnedAndUseFeatureResources() {
        assertFalse(File(repositoryRoot(), legacyMessageReactionsItemRelativePath).exists())
        assertFalse(File(repositoryRoot(), legacyReactionPillRelativePath).exists())

        val messageReactionsItem = sourceFile(messageReactionsItemRelativePath)
        val reactionPill = sourceFile(reactionPillRelativePath)

        assertTrue(messageReactionsItem.contains("package com.wire.android.ui.home.conversations.messages.item"))
        assertTrue(reactionPill.contains("package com.wire.android.ui.home.conversations.messages"))
        assertFalse(reactionPill.contains("com.wire.android.R"))
        reactionStrings.keys.forEach { resourceName ->
            assertTrue(reactionPill.contains("conversationR.string.$resourceName"))
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

        const val messageReactionsItemRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/messages/item/MessageReactionsItem.kt"
        const val reactionPillRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/messages/ReactionPill.kt"
        const val legacyMessageReactionsItemRelativePath =
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/messages/item/MessageReactionsItem.kt"
        const val legacyReactionPillRelativePath =
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/messages/ReactionPill.kt"
        val appResources = File(repositoryRoot(), "app/src/main/res")
        val featureResources = File(repositoryRoot(), "features/conversation/src/main/res")
        val reactionStrings = mapOf(
            "content_description_add_this_reaction" to mapOf(
                "values" to "Double tap to add this reaction",
                "values-de" to "Doppeltippen, um diese Reaktion hinzuzufügen",
                "values-ru" to "Нажмите дважды, чтобы добавить эту реакцию",
            ),
            "content_description_remove_your_reaction" to mapOf(
                "values" to "Double tap to remove reaction",
                "values-de" to "Doppeltippen, um die Reaktion zu entfernen",
                "values-ru" to "Нажмите дважды, чтобы удалить реакцию",
            ),
        )
    }
}
