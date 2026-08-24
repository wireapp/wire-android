/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.home.settings

import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SettingsOptionSwitchResourceOwnershipSourceTest {
    @Test
    fun neutralSwitchResourcesAreCoreOwnedWithExactLocalizedParity() {
        assertStringOwnership("label_on", labelOn)
        assertStringOwnership("content_description_toggle_setting_label", toggleActionDescription)
    }

    @Test
    fun switchAndItsCallersUseCoreResources() {
        assertFalse(File(root, "app/src/main/kotlin/com/wire/android/ui/home/settings/SettingsOptionSwitch.kt").exists())
        assertTrue(File(root, "core/ui-common/src/main/kotlin/com/wire/android/ui/home/settings/SettingsOptionSwitch.kt").exists())
        val switch = source("core/ui-common/src/main/kotlin/com/wire/android/ui/home/settings/SettingsOptionSwitch.kt")
        val newConversation = source("app/src/main/kotlin/com/wire/android/ui/home/newconversation/common/SendContentButton.kt")
        val cells = source("features/cells/src/main/java/com/wire/android/feature/cells/ui/publiclink/settings/PublicLinkSettingsSection.kt")

        assertFalse(switch.contains("import com.wire.android.R"))
        assertTrue(switch.contains("R.string.label_on"))
        assertTrue(switch.contains("R.string.content_description_toggle_setting_label"))
        assertTrue(newConversation.contains("commonR.string.content_description_toggle_setting_label"))
        assertTrue(cells.contains("commonR.string.label_on"))
        assertFalse(cells.contains("if (isEnabled) R.string.label_on"))
    }

    private fun assertStringOwnership(name: String, expected: Map<String, String>) {
        assertTrue(qualifiers(appResources, name).isEmpty(), "$name must not remain in :app.")
        assertTrue(qualifiers(cellsResources, name).isEmpty(), "$name must not remain in :features:cells.")
        assertEquals(expected.keys, qualifiers(coreResources, name))
        expected.forEach { (qualifier, value) ->
            assertEquals(value, string(coreResources, qualifier, name))
        }
    }

    private fun qualifiers(directory: File, name: String): Set<String> =
        directory.walkTopDown().filter { it.isFile && it.extension == "xml" }
            .filter { it.readText().contains("name=\"$name\"") }
            .map { requireNotNull(it.parentFile).name }.toSet()

    private fun string(directory: File, qualifier: String, name: String): String {
        val file = File(directory, "$qualifier/strings.xml")
        val match = Regex("""<string\s+name="$name"[^>]*>(.*?)</string>""").find(file.readText())
        assertTrue(match != null, "Missing $name in ${file.path}")
        return requireNotNull(match).groupValues[1]
    }

    private fun source(path: String): String = File(root, path).readText()

    private companion object {
        val root = generateSequence(File(requireNotNull(System.getProperty("user.dir"))).absoluteFile) { it.parentFile }
            .first { File(it, "settings.gradle.kts").isFile }
        val appResources = File(root, "app/src/main/res")
        val cellsResources = File(root, "features/cells/src/main/res")
        val coreResources = File(root, "core/ui-common/src/main/res")
        val labelOn = mapOf(
            "values" to "ON", "values-de" to "EIN", "values-es" to "Encendido", "values-et" to "SEES",
            "values-fr" to "ON", "values-hr" to "UKLJUČENO", "values-hu" to "BE", "values-it" to "ACCESO",
            "values-ja" to "オン", "values-pl" to "W&#322;&#261;czone", "values-pt" to "Ativado",
            "values-ru" to "ВКЛ", "values-si" to "සක්‍රියයි", "values-sv" to "PÅ", "values-tr" to "AÇIK",
            "values-uk" to "УВІМКНУТИ",
        )
        val toggleActionDescription = mapOf(
            "values" to "toggle setting", "values-de" to "umschalten", "values-hu" to "beállításkapcsoló",
            "values-pt" to "alterar configuração", "values-ru" to "переключить настройку",
            "values-si" to "ටොගල් සැකසුම", "values-tr" to "geçiş ayarı",
        )
    }
}
