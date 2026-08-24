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

class ConversationMessageStatusIndicatorResourceOwnershipTest {

    @Test
    fun messageStatusAccessibilityStringsAreFeatureOwnedWithUnchangedLocalizedValues() {
        messageStatusStrings.forEach { (resourceName, expectedValues) ->
            assertTrue(
                qualifiersContaining(appResources, resourceName).isEmpty(),
                "$resourceName must not remain in :app resources.",
            )
            assertEquals(
                expectedValues.keys,
                qualifiersContaining(featureResources, resourceName),
                "$resourceName must preserve its qualifier coverage.",
            )
            expectedValues.forEach { (qualifier, expectedValue) ->
                val definition = stringDefinition(featureResources, qualifier, resourceName)

                assertEquals("", definition.attributes.trim(), "$resourceName must preserve its attributes.")
                assertEquals(expectedValue, definition.value, "$resourceName has an unexpected value in $qualifier.")
            }
        }
    }

    @Test
    fun messageStatusDrawablesHaveReusableOwnersWithUnchangedContents() {
        messageStatusDrawableHashes.forEach { (name, expectedHash) ->
            assertFalse(File(appResources, "drawable/$name.xml").exists(), "$name must not remain in :app resources.")

            val owner = if (name in sharedDrawableNames) coreResources else featureResources
            val nonOwner = if (name in sharedDrawableNames) featureResources else coreResources
            assertFalse(File(nonOwner, "drawable/$name.xml").exists(), "$name must have exactly one reusable owner.")
            val drawable = File(owner, "drawable/$name.xml")
            assertTrue(drawable.isFile, "Missing ${drawable.path}")
            assertEquals(expectedHash, drawable.readBytes().sha256(), "$name content changed during ownership move.")
        }
    }

    @Test
    fun statusRendererRemainsFeatureOwnedAndUsesFeatureResources() {
        assertFalse(File(repositoryRoot(), legacyMessageStatusIndicatorRelativePath).exists())

        val source = sourceFile(messageStatusIndicatorRelativePath)

        assertTrue(source.contains("package com.wire.android.ui.home.conversations.messages.item"))
        assertTrue(source.contains("import com.wire.android.feature.conversation.R as conversationR"))
        assertFalse(source.contains("com.wire.android.R"))
        assertFalse(source.contains("BuildConfig"))
        assertFalse(source.contains("PreviewMultipleThemes"))
        messageStatusStrings.keys.forEach { resourceName ->
            assertTrue(source.contains("conversationR.string.$resourceName"))
        }
        (messageStatusDrawableHashes.keys - sharedDrawableNames).forEach { resourceName ->
            assertTrue(source.contains("conversationR.drawable.$resourceName"))
        }
        sharedDrawableNames.forEach { resourceName ->
            assertTrue(source.contains("commonR.drawable.$resourceName"))
        }
    }

    private fun qualifiersContaining(resourceDirectory: File, resourceName: String): Set<String> =
        resourceDirectory.walkTopDown()
            .filter { it.isFile && it.name == "strings.xml" }
            .filter { it.readText().contains("name=\"$resourceName\"") }
            .map { requireNotNull(it.parentFile).name }
            .toSet()

    private fun stringDefinition(resourceDirectory: File, qualifier: String, resourceName: String): StringDefinition {
        val file = File(resourceDirectory, "$qualifier/strings.xml")
        assertTrue(file.isFile, "Missing ${file.path}")
        val match = Regex(
            """<string\s+name=\"$resourceName\"([^>]*)>(.*?)</string>""",
            setOf(RegexOption.DOT_MATCHES_ALL),
        ).find(file.readText())

        assertTrue(match != null, "Missing $resourceName in ${file.path}")
        return StringDefinition(
            attributes = requireNotNull(match).groupValues[1],
            value = match.groupValues[2],
        )
    }

    private fun sourceFile(relativePath: String): String = File(repositoryRoot(), relativePath).also { file ->
        assertTrue(file.isFile, "Missing ${file.path}")
    }.readText()

    private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(this)
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }

    private data class StringDefinition(
        val attributes: String,
        val value: String,
    )

    private companion object {
        private fun repositoryRoot(): File =
            generateSequence(File(requireNotNull(System.getProperty("user.dir"))).absoluteFile) { it.parentFile }
                .first { File(it, "settings.gradle.kts").isFile }

        const val messageStatusIndicatorRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/messages/item/MessageStatusIndicator.kt"
        const val legacyMessageStatusIndicatorRelativePath =
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/messages/item/MessageStatusIndicator.kt"
        val appResources = File(repositoryRoot(), "app/src/main/res")
        val featureResources = File(repositoryRoot(), "features/conversation/src/main/res")
        val coreResources = File(repositoryRoot(), "core/ui-common/src/main/res")
        val messageStatusStrings = mapOf(
            "content_description_message_sending_status" to mapOf(
                "values" to "Message sending status",
                "values-de" to "Nachricht wird gesendet",
                "values-es" to "Estado de envío del mensaje",
                "values-fr" to "État d\\'envoi des messages",
                "values-hr" to "Status slanja poruke",
                "values-hu" to "Üzenet küldése állapot",
                "values-it" to "Stato di invio del messaggio",
                "values-ja" to "メッセージ送信中の状態",
                "values-pt" to "Status de envio de mensagem",
                "values-ru" to "Статус отправки сообщения",
                "values-si" to "පණවිඩය යවන තත්‍වය",
                "values-tr" to "Mesaj gönderme durumu",
            ),
            "content_description_message_delivered_status" to mapOf(
                "values" to "Message delivered status",
                "values-de" to "Nachricht wurde zugestellt",
                "values-fr" to "Statut de la livraison du message",
                "values-hr" to "Status isporučene poruke",
                "values-hu" to "Üzenet kézbesítve állapot",
                "values-it" to "Stato messaggio consegnato",
                "values-ja" to "メッセージの配信状態",
                "values-pt" to "Estado de mensagem entregue",
                "values-ru" to "Статус доставки сообщения",
                "values-si" to "බාරදුන් පණවිඩයේ තත්‍වය",
                "values-tr" to "Mesaj teslim durumu",
            ),
            "content_description_message_read_status" to mapOf(
                "values" to "Message read status",
                "values-de" to "Nachricht wurde gelesen",
                "values-fr" to "Statut de lecture du message",
                "values-hr" to "Status pročitane poruke",
                "values-hu" to "Üzenet olvasva állapot",
                "values-it" to "Stato messaggio letto",
                "values-ja" to "メッセージの既読状態",
                "values-pt" to "Status de leitura da mensagem",
                "values-ru" to "Статус прочтения сообщения",
                "values-si" to "පණවිඩය කියවූ තත්‍වය",
                "values-tr" to "Mesaj okunma durumu",
            ),
        )
        val messageStatusDrawableHashes = mapOf(
            "ic_message_sending" to "1e40088c95e253d06bf8f031437ee1962f72c8717508c2c219b8196bf0b1fa7f",
            "ic_message_sent" to "e0bf66aa562db0cee8e4c3fa893ed115f158c6c04bdd679a4d1b1b9b7db03415",
            "ic_message_delivered" to "e0bdb64030f7a7dc7eaac0e98db6a3eec17cadf9e89af16ce3278c025db44c2d",
            "ic_message_read" to "c70fea3930db58849054f4bf60ca774d3e130ec3de86aff00a99eb35a54b491b",
            "ic_warning_circle" to "c841d743b542493737053582586e3c3d5cb3ee3800a931c5f5b1a9a696040585",
        )
        val sharedDrawableNames = setOf("ic_message_read", "ic_warning_circle")
    }
}
