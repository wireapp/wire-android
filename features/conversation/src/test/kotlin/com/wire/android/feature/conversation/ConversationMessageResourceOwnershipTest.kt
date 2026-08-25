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
    fun messagePresentationResourcesAreFeatureOwnedWithUnchangedQualifierCoverage() {
        assertTrue(
            resourceDefinitions(appResources).isEmpty(),
            "Message-presentation strings must not remain in :app.",
        )

        val definitions = resourceDefinitions(featureResources)

        assertEquals(608, definitions.size)
        assertEquals(59, definitions.map { it.name }.toSet().size)
        assertEquals(messageResourceNames, definitions.map { it.name }.toSet())
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
                resourceDefinitionPattern.findAll(file.readText()).mapNotNull { match ->
                    val name = match.groupValues[2]
                    if (name !in messageResourceNames) {
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
        val appSourceRoots = listOf(
            File(repositoryRoot(), "app/src/main/kotlin"),
            File(repositoryRoot(), "app/src/test/kotlin"),
        )
        val resourceDefinitionPattern = Regex(
            """<(string|plurals)\s+name="([^"]+)"([^>]*)>(.*?)</\1>""",
            setOf(RegexOption.DOT_MATCHES_ALL),
        )
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
            "ephemeral_group_channel_event_message",
            "ephemeral_one_to_one_event_message",
            "label_draft",
            "last_message_call",
            "last_message_composite_with_missing_text",
            "last_message_conversations_verification_degraded_mls",
            "last_message_conversations_verification_degraded_proteus",
            "last_message_mentioned",
            "last_message_other_added_only_self_user",
            "last_message_other_added_other_users",
            "last_message_other_added_self_user",
            "last_message_other_changed_conversation_name",
            "last_message_other_removed_only_self_user",
            "last_message_other_removed_other_users",
            "last_message_other_removed_self_user_and_others",
            "last_message_other_user_joined_conversation",
            "last_message_other_user_knock",
            "last_message_other_user_left_conversation",
            "last_message_other_user_shared_asset",
            "last_message_other_user_shared_image",
            "last_message_other_user_shared_location",
            "last_message_other_user_shared_video",
            "last_message_replied",
            "last_message_self_added_users",
            "last_message_self_changed_conversation_name",
            "last_message_self_removed_users",
            "last_message_self_user_joined_conversation",
            "last_message_self_user_knock",
            "last_message_self_user_left_conversation",
            "last_message_self_user_shared_asset",
            "last_message_self_user_shared_audio",
            "last_message_self_user_shared_image",
            "last_message_self_user_shared_location",
            "last_message_self_user_shared_video",
            "last_message_team_member_removed",
            "last_message_verified_conversation_mls",
            "last_message_verified_conversation_proteus",
            "unread_event_call",
            "unread_event_knock",
            "unread_event_mention",
            "unread_event_message",
            "unread_event_reply",
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
        const val expectedDefinitionFingerprint = "c26e4ffaccb869fab3ba7ce8055528552ec1e9877d790f5f353f6e7f1d5a97b9"
        val expectedConsumers = setOf(
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/messages/QuotedMessage.kt",
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/messages/preview/PreviewMessageTypes.kt",
            "app/src/main/kotlin/com/wire/android/ui/home/conversationslist/common/ConversationItemFactory.kt",
            "app/src/test/kotlin/com/wire/android/ui/home/conversations/messages/draft/MessageDraftViewModelTest.kt",
            "app/src/main/kotlin/com/wire/android/ui/common/bottomsheet/conversation/ConversationOptionsData.kt",
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/ConversationInfoViewModelAppAdapter.kt",
            "app/src/main/kotlin/com/wire/android/ui/home/messagecomposer/MessageComposer.kt",
            "app/src/test/kotlin/com/wire/android/ui/home/conversations/ConversationInfoViewModelAssemblyOwnershipSourceTest.kt",
        )
    }
}
