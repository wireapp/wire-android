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

class MessageExpirationItemsResourceOwnershipTest {

    @Test
    fun messageExpirationItemsAreFeatureOwnedWithAnAppProvidedUnknownUserName() {
        val root = repositoryRoot()
        val featureSource = source(
            root,
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/messages/item/" +
                "MessageExpirationItems.kt",
        )
        val appCaller = source(
            root,
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/messages/item/MessageContentItem.kt",
        )

        assertFalse(
            File(
                root,
                "app/src/main/kotlin/com/wire/android/ui/home/conversations/messages/item/MessageExpirationItems.kt",
            ).exists(),
        )
        assertTrue(featureSource.contains("unknownUserName: String"))
        assertFalse(featureSource.contains("com.wire.android.R"))
        assertFalse(featureSource.contains("R.string.unknown_user_name"))
        assertEquals(
            2,
            "unknownUserName = stringResource(R.string.unknown_user_name)".countIn(appCaller),
        )
    }

    @Test
    fun expirationPresentationResourcesAreFeatureOwnedWithUnchangedQualifierCoverage() {
        assertTrue(resourceDefinitions(appResources).isEmpty())

        val definitions = resourceDefinitions(featureResources)

        assertEquals(31, definitions.size)
        assertEquals(resourceNames, definitions.map { it.name }.toSet())
        assertEquals(expectedQualifiersByName, definitions.groupBy { it.name }.mapValues { (_, values) ->
            values.map { it.qualifier }.toSet()
        })
        assertEquals(expectedDefinitionFingerprint, definitions.fingerprint())
    }

    private fun String.countIn(source: String): Int = Regex(Regex.escape(this)).findAll(source).count()

    private fun resourceDefinitions(directory: File): List<ResourceDefinition> =
        directory.walkTopDown()
            .filter { it.isFile && it.name == "strings.xml" }
            .flatMap { file ->
                resourceDefinitionPattern.findAll(file.readText()).mapNotNull { match ->
                    val name = match.groupValues[2]
                    if (name !in resourceNames) {
                        null
                    } else {
                        ResourceDefinition(
                            qualifier = requireNotNull(file.parentFile).name,
                            type = match.groupValues[1],
                            name = name,
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
        val resourceNames = setOf(
            "self_deleting_message_time_left",
            "label_information_waiting_for_recipient_timer_to_expire_group",
            "label_information_waiting_for_recipient_timer_to_expire_one_to_one",
            "label_information_waiting_for_deleation_when_self_not_sender",
        )
        val localizedQualifiers = setOf(
            "values",
            "values-de",
            "values-es",
            "values-hu",
            "values-it",
            "values-pt",
            "values-ru",
            "values-si",
        )
        val expectedQualifiersByName = mapOf(
            "self_deleting_message_time_left" to localizedQualifiers,
            "label_information_waiting_for_recipient_timer_to_expire_group" to localizedQualifiers,
            "label_information_waiting_for_recipient_timer_to_expire_one_to_one" to localizedQualifiers,
            // Spanish deliberately falls back to the default value, as it did before the move.
            "label_information_waiting_for_deleation_when_self_not_sender" to localizedQualifiers - "values-es",
        )
        const val expectedDefinitionFingerprint = "a417de7f71f5386a9e0d35daa23481b041e344145b057031c79ea36329ca55de"
    }
}
