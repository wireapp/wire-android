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
import java.security.MessageDigest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SearchConversationMessagesNoResultsResourceOwnershipTest {

    @Test
    fun noResultsRendererIsFeatureOwnedWhileTheAppKeepsItsExistingCaller() {
        val root = repositoryRoot()
        val featureSource = source(
            root,
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/search/messages/" +
                "SearchConversationMessagesNoResultsScreen.kt",
        )
        val appCaller = source(
            root,
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/search/messages/" +
                "SearchConversationMessagesScreen.kt",
        )

        assertFalse(
            File(
                root,
                "app/src/main/kotlin/com/wire/android/ui/home/conversations/search/messages/" +
                    "SearchConversationMessagesNoResultsScreen.kt",
            ).exists(),
        )
        assertTrue(featureSource.contains("import com.wire.android.feature.conversation.R as conversationR"))
        assertTrue(featureSource.contains("conversationR.string.label_search_messages_no_results"))
        assertFalse(featureSource.contains("com.wire.android.R"))
        assertTrue(appCaller.contains("SearchConversationMessagesNoResultsScreen()"))
    }

    @Test
    fun noResultsStringIsFeatureOwnedWithUnchangedQualifierCoverage() {
        assertTrue(resourceDefinitions(appResources).isEmpty())

        val definitions = resourceDefinitions(featureResources)

        assertEquals(7, definitions.size)
        assertEquals(expectedQualifiers, definitions.map { it.qualifier }.toSet())
        assertEquals(expectedDefinitionFingerprint, definitions.fingerprint())
    }

    private fun resourceDefinitions(directory: File): List<ResourceDefinition> =
        directory.walkTopDown()
            .filter { it.isFile && it.name == "strings.xml" }
            .flatMap { file ->
                resourceDefinitionPattern.findAll(file.readText()).mapNotNull { match ->
                    if (match.groupValues[2] != resourceName) {
                        null
                    } else {
                        ResourceDefinition(
                            qualifier = requireNotNull(file.parentFile).name,
                            type = match.groupValues[1],
                            name = match.groupValues[2],
                            attributes = match.groupValues[3].trim(),
                            value = match.groupValues[4],
                        )
                    }
                }
            }
            .toList()

    private fun List<ResourceDefinition>.fingerprint(): String {
        val canonical = sortedBy { it.canonicalValue }.joinToString("\n") { it.canonicalValue }
        return MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray())
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    private fun source(root: File, relativePath: String): String =
        File(root, relativePath).also { file ->
            assertTrue(file.isFile, "Missing ${file.path}")
        }.readText()

    private data class ResourceDefinition(
        val qualifier: String,
        val type: String,
        val name: String,
        val attributes: String,
        val value: String,
    ) {
        val canonicalValue: String = "$qualifier|$type|$name|$attributes|$value"
    }

    private companion object {
        private fun repositoryRoot(): File =
            generateSequence(File(requireNotNull(System.getProperty("user.dir"))).absoluteFile) { it.parentFile }
                .first { File(it, "settings.gradle.kts").isFile }

        val appResources = File(repositoryRoot(), "app/src/main/res")
        val featureResources = File(repositoryRoot(), "features/conversation/src/main/res")
        val resourceDefinitionPattern = Regex(
            """<(string|plurals)\s+name="([^"]+)"([^>]*)>(.*?)</\1>""",
            setOf(RegexOption.DOT_MATCHES_ALL),
        )
        const val resourceName = "label_search_messages_no_results"
        val expectedQualifiers = setOf(
            "values",
            "values-de",
            "values-hu",
            "values-it",
            "values-pt",
            "values-ru",
            "values-si",
        )
        const val expectedDefinitionFingerprint = "8a28c866dca1523b0821475f359aaba74ef1d2daa861465f3ce2ade40fb98350"
    }
}
