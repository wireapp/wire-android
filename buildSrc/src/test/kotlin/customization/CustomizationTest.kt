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

@RunWith(JUnit4::class)
class CustomizationTest {

    @Suppress("VulnerableCodeUsages") // We don't write sensitive info, and this is patched in newer JDKs
    @get:Rule
    val rootDir: TemporaryFolder = TemporaryFolder()
    private val defaultFile get() = File(rootDir.root, "default.json")
    private val customFile get() = File(rootDir.root, "custom.json")

    @Test
    fun givenNoCustomization_whenGettingBuildInfo_thenShouldOnlyUseDefaultValues() {
        defaultFile.writeText(
            """
            {
                "${ConfigurationFileImporter.KEY_FLAVORS}": {
                    "dev": {}
                },
                "foo": "default" 
            }
            """.trimIndent()
        )
        customFile.writeText(
            """
            {
                "${ConfigurationFileImporter.KEY_FLAVORS}": {
                    "dev": {}
                },
                "foo": "custom" 
            }
            """.trimIndent()
        )

        val result = Customization.getBuildtimeConfiguration(
            rootDir.root,
            Customization.CustomizationOption.DefaultOnly
        )

        assertEquals("default", result.flavorSettings.flavorMap["dev"]!!["foo"])
    }

    @Test
    fun givenCustomizationOverwrite_whenGettingBuildInfo_thenShouldOverwriteDefaultValueUsingTheSpecifiedFile() {
        defaultFile.writeText(
            """
            {
                "${ConfigurationFileImporter.KEY_FLAVORS}": {
                    "dev": {}
                },
                "foo": "default" 
            }
            """.trimIndent()
        )
        customFile.writeText(
            """
            {
                "${ConfigurationFileImporter.KEY_FLAVORS}": {
                    "dev": {}
                },
                "foo": "custom" 
            }
            """.trimIndent()
        )

        val result = Customization.getBuildtimeConfiguration(
            rootDir.root,
            Customization.CustomizationOption.FromFile(customFile)
        )

        assertEquals("custom", result.flavorSettings.flavorMap["dev"]!!["foo"])
    }
}
