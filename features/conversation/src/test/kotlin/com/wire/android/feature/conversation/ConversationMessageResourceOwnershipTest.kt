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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConversationMessageResourceOwnershipTest {

    @Test
    fun messagePresentationStringsAreFeatureOwnedWithUnchangedQualifierCoverage() {
        assertTrue(
            resourceDefinitions(appResources).isEmpty(),
            "Message-presentation strings must not remain in :app.",
        )

        val definitions = resourceDefinitions(featureResources)

        assertEquals(211, definitions.size)
        assertEquals(expectedQualifierCoverage, definitions.groupBy({ it.name }, { it.qualifier }).mapValues { it.value.toSet() })
        assertEquals(expectedDefinitionFingerprint, definitions.fingerprint())
    }

    @Test
    fun appConsumersUseTheConversationResourceNamespace() {
        val consumers = appSourceRoots
            .asSequence()
            .flatMap { it.walkTopDown().asSequence() }
            .filter { it.isFile && it.extension == "kt" }
            .filter { source -> messageResourceReferencePattern.containsMatchIn(source.readText()) }
            .map { it.relativeTo(repositoryRoot()).invariantSeparatorsPath }
            .toSet()

        assertEquals(expectedConsumers, consumers)
        consumers.forEach { relativePath ->
            val source = File(repositoryRoot(), relativePath).readText()
            val references = messageResourceReferencePattern.findAll(source).toList()

            assertTrue(source.contains("import com.wire.android.feature.conversation.R as conversationR"))
            assertTrue(references.isNotEmpty())
            references.forEach { reference ->
                assertEquals(
                    "conversationR",
                    reference.groupValues[1],
                    "$relativePath must resolve ${reference.groupValues[2]} from feature R.",
                )
            }
        }
    }

    private fun resourceDefinitions(directory: File): List<ResourceDefinition> =
        directory.walkTopDown()
            .filter { it.isFile && it.name == "strings.xml" }
            .flatMap { file ->
                stringDefinitionPattern.findAll(file.readText()).mapNotNull { match ->
                    val name = match.groupValues[1]
                    if (name !in messageResourceNames) {
                        null
                    } else {
                        ResourceDefinition(
                            qualifier = requireNotNull(file.parentFile).name,
                            name = name,
                            attributes = match.groupValues[2].trim(),
                            value = match.groupValues[3],
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

    private data class ResourceDefinition(
        val qualifier: String,
        val name: String,
        val attributes: String,
        val value: String,
    ) {
        val canonicalValue: String = "$qualifier|$name|$attributes|$value"
    }

    private companion object {
        private fun repositoryRoot(): File =
            generateSequence(File(requireNotNull(System.getProperty("user.dir"))).absoluteFile) { it.parentFile }
                .first { File(it, "settings.gradle.kts").isFile }

        val appResources = File(repositoryRoot(), "app/src/main/res")
        val featureResources = File(repositoryRoot(), "features/conversation/src/main/res")
        val appSourceRoots = listOf(
            File(repositoryRoot(), "app/src/main/kotlin"),
            File(repositoryRoot(), "app/src/test/kotlin"),
        )
        val stringDefinitionPattern = Regex("""<string\s+name="([^"]+)"([^>]*)>(.*?)</string>""")
        val messageResourceNames = setOf(
            "label_message_edit_sent_failure",
            "label_message_sent_failure",
            "label_message_edit_sent_remotely_failure",
            "label_message_sent_remotely_failure",
            "label_message_decryption_failure_message_with_error_code",
            "label_message_decryption_failure_message",
            "deleted_message_text",
            "label_message_status_edited_with_date",
            "url_maps_location_coordinates_fallback",
            "member_name_deleted_label",
            "member_name_you_label_lowercase",
            "member_name_you_label_titlecase",
            "sent_a_message_with_content",
            "label_system_message_receipt_mode_on",
            "label_system_message_receipt_mode_off",
            "sent_a_message_with_unknown_content",
            "label_quote_original_message_date",
        )
        val messageResourceReferencePattern = Regex(
            """([A-Za-z0-9_]*R)\.string\.(label_message_edit_sent_failure|label_message_sent_failure|""" +
                """label_message_edit_sent_remotely_failure|label_message_sent_remotely_failure|""" +
                """label_message_decryption_failure_message_with_error_code|""" +
                """label_message_decryption_failure_message|deleted_message_text|""" +
                """label_message_status_edited_with_date|url_maps_location_coordinates_fallback|""" +
                """member_name_deleted_label|member_name_you_label_lowercase|member_name_you_label_titlecase|""" +
                """sent_a_message_with_content|label_system_message_receipt_mode_on|""" +
                """label_system_message_receipt_mode_off|sent_a_message_with_unknown_content|""" +
                """label_quote_original_message_date)\b""",
        )
        const val expectedDefinitionFingerprint = "dfb6b3a5b81f7db207ea6121129c90bd858f78b16404021dc6b326d21aae216a"
        val expectedQualifierCoverage = mapOf(
            "label_message_edit_sent_failure" to setOf(
                "values", "values-cs", "values-de", "values-es", "values-fr", "values-hr", "values-hu", "values-it",
                "values-ja", "values-pl", "values-pt", "values-ru", "values-si", "values-sv", "values-tr",
            ),
            "label_message_sent_failure" to setOf(
                "values", "values-ar", "values-cs", "values-da", "values-de", "values-el", "values-es", "values-fi",
                "values-fr", "values-hr", "values-hu", "values-it", "values-ja", "values-nl", "values-pl", "values-pt",
                "values-ro", "values-ru", "values-si", "values-sr", "values-sv", "values-tr", "values-uk",
            ),
            "label_message_edit_sent_remotely_failure" to setOf(
                "values", "values-cs", "values-de", "values-es", "values-fr", "values-hr", "values-hu", "values-it",
                "values-ja", "values-pt", "values-ru", "values-si", "values-tr",
            ),
            "label_message_sent_remotely_failure" to setOf(
                "values", "values-cs", "values-de", "values-es", "values-fr", "values-hr", "values-hu", "values-it",
                "values-ja", "values-nl", "values-pt", "values-ru", "values-si", "values-tr",
            ),
            "label_message_decryption_failure_message_with_error_code" to setOf(
                "values", "values-de", "values-es", "values-fr", "values-hu", "values-pt", "values-ru", "values-si",
                "values-tr",
            ),
            "label_message_decryption_failure_message" to setOf(
                "values", "values-cs", "values-de", "values-es", "values-et", "values-fr", "values-hr", "values-hu",
                "values-it", "values-ja", "values-pl", "values-pt", "values-ru", "values-si", "values-sv", "values-tr",
            ),
            "deleted_message_text" to setOf(
                "values", "values-de", "values-es", "values-fr", "values-hr", "values-hu", "values-it", "values-ja",
                "values-pl", "values-pt", "values-ru", "values-si",
            ),
            "label_message_status_edited_with_date" to setOf(
                "values", "values-de", "values-es", "values-et", "values-fr", "values-hr", "values-hu", "values-it",
                "values-ja", "values-lt", "values-pl", "values-pt", "values-ru", "values-si", "values-sv", "values-tr",
                "values-uk",
            ),
            "url_maps_location_coordinates_fallback" to setOf("values"),
            "member_name_deleted_label" to setOf(
                "values", "values-de", "values-es", "values-fr", "values-hr", "values-hu", "values-it", "values-pl",
                "values-pt", "values-ru", "values-si",
            ),
            "member_name_you_label_lowercase" to setOf(
                "values", "values-de", "values-es", "values-fr", "values-hr", "values-hu", "values-it", "values-pl",
                "values-pt", "values-ru", "values-si", "values-sv",
            ),
            "member_name_you_label_titlecase" to setOf(
                "values", "values-de", "values-es", "values-fr", "values-hr", "values-hu", "values-it", "values-pl",
                "values-pt", "values-ru", "values-si", "values-sv",
            ),
            "sent_a_message_with_content" to setOf(
                "values", "values-de", "values-es", "values-fr", "values-hr", "values-hu", "values-it", "values-pl",
                "values-pt", "values-ru", "values-si", "values-sv",
            ),
            "label_system_message_receipt_mode_on" to setOf(
                "values", "values-de", "values-es", "values-et", "values-hu", "values-it", "values-pl", "values-pt",
                "values-ru", "values-si",
            ),
            "label_system_message_receipt_mode_off" to setOf(
                "values", "values-de", "values-es", "values-et", "values-hu", "values-it", "values-pl", "values-pt",
                "values-ru", "values-si",
            ),
            "sent_a_message_with_unknown_content" to setOf(
                "values", "values-de", "values-es", "values-fr", "values-hr", "values-hu", "values-it", "values-pt",
                "values-ru", "values-si", "values-sv",
            ),
            "label_quote_original_message_date" to setOf(
                "values", "values-de", "values-es", "values-et", "values-fr", "values-hr", "values-hu", "values-it",
                "values-ja", "values-pl", "values-pt", "values-ru", "values-si",
            ),
        )
        val expectedConsumers = setOf(
            "app/src/main/kotlin/com/wire/android/mapper/MessagePreviewContentMapper.kt",
            "app/src/main/kotlin/com/wire/android/mapper/RegularMessageContentMapper.kt",
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/messages/QuotedMessage.kt",
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/messages/item/MessageExpirationItems.kt",
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/messages/preview/PreviewMessageTypes.kt",
            "app/src/main/kotlin/com/wire/android/ui/home/conversationslist/common/ConversationItemFactory.kt",
            "app/src/test/kotlin/com/wire/android/mapper/MessagePreviewContentMapperTest.kt",
            "app/src/test/kotlin/com/wire/android/ui/home/conversations/messages/draft/MessageDraftViewModelTest.kt",
            "app/src/main/kotlin/com/wire/android/ui/common/bottomsheet/conversation/ConversationOptionsData.kt",
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/ConversationInfoViewModelAppAdapter.kt",
            "app/src/main/kotlin/com/wire/android/ui/home/messagecomposer/MessageComposer.kt",
            "app/src/test/kotlin/com/wire/android/ui/home/conversations/ConversationInfoViewModelAssemblyOwnershipSourceTest.kt",
        )
    }
}
