package com.wire.android.ui.common

import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SelfDeletionTypeOwnershipSourceTest {
    @Test
    fun selfDeletionTypesAreCoreOwnedAndAppPassesTheDeveloperCapability() {
        val duration = source("core/ui-common/src/main/kotlin/com/wire/android/ui/home/messagecomposer/SelfDeletionDuration.kt")
        val mapper = source("core/ui-common/src/main/kotlin/com/wire/android/ui/home/conversations/selfdeletion/SelfDeletionMapper.kt")
        val menuItems = source("app/src/main/kotlin/com/wire/android/ui/home/conversations/selfdeletion/SelfDeletionMenuItems.kt")
        val screen = source("app/src/main/kotlin/com/wire/android/ui/home/conversations/details/editselfdeletingmessages/EditSelfDeletingMessagesScreen.kt")

        assertTrue(duration.contains("fun customValues(developerFeaturesEnabled: Boolean)"))
        assertFalse(duration.contains("BuildConfig"))
        assertTrue(mapper.contains("object SelfDeletionMapper"))
        assertTrue(menuItems.contains("SelfDeletionDuration.customValues(BuildConfig.DEVELOPER_FEATURES_ENABLED)"))
        assertTrue(screen.contains("SelfDeletionDuration.customValues(BuildConfig.DEVELOPER_FEATURES_ENABLED)"))
        assertFalse(File(root, "app/src/main/kotlin/com/wire/android/ui/home/messagecomposer/SelfDeletionDuration.kt").exists())
        assertFalse(File(root, "app/src/main/kotlin/com/wire/android/ui/home/conversations/selfdeletion/SelfDeletionMapper.kt").exists())
    }

    @Test
    fun shortLabelsAreCoreOwnedWithExactLocalizedValues() {
        expectedValues.forEach { (name, values) ->
            assertTrue(qualifiersFor(appResources, name).isEmpty())
            assertEquals(values.keys, qualifiersFor(coreResources, name))
            values.forEach { (qualifier, expectedValue) ->
                assertEquals(expectedValue, stringValue(coreResources, qualifier, name))
            }
        }
    }

    private fun source(path: String): String = File(root, path).readText()

    private fun qualifiersFor(directory: File, name: String): Set<String> =
        directory.walkTopDown().filter { it.isFile && it.extension == "xml" }
            .filter { it.readText().contains("name=\"$name\"") }
            .map { requireNotNull(it.parentFile).name }.toSet()

    private fun stringValue(directory: File, qualifier: String, name: String): String {
        val file = File(directory, "$qualifier/strings.xml")
        val match = Regex("<string name=\\\"$name\\\">(.*?)</string>").find(file.readText())
        return requireNotNull(match).groupValues[1]
    }

    private companion object {
        val root = generateSequence(File(requireNotNull(System.getProperty("user.dir"))).absoluteFile) { it.parentFile }
            .first { File(it, "settings.gradle.kts").isFile }
        val appResources = File(root, "app/src/main/res")
        val coreResources = File(root, "core/ui-common/src/main/res")
        val expectedValues = mapOf(
            "ten_seconds_short_label" to mapOf(
                "values" to "10s", "values-de" to "10 Sek", "values-hu" to "10mp", "values-it" to "10s",
                "values-pt" to "10s", "values-ru" to "10 сек", "values-si" to "ත. 10",
            ),
            "one_minute_short_label" to mapOf(
                "values" to "1m", "values-de" to "1 Min.", "values-hu" to "1p", "values-it" to "1m",
                "values-pt" to "1m", "values-ru" to "1 мин.", "values-si" to "වි. 1",
            ),
            "five_minutes_short_label" to mapOf(
                "values" to "5m", "values-de" to "5 Min", "values-es" to "5min", "values-hu" to "5p",
                "values-it" to "5m", "values-pt" to "5m", "values-ru" to "5 мин.", "values-si" to "වි. 5",
            ),
            "one_hour_short_label" to mapOf(
                "values" to "1h", "values-de" to "1 Std", "values-et" to "1t", "values-hu" to "1ó",
                "values-it" to "1h", "values-pt" to "1h", "values-ru" to "1ч", "values-si" to "පැ. 1",
            ),
            "one_day_short_label" to mapOf(
                "values" to "1d", "values-de" to "1Tag", "values-et" to "1p", "values-hu" to "1n",
                "values-it" to "1g", "values-pt" to "1d", "values-ru" to "1д", "values-si" to "ද. 1",
            ),
            "one_week_short_label" to mapOf(
                "values" to "1w", "values-de" to "1 Wo", "values-es" to "1sem", "values-et" to "1ndl",
                "values-hu" to "1h", "values-it" to "1w", "values-pt" to "1sem", "values-ru" to "1 нед.",
                "values-si" to "ස. 1",
            ),
            "four_weeks_short_label" to mapOf(
                "values" to "4w", "values-de" to "4 Wo", "values-es" to "4sem", "values-et" to "4ndl",
                "values-hu" to "4h", "values-it" to "4w", "values-pt" to "4sem", "values-ru" to "4 нед",
                "values-si" to "ස. 4",
            ),
        )
    }
}
