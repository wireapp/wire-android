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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PasswordProtectedLinkBannerOwnershipTest {

    @Test
    fun passwordProtectedLinkBannerAndItsLocalizedStringsAreFeatureOwned() {
        assertFalse(File(root, legacyBannerRelativePath).exists())

        val banner = source(featureBannerRelativePath)
        assertTrue(banner.contains("package com.wire.android.ui.home.conversations.details.editguestaccess"))
        assertTrue(banner.contains("import com.wire.android.feature.conversation.R as conversationR"))
        assertFalse(banner.contains("com.wire.android.R"))
        resourceNames.forEach { resourceName ->
            assertTrue(
                banner.contains("conversationR.string.$resourceName"),
                "$resourceName must use the feature resource namespace.",
            )
        }

        resourceNames.forEach { resourceName ->
            assertTrue(qualifiersContaining(appResources, resourceName).isEmpty(), "$resourceName must not remain in :app.")
            assertTrue(qualifiersContaining(featureResources, resourceName).isNotEmpty(), "$resourceName must be feature-owned.")
        }
        assertEquals(expectedResourceCountsByQualifier, resourceNamesByQualifier(featureResources))
    }

    private fun qualifiersContaining(resourceDirectory: File, resourceName: String): Set<String> =
        resourceNamesByQualifier(resourceDirectory)
            .filterValues { resourceName in it }
            .keys

    private fun resourceNamesByQualifier(resourceDirectory: File): Map<String, Set<String>> =
        resourceDirectory.walkTopDown()
            .filter { it.isFile && it.name == "strings.xml" }
            .mapNotNull { resourceFile ->
                val names = stringName.findAll(resourceFile.readText())
                    .map { it.groupValues[1] }
                    .filterTo(mutableSetOf()) { it in resourceNames }
                resourceFile.parentFile?.name?.takeIf { names.isNotEmpty() }?.let { it to names }
            }
            .toMap()

    private fun source(relativePath: String): String = File(root, relativePath).also { file ->
        assertTrue(file.isFile, "Missing ${file.path}")
    }.readText()

    private companion object {
        val root = generateSequence(File(requireNotNull(System.getProperty("user.dir"))).absoluteFile) { it.parentFile }
            .first { File(it, "settings.gradle.kts").isFile }
        val appResources = File(root, "app/src/main/res")
        val featureResources = File(root, "features/conversation/src/main/res")
        val stringName = Regex("""<string\s+name="([^"]+)"""")
        val resourceNames = setOf(
            "password_protected_link_banner_description",
            "password_protected_link_banner_title",
        )
        val expectedResourceCountsByQualifier = mapOf(
            "values" to resourceNames,
            "values-de" to resourceNames,
            "values-hr" to setOf("password_protected_link_banner_description"),
            "values-hu" to resourceNames,
            "values-it" to resourceNames,
            "values-pt" to resourceNames,
            "values-ru" to resourceNames,
            "values-si" to resourceNames,
        )
        const val featureBannerRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/editguestaccess/PasswordProtectedLinkBanner.kt"
        const val legacyBannerRelativePath =
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/details/editguestaccess/PasswordProtectedLinkBanner.kt"
    }
}
