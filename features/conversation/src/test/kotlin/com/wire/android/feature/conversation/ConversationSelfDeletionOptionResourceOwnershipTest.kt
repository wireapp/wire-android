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

class ConversationSelfDeletionOptionResourceOwnershipTest {
    @Test
    fun selfDeletionOptionStringsAreFeatureOwnedWithExactLocalizedParity() {
        assertStringOwnership("self_deleting_messages_option", optionLabels)
        assertStringOwnership("self_deleting_messages_option_description", optionDescriptions)
    }

    @Test
    fun selfDeletionOptionRendererUsesFeatureResourcesWithoutAppImports() {
        assertFalse(File(root, legacyRendererRelativePath).exists())

        val renderer = source(featureRendererRelativePath)
        assertTrue(renderer.contains("package com.wire.android.ui.home.conversations.details.editselfdeletingmessages"))
        assertTrue(renderer.contains("import com.wire.android.feature.conversation.R as conversationR"))
        assertTrue(renderer.contains("conversationR.string.self_deleting_messages_option"))
        assertTrue(renderer.contains("conversationR.string.self_deleting_messages_option_description"))
        assertFalse(renderer.contains("com.wire.android.R"))
    }

    private fun assertStringOwnership(name: String, expected: Map<String, String>) {
        assertTrue(qualifiers(appResources, name).isEmpty(), "$name must not remain in :app.")
        assertEquals(expected.keys, qualifiers(featureResources, name))
        expected.forEach { (qualifier, expectedValue) ->
            val definition = stringDefinition(featureResources, qualifier, name)
            assertEquals("", definition.attributes.trim(), "$name must preserve its attributes in $qualifier.")
            assertEquals(expectedValue, definition.value, "$name has an unexpected value in $qualifier.")
        }
    }

    private fun qualifiers(resourceDirectory: File, name: String): Set<String> =
        resourceDirectory.walkTopDown().filter { it.isFile && it.name == "strings.xml" }
            .filter { it.readText().contains("name=\"$name\"") }
            .map { requireNotNull(it.parentFile).name }
            .toSet()

    private fun stringDefinition(resourceDirectory: File, qualifier: String, name: String): StringDefinition {
        val file = File(resourceDirectory, "$qualifier/strings.xml")
        val match = Regex("""<string\s+name="$name"([^>]*)>(.*?)</string>""").find(file.readText())
        assertTrue(match != null, "Missing $name in ${file.path}")
        return StringDefinition(
            attributes = requireNotNull(match).groupValues[1],
            value = match.groupValues[2],
        )
    }

    private fun source(relativePath: String): String = File(root, relativePath).also { file ->
        assertTrue(file.isFile, "Missing ${file.path}")
    }.readText()

    private data class StringDefinition(
        val attributes: String,
        val value: String,
    )

    private companion object {
        val root = generateSequence(File(requireNotNull(System.getProperty("user.dir"))).absoluteFile) { it.parentFile }
            .first { File(it, "settings.gradle.kts").isFile }
        val appResources = File(root, "app/src/main/res")
        val featureResources = File(root, "features/conversation/src/main/res")
        val optionLabels = mapOf(
            "values" to "Enforce message deletion", "values-de" to "Löschen von Nachrichten erzwingen",
            "values-es" to "Forzar auto-borrado de mensajes", "values-hu" to "Üzenet törlésének kényszerítése",
            "values-it" to "Forza eliminazione dei messaggi", "values-pt" to "Forçar exclusão de mensagens",
            "values-ru" to "Принудительное удаление сообщений", "values-si" to "බලෙන් පණිවිඩය මකන්න",
        )
        val optionDescriptions = mapOf(
            "values" to "When this is on, all messages in this conversation will disappear after a certain time. This applies to all participants.",
            "values-de" to "Wenn diese Option aktiviert ist, verschwinden alle Nachrichten in dieser Unterhaltung nach einer bestimmten Zeit. Dies gilt für alle Teilnehmer.",
            "values-hu" to "Ha ez aktív, ebben a beszélgetésben minden üzenet el fog tűnni bizonyos idő után. Ez minden résztvevőre érvényes.",
            "values-ru" to "При включении этой опции все сообщения в данной беседе будут исчезать через определенное время. Это относится ко всем участникам.",
            "values-si" to "මෙය ක්‍රියාත්මක විට, මෙම සංවාදයේ ඇති සියලුම පණිවිඩ නිශ්චිත කාලයකට පසු අතුරුදහන් වනු ඇත. මෙය සියලුම සහභාගිවන්නන්ට අදාළ වේ.",
        )
        const val featureRendererRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/editselfdeletingmessages/SelfDeletingMessageOption.kt"
        const val legacyRendererRelativePath =
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/details/editselfdeletingmessages/SelfDeletingMessageOption.kt"
    }
}
