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
import java.security.MessageDigest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RecordAudioInfoMessageResourceOwnershipTest {

    @Test
    fun recordAudioFailureStringsAreFeatureOwnedWithUnchangedQualifierCoverageAndValues() {
        assertEquals(emptyMap<String, Set<String>>(), resourceNamesByQualifier(appResources))
        assertEquals(expectedResourcesByQualifier, resourceNamesByQualifier(featureResources))
        assertEquals(expectedResourceFingerprint, resourceDefinitions(featureResources).fingerprint())
    }

    @Test
    fun recordAudioInfoMessageTypeIsFeatureOwnedAndAppConsumerUsesFeatureResources() {
        assertFalse(File(repositoryRoot(), legacySourceRelativePath).exists())

        val source = sourceFile(featureSourceRelativePath)
        assertTrue(source.contains("package com.wire.android.ui.home.messagecomposer.recordaudio"))
        assertTrue(source.contains("import com.wire.android.feature.conversation.R as conversationR"))
        resourceNames.forEach { resourceName ->
            assertTrue(source.contains("conversationR.string.$resourceName"))
        }

        val appConsumer = sourceFile(appConsumerRelativePath)
        assertTrue(appConsumer.contains("import com.wire.android.feature.conversation.R as conversationR"))
        assertTrue(appConsumer.contains("conversationR.string.record_audio_unable_due_to_ongoing_call"))
    }

    private fun resourceNamesByQualifier(resourceDirectory: File): Map<String, Set<String>> =
        resourceDefinitions(resourceDirectory)
            .groupBy { it.qualifier }
            .mapValues { (_, definitions) -> definitions.mapTo(mutableSetOf()) { it.name } }

    private fun resourceDefinitions(resourceDirectory: File): List<ResourceDefinition> =
        resourceDirectory.walkTopDown()
            .filter { it.isFile && it.name == "strings.xml" }
            .flatMap { resourceFile ->
                stringDefinition.findAll(resourceFile.readText())
                    .filter { it.groupValues[1] in resourceNames }
                    .map { match ->
                        ResourceDefinition(
                            qualifier = requireNotNull(resourceFile.parentFile).name,
                            name = match.groupValues[1],
                            attributes = match.groupValues[2],
                            value = match.groupValues[3],
                        )
                    }
            }
            .toList()

    private fun List<ResourceDefinition>.fingerprint(): String =
        sortedBy { it.canonicalValue }
            .joinToString("\n") { it.canonicalValue }
            .toByteArray()
            .sha256()

    private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(this)
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }

    private fun sourceFile(relativePath: String): String = File(repositoryRoot(), relativePath).also { source ->
        assertTrue(source.isFile, "Missing ${source.path}")
    }.readText()

    private data class ResourceDefinition(
        val qualifier: String,
        val name: String,
        val attributes: String,
        val value: String,
    ) {
        val canonicalValue = "$qualifier|$name|$attributes|$value"
    }

    private companion object {
        private fun repositoryRoot(): File =
            generateSequence(File(requireNotNull(System.getProperty("user.dir"))).absoluteFile) { it.parentFile }
                .first { File(it, "settings.gradle.kts").isFile }

        val appResources = File(repositoryRoot(), "app/src/main/res")
        val featureResources = File(repositoryRoot(), "features/conversation/src/main/res")
        val resourceNames = setOf(
            "record_audio_unable_due_to_ongoing_call",
            "record_audio_unable_due_to_error",
        )
        val expectedResourcesByQualifier = mapOf(
            "values" to resourceNames,
            "values-de" to resourceNames,
            "values-hu" to resourceNames,
            "values-it" to setOf("record_audio_unable_due_to_error"),
            "values-pt" to resourceNames,
            "values-ru" to resourceNames,
            "values-si" to resourceNames,
            "values-sv" to setOf("record_audio_unable_due_to_ongoing_call"),
        )
        val stringDefinition = Regex(
            """<string\s+name="(record_audio_unable_due_to_(?:ongoing_call|error))"([^>]*)>(.*?)</string>""",
            setOf(RegexOption.DOT_MATCHES_ALL),
        )
        const val expectedResourceFingerprint = "a099f88c23e55d459da31570a121321612138031267009a23e9c5d3790d4b530"
        const val featureSourceRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/messagecomposer/recordaudio/RecordAudioInfoMessageType.kt"
        const val legacySourceRelativePath =
            "app/src/main/kotlin/com/wire/android/ui/home/messagecomposer/recordaudio/RecordAudioInfoMessageType.kt"
        const val appConsumerRelativePath =
            "app/src/main/kotlin/com/wire/android/ui/home/messagecomposer/EnabledMessageComposer.kt"
    }
}
