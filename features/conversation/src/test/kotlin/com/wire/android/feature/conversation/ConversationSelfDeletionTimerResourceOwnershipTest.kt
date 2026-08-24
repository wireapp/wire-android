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

class ConversationSelfDeletionTimerResourceOwnershipTest {

    @Test
    fun selfDeletionTimerPluralsAreFeatureOwnedWithTheOriginalQualifierCoverage() {
        assertEquals(
            emptyMap<String, Set<String>>(),
            pluralResourceNamesByQualifier(appResources),
            "The timer plurals must not remain in :app resources.",
        )
        assertEquals(
            expectedPluralResources,
            pluralResourceNamesByQualifier(featureResources),
            "The timer plurals must preserve their original qualifier coverage.",
        )
        assertEquals(65, pluralResourceNamesByQualifier(featureResources).values.sumOf { it.size })
    }

    @Test
    fun selfDeletionTimerStateIsFeatureOwnedAndUsesFeatureResources() {
        assertFalse(File(repositoryRoot(), legacyMessageExpirationRelativePath).exists())

        val source = sourceFile(messageExpirationRelativePath)
        assertTrue(source.contains("package com.wire.android.ui.home.conversations"))
        assertTrue(source.contains("import com.wire.android.feature.conversation.R as conversationR"))
        pluralResourceNames.forEach { resourceName ->
            assertTrue(source.contains("conversationR.plurals.$resourceName"))
        }
    }

    private fun pluralResourceNamesByQualifier(resourceDirectory: File): Map<String, Set<String>> =
        resourceDirectory.walkTopDown()
            .filter { it.isFile && it.name == "strings.xml" }
            .mapNotNull { resourceFile ->
                val names = pluralName.findAll(resourceFile.readText())
                    .map { it.groupValues[1] }
                    .toSet()
                resourceFile.parentFile?.name?.takeIf { names.isNotEmpty() }?.let { it to names }
            }
            .toMap()

    private fun sourceFile(relativePath: String): String = File(repositoryRoot(), relativePath).also { file ->
        assertTrue(file.isFile, "Missing ${file.path}")
    }.readText()

    private companion object {
        private fun repositoryRoot(): File =
            generateSequence(File(requireNotNull(System.getProperty("user.dir"))).absoluteFile) { it.parentFile }
                .first { File(it, "settings.gradle.kts").isFile }

        val pluralName = Regex("""<plurals\s+name="((?:weeks|days|hours|minutes|seconds)(?:_left)?)"""")
        val pluralResourceNames = setOf(
            "weeks",
            "days",
            "hours",
            "minutes",
            "seconds",
            "weeks_left",
            "days_left",
            "hours_left",
            "minutes_left",
            "seconds_left",
        )
        val leftPluralResourceNames = pluralResourceNames.filterTo(mutableSetOf()) { it.endsWith("_left") }
        val expectedPluralResources = mapOf(
            "values" to pluralResourceNames,
            "values-de" to pluralResourceNames,
            "values-ru" to pluralResourceNames,
            "values-es" to leftPluralResourceNames,
            "values-et" to leftPluralResourceNames,
            "values-hu" to leftPluralResourceNames,
            "values-it" to leftPluralResourceNames,
            "values-pt" to leftPluralResourceNames,
            "values-si" to leftPluralResourceNames,
            "values-sv" to leftPluralResourceNames,
        )
        const val messageExpirationRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/MessageExpiration.kt"
        const val legacyMessageExpirationRelativePath =
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/MessageExpiration.kt"
        val appResources = File(repositoryRoot(), "app/src/main/res")
        val featureResources = File(repositoryRoot(), "features/conversation/src/main/res")
    }
}
