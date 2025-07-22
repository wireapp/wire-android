/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

package customization

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(JUnit4::class)
class ConfigurationFileImporterTest {

    @Suppress("VulnerableCodeUsages") // We don't write sensitive info, and this is patched in newer JDKs
    @get:Rule
    val folder: TemporaryFolder = TemporaryFolder()
    private val configFile get() = File(folder.root, "someFile.json")

    private val importer get() = ConfigurationFileImporter()

    @Test
    fun givenDefaultValuesAndFlavorSpecifics_whenImportingFile_shouldOverwriteDefaultWithFlavorSpecific() {
        val keyName = "color"
        val overwrittenValue = "yellow"
        configFile.writeText(
            """
            {
            "${ConfigurationFileImporter.KEY_FLAVORS}": {
                    "strawberry": {},            
                    "apple": {},            
                    "banana": {
                        "$keyName": "$overwrittenValue"
                    }           
                },
                "$keyName": "red"
            }
            """.trimIndent()
        )

        val result = importer.loadConfigsFromFile(configFile)

        assertEquals(overwrittenValue, result.flavorMap["banana"]!![keyName])
    }

    @Test
    fun givenDefaultValuesAndFlavorSpecifics_whenImportingFile_shouldContainDefaultWhenNotOverwritten() {
        val keyName = "color"
        val defaultValue = "red"
        configFile.writeText(
            """
            {
            "${ConfigurationFileImporter.KEY_FLAVORS}": {
                    "strawberry": {},            
                    "apple": {},            
                    "banana": {
                        "$keyName": "yellow"
                    }           
                },
                "$keyName": "$defaultValue"
            }
            """.trimIndent()
        )

        val result = importer.loadConfigsFromFile(configFile)

        assertEquals(defaultValue, result.flavorMap["strawberry"]!![keyName])
        assertEquals(defaultValue, result.flavorMap["apple"]!![keyName])
    }

    @Test
    fun givenDefaultValuesAndFlavorSpecifics_whenImportingFile_shouldIncludeAllFlavors() {
        configFile.writeText(
            """
            {
            "${ConfigurationFileImporter.KEY_FLAVORS}": {
                    "strawberry": {},            
                    "apple": {},            
                    "banana": {
                        "color": "yellow"
                    }           
                },
                "color": "red"
            }
            """.trimIndent()
        )

        val result = importer.loadConfigsFromFile(configFile)
        assertTrue(result.flavorMap.keys.containsAll(setOf("strawberry", "apple", "banana")))
    }
}
