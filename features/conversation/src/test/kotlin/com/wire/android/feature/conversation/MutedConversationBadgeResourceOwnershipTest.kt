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

class MutedConversationBadgeResourceOwnershipTest {

    @Test
    fun mutedConversationAccessibilityStringIsFeatureOwnedWithUnchangedLocalizedValues() {
        assertTrue(
            qualifiersContaining(appResources, mutedConversationResourceName).isEmpty(),
            "$mutedConversationResourceName must not remain in :app resources.",
        )
        assertEquals(
            mutedConversationStrings.keys,
            qualifiersContaining(featureResources, mutedConversationResourceName),
            "$mutedConversationResourceName must preserve its qualifier coverage.",
        )
        mutedConversationStrings.forEach { (qualifier, expectedValue) ->
            val definition = stringDefinition(featureResources, qualifier, mutedConversationResourceName)

            assertEquals("", definition.attributes.trim(), "$mutedConversationResourceName must preserve its attributes.")
            assertEquals(expectedValue, definition.value, "$mutedConversationResourceName has an unexpected value in $qualifier.")
        }
    }

    @Test
    fun muteDrawableIsFeatureOwnedWithByteIdenticalContents() {
        val appDrawable = File(appResources, "drawable/ic_mute.xml")
        val featureDrawable = File(featureResources, "drawable/ic_mute.xml")

        assertFalse(appDrawable.exists(), "ic_mute must not remain in :app resources.")
        assertTrue(featureDrawable.isFile, "Missing ${featureDrawable.path}")
        assertEquals(mutedConversationDrawableHash, featureDrawable.readBytes().sha256())
    }

    @Test
    fun badgeAndBottomSheetUseFeatureOwnedMuteResources() {
        assertFalse(File(repositoryRoot(), legacyBadgeRelativePath).exists())

        val badge = sourceFile(featureBadgeRelativePath)
        assertTrue(badge.contains("package com.wire.android.ui.home.conversationslist.common"))
        assertTrue(badge.contains("import com.wire.android.feature.conversation.R as conversationR"))
        assertTrue(badge.contains("import com.wire.android.ui.common.preview.MultipleThemePreviews"))
        assertFalse(badge.contains("com.wire.android.R"))
        assertFalse(badge.contains("PreviewMultipleThemes"))
        assertTrue(badge.contains("conversationR.drawable.ic_mute"))
        assertTrue(badge.contains("conversationR.string.content_description_muted_conversation"))

        val bottomSheet = sourceFile(bottomSheetRelativePath)
        assertTrue(bottomSheet.contains("import com.wire.android.feature.conversation.R as conversationR"))
        assertTrue(bottomSheet.contains("id = conversationR.drawable.ic_mute"))
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

        const val mutedConversationResourceName = "content_description_muted_conversation"
        const val mutedConversationDrawableHash = "bd726c43704a0d6117e99d6eaeef40c0e588b49748029f3284de9803a333f24a"
        const val featureBadgeRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversationslist/common/MutedConversationBadge.kt"
        const val legacyBadgeRelativePath =
            "app/src/main/kotlin/com/wire/android/ui/home/conversationslist/common/MutedConversationBadge.kt"
        const val bottomSheetRelativePath =
            "app/src/main/kotlin/com/wire/android/ui/common/bottomsheet/conversation/ConversationMainSheetContent.kt"
        val appResources = File(repositoryRoot(), "app/src/main/res")
        val featureResources = File(repositoryRoot(), "features/conversation/src/main/res")
        val mutedConversationStrings = mapOf(
            "values" to "Muted conversation",
            "values-ar" to "محادثة مكتومة",
            "values-de" to "Unterhaltung ist stummgeschaltet",
            "values-es" to "Conversación silenciada",
            "values-et" to "Vaigistatud vestlus",
            "values-fr" to "Conversation en sourdine",
            "values-hr" to "Utišan razgovor",
            "values-hu" to "Némított beszélgetés",
            "values-it" to "Conversazione silenziata",
            "values-ja" to "ミュートした会話",
            "values-pl" to "Wyciszona rozmowa",
            "values-pt" to "Conversa silenciada",
            "values-ru" to "Беззвучные беседы",
            "values-si" to "නිහඬ කළ සංවාදය",
            "values-sv" to "Tystad konversation",
            "values-tr" to "Sessize alınmış sohbet",
            "values-uk" to "Вимкнена бесіда",
        )
    }
}
