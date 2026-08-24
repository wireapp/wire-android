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

class ConversationGroupOptionsPresentationOwnershipTest {
    @Test
    fun groupOptionsStringsAreFeatureOwnedWithTheOriginalQualifierCoverage() {
        optionResourceNames.forEach { resourceName ->
            assertTrue(qualifiersContaining(appResources, resourceName).isEmpty(), "$resourceName must not remain in :app.")
            assertTrue(qualifiersContaining(featureResources, resourceName).isNotEmpty(), "$resourceName must be feature-owned.")
        }

        val featureDefinitions = resourceNamesByQualifier(featureResources)
        assertEquals(expectedResourceCountsByQualifier, featureDefinitions.mapValues { (_, names) -> names.size })
        assertEquals(optionResourceNames, featureDefinitions.values.flatten().toSet())
        assertEquals(218, featureDefinitions.values.sumOf { it.size })
    }

    @Test
    fun groupOptionsFeatureDependsOnlyOnFeatureAndNeutralPresentationSeams() {
        assertFalse(File(root, legacyGroupOptionsRelativePath).exists())
        assertFalse(File(root, legacyOptionsItemRelativePath).exists())

        val groupOptions = source(featureGroupOptionsRelativePath)
        val item = source(coreOptionsItemRelativePath)
        val switch = source(coreOptionWithSwitchRelativePath)
        val dialog = source(appDisableConfirmationDialogRelativePath)

        assertTrue(groupOptions.contains("package com.wire.android.ui.home.conversations.details.options"))
        assertTrue(groupOptions.contains("import com.wire.android.feature.conversation.R as conversationR"))
        assertTrue(groupOptions.contains("import com.wire.android.ui.common.preview.MultipleThemePreviews"))
        assertFalse(groupOptions.contains("com.wire.android.R"))
        assertFalse(groupOptions.contains("PreviewMultipleThemes"))
        optionResourceNames.forEach { resourceName ->
            assertTrue(groupOptions.contains("conversationR.string.$resourceName"), "$resourceName must use the feature namespace.")
        }

        assertTrue(item.contains("package com.wire.android.ui.home.conversations.details.options"))
        assertTrue(item.contains("import com.wire.android.ui.common.R"))
        assertFalse(item.contains("com.wire.android.R"))
        assertFalse(item.contains("PreviewMultipleThemes"))
        assertTrue(switch.contains("fun GroupOptionWithSwitch("))
        assertFalse(switch.contains("com.wire.android.R"))
        assertTrue(dialog.contains("fun DisableConfirmationDialog("))
        assertTrue(dialog.contains("import com.wire.android.R"))
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
                    .filterTo(mutableSetOf()) { it in optionResourceNames }
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
        val optionResourceNames = setOf(
            "folder_label_access", "folder_label_messaging", "folder_label_protocol_details", "channel_access_label",
            "channel_access_short_description", "conversation_options_guests_label", "conversation_details_guest_description",
            "conversation_options_services_label", "conversation_details_apps_description", "conversation_options_shared_drive_label",
            "conversation_options_shared_drive_description", "conversation_options_self_deleting_messages_label",
            "conversation_options_self_deleting_messages_description", "conversation_options_self_deleting_messages_shared_drive_description",
            "conversation_options_read_receipt_label", "conversation_options_read_receipt_description", "protocol", "cipher_suite",
            "last_key_material_update_label", "group_state_label", "channel_name_title", "conversation_details_options_group_name",
            "content_description_edit_label", "content_description_conversation_details_guests_action",
            "content_description_conversation_details_apps_action", "content_description_conversation_details_self_deleting_action",
        )
        val expectedResourceCountsByQualifier = mapOf(
            "values" to 26, "values-de" to 25, "values-es" to 16, "values-et" to 2, "values-fr" to 13,
            "values-hr" to 6, "values-hu" to 20, "values-it" to 13, "values-ja" to 3, "values-pl" to 11,
            "values-pt" to 19, "values-ru" to 26, "values-si" to 20, "values-sv" to 5, "values-tr" to 9,
            "values-uk" to 4,
        )
        const val featureGroupOptionsRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/options/GroupConversationOptions.kt"
        const val legacyGroupOptionsRelativePath =
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/details/options/GroupConversationOptions.kt"
        const val legacyOptionsItemRelativePath =
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/details/options/GroupConversationOptionsItem.kt"
        const val coreOptionsItemRelativePath =
            "core/ui-common/src/main/kotlin/com/wire/android/ui/home/conversations/details/options/GroupConversationOptionsItem.kt"
        const val coreOptionWithSwitchRelativePath =
            "core/ui-common/src/main/kotlin/com/wire/android/ui/home/conversations/details/options/GroupOptionWithSwitch.kt"
        const val appDisableConfirmationDialogRelativePath =
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/details/options/DisableConfirmationDialog.kt"
    }
}
