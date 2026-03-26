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

package customization

import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertTrue

@RunWith(JUnit4::class)
class GenerateWireColorSchemeProviderTaskTest {
    @Suppress("VulnerableCodeUsages") // We don't write sensitive info, and this is patched in newer JDKs
    @get:Rule
    val folder: TemporaryFolder = TemporaryFolder()

    @Test
    fun givenNoConfiguredPalette_whenGeneratingProvider_shouldUseDefaultThemeTypes() {
        val task = newTask()
        val configFile = folder.newFile("default.json").apply {
            writeText(
                """
                {
                  "flavors": {
                    "dev": {}
                  }
                }
                """.trimIndent()
            )
        }

        task.flavor.set("dev")
        task.baseConfigFile.set(configFile)
        task.outputDir.set(folder.newFolder("generated-default"))

        task.generate()

        val generated = generatedProviderText("generated-default")
        assertTrue(generated.contains("internal val light: WireColorScheme = WireColorSchemeTypes.light"))
        assertTrue(generated.contains("internal val dark: WireColorScheme = WireColorSchemeTypes.dark"))
    }

    @Test
    fun givenConfiguredPalette_whenGeneratingProvider_shouldCopyPaletteOverridesIntoGeneratedSource() {
        val task = newTask()
        val configFile = folder.newFile("default-with-palette.json").apply {
            writeText(
                """
                {
                  "wire_color_scheme": "acme",
                  "flavors": {
                    "dev": {}
                  }
                }
                """.trimIndent()
            )
        }
        val themeDir = folder.newFolder("theme")
        themeDir.resolve("acme.json").writeText(
            """
            {
              "light": {
                "primary": "#0057B8",
                "on_primary": "#FFFFFF"
              },
              "dark": {
                "primary": "#76B8FF"
              }
            }
            """.trimIndent()
        )

        task.flavor.set("dev")
        task.baseConfigFile.set(configFile)
        task.repoThemeDirectory.set(themeDir)
        task.outputDir.set(folder.newFolder("generated-custom"))

        task.generate()

        val generated = generatedProviderText("generated-custom")
        assertTrue(generated.contains("WireColorSchemeTypes.light.copy("))
        assertTrue(generated.contains("primary = Color(0xFF0057B8)"))
        assertTrue(generated.contains("onPrimary = Color(0xFFFFFFFF)"))
        assertTrue(generated.contains("WireColorSchemeTypes.dark.copy("))
        assertTrue(generated.contains("primary = Color(0xFF76B8FF)"))
    }

    private fun newTask(): GenerateWireColorSchemeProviderTask =
        ProjectBuilder.builder()
            .withProjectDir(folder.newFolder())
            .build()
            .tasks
            .register("generateWireColorSchemeProvider", GenerateWireColorSchemeProviderTask::class.java)
            .get()

    private fun generatedProviderText(outputDirectoryName: String): String =
        folder.root
            .resolve(outputDirectoryName)
            .resolve("com/wire/android/generated/GeneratedWireColorSchemeProvider.kt")
            .readText()
}
