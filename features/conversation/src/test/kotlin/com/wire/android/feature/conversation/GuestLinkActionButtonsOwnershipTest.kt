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

class GuestLinkActionButtonsOwnershipTest {

    @Test
    fun guestLinkActionButtonsAreFeatureOwnedWhileTheAppKeepsItsCaller() {
        assertFalse(File(root, legacyButtonsRelativePath).exists())
        assertFalse(File(root, legacyActionButtonsRelativePath).exists())

        listOf(featureButtonsRelativePath, featureActionButtonsRelativePath).forEach { relativePath ->
            val source = source(relativePath)
            assertTrue(source.contains("package com.wire.android.ui.home.conversations.details.editguestaccess"))
            assertFalse(source.contains("com.wire.android.R"))
        }

        val buttons = source(featureButtonsRelativePath)
        assertTrue(buttons.contains("import com.wire.android.feature.conversation.R as conversationR"))
        resourceNames.forEach { resourceName ->
            assertTrue(buttons.contains("conversationR.string.$resourceName"))
        }
        assertTrue(source(appCallerRelativePath).contains("GuestLinkActionButtons("))
    }

    @Test
    fun guestLinkActionButtonStringsAreFeatureOwnedWithExactOriginalFingerprint() {
        assertTrue(resourceDefinitions(appResources).isEmpty())

        val definitions = resourceDefinitions(featureResources)

        assertEquals(45, definitions.size)
        assertEquals(expectedQualifiers, definitions.map { it.qualifier }.toSet())
        assertEquals(expectedDefinitionFingerprint, definitions.fingerprint())
    }

    private fun resourceDefinitions(directory: File): List<ResourceDefinition> =
        directory.walkTopDown()
            .filter { it.isFile && it.name == "strings.xml" }
            .flatMap { file ->
                resourceDefinitionPattern.findAll(file.readText()).mapNotNull { match ->
                    if (match.groupValues[2] !in resourceNames) {
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
        val resourceNames = setOf(
            "guest_link_button_create_link",
            "guest_link_button_copy_link",
            "guest_link_button_share_link",
            "guest_link_button_revoke_link",
        )
        val resourceDefinitionPattern = Regex(
            """<(string|plurals)\s+name="([^"]+)"([^>]*)>(.*?)</\1>""",
            setOf(RegexOption.DOT_MATCHES_ALL),
        )
        val expectedQualifiers = setOf(
            "values",
            "values-de",
            "values-es",
            "values-et",
            "values-hr",
            "values-hu",
            "values-it",
            "values-pl",
            "values-pt",
            "values-ru",
            "values-si",
            "values-sv",
        )
        const val expectedDefinitionFingerprint = "a969be8e9e1dab654ee47c79c6e1a59b4b2e6a3bb288c7cf7e18db4ea6c537a4"
        const val featureButtonsRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/editguestaccess/Buttons.kt"
        const val featureActionButtonsRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/editguestaccess/GuestLinkActionButtons.kt"
        const val legacyButtonsRelativePath =
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/details/editguestaccess/Buttons.kt"
        const val legacyActionButtonsRelativePath =
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/details/editguestaccess/GuestLinkActionButtons.kt"
        const val appCallerRelativePath =
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/details/editguestaccess/EditGuestAccessScreen.kt"
    }
}
