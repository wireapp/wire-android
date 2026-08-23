package com.wire.android.ui.common

import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GenericDurationLabelResourceOwnershipSourceTest {
    @Test
    fun genericDurationLabelsAreCoreOwnedWithExactLocalizedParity() {
        assertStringOwnership("label_off", labelOff)
        pluralLabels.forEach { (name, expected) -> assertPluralOwnership(name, expected) }
    }

    @Test
    fun callersUseCoreResourcesForTransferredIds() {
        val device = source("app/src/main/kotlin/com/wire/android/ui/authentication/devices/model/Device.kt")
        val durationUtil = source("app/src/main/kotlin/com/wire/android/util/DurationUtil.kt")
        val channelHistory = source("app/src/main/kotlin/com/wire/android/ui/home/newconversation/channelhistory/ChannelHistoryType.kt")
        val settings = source("app/src/main/kotlin/com/wire/android/ui/home/settings/SettingsOptionSwitch.kt")
        val selfDeletion = source("core/ui-common/src/main/kotlin/com/wire/android/ui/home/messagecomposer/SelfDeletionDuration.kt")
        val cells = source("features/cells/src/main/java/com/wire/android/feature/cells/ui/publiclink/settings/PublicLinkSettingsSection.kt")

        assertTrue(device.contains("commonR.plurals.weeks_long_label"))
        assertTrue(durationUtil.contains("import com.wire.android.ui.common.R"))
        assertFalse(durationUtil.contains("import com.wire.android.R"))
        assertTrue(channelHistory.contains("commonR.plurals.days_long_label"))
        assertTrue(channelHistory.contains("commonR.plurals.weeks_long_label"))
        assertTrue(settings.contains("else commonR.string.label_off"))
        assertTrue(selfDeletion.contains("import com.wire.android.ui.common.R"))
        assertFalse(selfDeletion.contains("import com.wire.android.R"))
        assertTrue(selfDeletion.contains("R.string.label_off"))
        pluralNames.forEach { assertTrue(selfDeletion.contains("R.plurals.$it")) }
        assertTrue(cells.contains("else commonR.string.label_off"))
        assertFalse(cells.contains("else R.string.label_off"))
    }

    private fun assertStringOwnership(name: String, expected: Map<String, String>) {
        assertNoAppOrCellsDefinition(name)
        assertEquals(expected.keys, qualifiers(coreResources, name))
        expected.forEach { (qualifier, value) ->
            assertEquals(value, string(coreResources, qualifier, name))
        }
    }

    private fun assertPluralOwnership(name: String, expected: Map<String, Map<String, String>>) {
        assertNoAppOrCellsDefinition(name)
        assertEquals(expected.keys, qualifiers(coreResources, name))
        expected.forEach { (qualifier, values) ->
            assertEquals(values, plural(coreResources, qualifier, name))
        }
    }

    private fun assertNoAppOrCellsDefinition(name: String) {
        assertTrue(qualifiers(appResources, name).isEmpty(), "$name must not remain in :app.")
        assertTrue(qualifiers(cellsResources, name).isEmpty(), "$name must not remain in :features:cells.")
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

    private fun plural(directory: File, qualifier: String, name: String): Map<String, String> {
        val file = File(directory, "$qualifier/strings.xml")
        val match = Regex("""<plurals\s+name="$name">(.*?)</plurals>""", setOf(RegexOption.DOT_MATCHES_ALL))
            .find(file.readText())
        assertTrue(match != null, "Missing $name in ${file.path}")
        return Regex("""<item\s+quantity="([^"]+)">(.*?)</item>""")
            .findAll(requireNotNull(match).groupValues[1])
            .associate { it.groupValues[1] to it.groupValues[2] }
    }

    private fun source(path: String): String = File(root, path).readText()

    private companion object {
        val root = generateSequence(File(requireNotNull(System.getProperty("user.dir"))).absoluteFile) { it.parentFile }
            .first { File(it, "settings.gradle.kts").isFile }
        val appResources = File(root, "app/src/main/res")
        val cellsResources = File(root, "features/cells/src/main/res")
        val coreResources = File(root, "core/ui-common/src/main/res")
        val pluralNames = listOf("seconds_long_label", "minutes_long_label", "hours_long_label", "days_long_label", "weeks_long_label")
        val labelOff = mapOf(
            "values" to "OFF", "values-de" to "AUS", "values-es" to "Apagado", "values-et" to "VÄLJAS",
            "values-fr" to "OFF", "values-hr" to "ISKLJUČENO", "values-hu" to "KI", "values-it" to "SPENTO",
            "values-ja" to "オフ", "values-pl" to "Wy&#322;&#261;czone", "values-pt" to "Desativado",
            "values-ru" to "ВЫКЛ", "values-si" to "අක්‍රියයි", "values-sv" to "AV", "values-tr" to "KAPALI",
            "values-uk" to "ВИМКНУТИ",
        )
        val pluralLabels = mapOf(
            "seconds_long_label" to mapOf(
                "values" to mapOf("one" to "1 second", "other" to "%1\$d seconds"), "values-de" to mapOf("one" to "1 Sekunde", "other" to "%1\$d Sekunden"), "values-es" to mapOf("one" to "1 segundo", "other" to "%1\$d segundos"), "values-et" to mapOf("one" to "1 sekund", "other" to "%1\$d sekundit"), "values-hu" to mapOf("one" to "1 másodperc", "other" to "%1\$d másodperc"), "values-it" to mapOf("one" to "1 secondo", "other" to "%1\$d secondi"), "values-pt" to mapOf("one" to "1 segundo", "other" to "%1\$d segundos"), "values-ru" to mapOf("one" to "1 секунда", "few" to "%1\$d секунды", "many" to "%1\$d секунд", "other" to "%1\$d секунды"), "values-si" to mapOf("one" to "තත්. 1", "other" to "තත්. %1\$d"), "values-sv" to mapOf("one" to "1 sekund", "other" to "%1\$d sekunder"),
            ),
            "minutes_long_label" to mapOf(
                "values" to mapOf("one" to "1 minute", "other" to "%1\$d minutes"), "values-de" to mapOf("one" to "1 Minute", "other" to "%1\$d Minuten"), "values-es" to mapOf("one" to "1 minuto", "other" to "%1\$d minutos"), "values-et" to mapOf("one" to "1 minut", "other" to "%1\$d minutit"), "values-hu" to mapOf("one" to "1 perc", "other" to "%1\$d perc"), "values-it" to mapOf("one" to "1 minuto", "other" to "%1\$d minuti"), "values-pt" to mapOf("one" to "1 minuto", "other" to "%1\$d minutos"), "values-ru" to mapOf("one" to "1 минуту", "few" to "%1\$d минуты", "many" to "%1\$d минут", "other" to "%1\$d минуты"), "values-si" to mapOf("one" to "විනාඩි 1", "other" to "විනාඩි %1\$d"), "values-sv" to mapOf("one" to "1 minut", "other" to "%1\$d minuter"),
            ),
            "hours_long_label" to mapOf(
                "values" to mapOf("one" to "1 hour", "other" to "%1\$d hours"), "values-de" to mapOf("one" to "1 Stunde", "other" to "%1\$d Stunden"), "values-es" to mapOf("one" to "1 hora", "other" to "%1\$d horas"), "values-et" to mapOf("one" to "1 tund", "other" to "%1\$d tundi"), "values-hu" to mapOf("one" to "1 óra", "other" to "%1\$d óra"), "values-it" to mapOf("one" to "1 ora", "other" to "%1\$d ore"), "values-pt" to mapOf("one" to "1 hora", "other" to "%1\$d horas"), "values-ru" to mapOf("one" to "1 час", "few" to "%1\$d часа", "many" to "%1\$d часов", "other" to "%1\$d часа"), "values-si" to mapOf("one" to "පැය 1", "other" to "පැය %1\$d"), "values-sv" to mapOf("one" to "1 timme", "other" to "%1\$d timmar"),
            ),
            "days_long_label" to mapOf(
                "values" to mapOf("one" to "1 day", "other" to "%1\$d days"), "values-de" to mapOf("one" to "1 Tag", "other" to "%1\$d Tage"), "values-es" to mapOf("one" to "1 día", "other" to "%1\$d días"), "values-et" to mapOf("one" to "1 päev", "other" to "%1\$d päeva"), "values-hu" to mapOf("one" to "1 nap", "other" to "%1\$d nap"), "values-it" to mapOf("one" to "1 giorno", "other" to "%1\$d giorni"), "values-pt" to mapOf("one" to "1 dia", "other" to "%1\$d dias"), "values-ru" to mapOf("one" to "1 день", "few" to "%1\$d дня", "many" to "%1\$d дней", "other" to "%1\$d дня"), "values-si" to mapOf("one" to "දවස් 1", "other" to "දවස් %1\$d"), "values-sv" to mapOf("one" to "1 dag", "other" to "%1\$d dagar"),
            ),
            "weeks_long_label" to mapOf(
                "values" to mapOf("one" to "1 week", "other" to "%1\$d weeks"), "values-de" to mapOf("one" to "1 Woche", "other" to "%1\$d Wochen"), "values-es" to mapOf("one" to "1 semana", "other" to "%1\$d semanas"), "values-et" to mapOf("one" to "1 nädal", "other" to "%1\$d nädalat"), "values-hu" to mapOf("one" to "1 hét", "other" to "%1\$d hét"), "values-it" to mapOf("one" to "1 settimana", "other" to "%1\$d settimane"), "values-pt" to mapOf("one" to "1 semana", "other" to "%1\$d semanas"), "values-ru" to mapOf("one" to "1 неделя", "few" to "%1\$d недели", "many" to "%1\$d недель", "other" to "%1\$d недели"), "values-si" to mapOf("one" to "සති 1", "other" to "සතිි %1\$d"), "values-sv" to mapOf("one" to "1 vecka", "other" to "%1\$d veckor"),
            ),
        )
    }
}
