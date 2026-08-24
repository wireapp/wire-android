/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.feature.conversation

import java.io.File
import java.security.MessageDigest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EmptyMediaContentScreenOwnershipTest {

    @Test
    fun emptyMediaContentScreenIsFeatureOwnedAndUsesFeatureResources() {
        assertFalse(File(root, legacySourceRelativePath).exists())

        val source = source(featureSourceRelativePath)

        assertTrue(source.contains("package com.wire.android.ui.home.conversations.media"))
        assertTrue(source.contains("fun EmptyMediaContentScreen("))
        assertTrue(source.contains("import com.wire.android.feature.conversation.R as conversationR"))
        assertTrue(source.contains("import com.wire.android.ui.common.preview.MultipleThemePreviews"))
        assertFalse(source.contains("com.wire.android.R"))
        assertFalse(source.contains("PreviewMultipleThemes"))
        resourceNames.forEach { resourceName ->
            assertTrue(source.contains("conversationR.string.$resourceName"))
        }
    }

    @Test
    fun emptyMediaStringsAreFeatureOwnedWithExactOriginalFingerprintAndQualifiers() {
        resourceNames.forEach { resourceName ->
            assertTrue(
                definitions(appResources, resourceName).isEmpty(),
                "$resourceName must not remain in :app resources.",
            )
        }

        val definitions = resourceNames.flatMap { resourceName -> definitions(featureResources, resourceName) }

        assertEquals(14, definitions.size)
        assertEquals(expectedQualifiers, definitions.map { it.qualifier }.toSet())
        assertEquals(expectedFingerprint, definitions.fingerprint())
    }

    private fun definitions(resourceDirectory: File, resourceName: String): List<ResourceDefinition> =
        resourceDirectory.walkTopDown()
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

    private fun source(relativePath: String): String = File(root, relativePath).also { file ->
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
        val root = generateSequence(File(requireNotNull(System.getProperty("user.dir"))).absoluteFile) { it.parentFile }
            .first { File(it, "settings.gradle.kts").isFile }
        val appResources = File(root, "app/src/main/res")
        val featureResources = File(root, "features/conversation/src/main/res")
        val resourceDefinitionPattern = Regex(
            """<(string|plurals)\s+name="([^"]+)"([^>]*)>(.*?)</\1>""",
            setOf(RegexOption.DOT_MATCHES_ALL),
        )
        val resourceNames = setOf(
            "label_conversation_files_empty",
            "label_conversation_pictures_empty",
        )
        val expectedQualifiers = setOf(
            "values",
            "values-de",
            "values-hu",
            "values-it",
            "values-pt",
            "values-ru",
            "values-si",
        )
        const val expectedFingerprint = "f68647d90cee21a12b9b8bbb8b2e6a6939a9079971fb498f92ce39975ab82eac"
        const val legacySourceRelativePath =
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/media/EmptyMediaContentScreen.kt"
        const val featureSourceRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/media/EmptyMediaContentScreen.kt"
    }
}
