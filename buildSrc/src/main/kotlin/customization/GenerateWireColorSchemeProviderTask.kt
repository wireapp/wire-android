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

import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class GenerateWireColorSchemeProviderTask : DefaultTask() {
    @get:Input
    abstract val flavor: Property<String>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val baseConfigFile: RegularFileProperty

    @get:Optional
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val customConfigFile: RegularFileProperty

    @get:Optional
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val repoThemeDirectory: DirectoryProperty

    @get:Optional
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val customThemeDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val selectedPalette = ConfigurationFileImporter()
            .loadConfigsFromFiles(
                baseConfigFile = baseConfigFile.asFile.get(),
                customConfigFile = customConfigFile.asFile.orNull
            )
            .flavorMap[flavor.get()]
            ?.get(FeatureConfigs.WIRE_COLOR_SCHEME.jsonKey) as? String

        val source = when {
            selectedPalette.isNullOrBlank() || selectedPalette == DEFAULT_SCHEME -> defaultProviderSource()
            else -> customProviderSource(loadPalette(selectedPalette))
        }

        val outputFile = outputDir.asFile.get()
            .resolve(PACKAGE_NAME.replace('.', '/'))
            .resolve("$CLASS_NAME.kt")
        outputFile.parentFile.mkdirs()
        outputFile.writeText(source)
    }

    private fun loadPalette(paletteName: String): PaletteOverrides {
        val candidateFiles = listOfNotNull(
            customThemeDirectory.asFile.orNull?.resolve("$paletteName.json"),
            repoThemeDirectory.asFile.orNull?.resolve("$paletteName.json")
        )

        val paletteFile = candidateFiles.firstOrNull(File::exists)
            ?: error(
                buildString {
                    append("Missing wire color scheme palette '$paletteName'. ")
                    append("Expected ")
                    append(candidateFiles.joinToString(" or ") { it.absolutePath })
                }
            )

        val parsed = JsonSlurper().parse(paletteFile) as? Map<*, *>
            ?: error("Palette file '${paletteFile.absolutePath}' must contain a JSON object.")

        return PaletteOverrides(
            light = parseColorSection(parsed["light"], "light", paletteFile),
            dark = parseColorSection(parsed["dark"], "dark", paletteFile)
        )
    }

    private fun parseColorSection(
        rawSection: Any?,
        sectionName: String,
        paletteFile: File
    ): Map<String, String> {
        if (rawSection == null) return emptyMap()
        val section = rawSection as? Map<*, *>
            ?: error("Palette section '$sectionName' in '${paletteFile.absolutePath}' must be a JSON object.")
        return section.entries.associate { (rawKey, rawValue) ->
            val jsonKey = rawKey as? String
                ?: error("Palette section '$sectionName' in '${paletteFile.absolutePath}' contains a non-string key.")
            val propertyName = jsonKey.toLowerCamelCase()
            require(propertyName in SUPPORTED_COLOR_PROPERTIES) {
                "Unsupported WireColorScheme color key '$jsonKey' in '${paletteFile.absolutePath}'."
            }
            val colorValue = rawValue as? String
                ?: error("Palette key '$jsonKey' in '${paletteFile.absolutePath}' must be a string.")
            propertyName to colorValue.toColorExpression(jsonKey, paletteFile)
        }
    }

    private fun customProviderSource(overrides: PaletteOverrides): String = """
        package $PACKAGE_NAME

        import androidx.compose.foundation.isSystemInDarkTheme
        import androidx.compose.runtime.Composable
        import androidx.compose.ui.graphics.Color
        import com.wire.android.ui.theme.WireColorScheme
        import com.wire.android.ui.theme.WireColorSchemeTypes

        internal object $CLASS_NAME {
            internal val light: WireColorScheme = ${renderColorScheme("WireColorSchemeTypes.light", overrides.light)}

            internal val dark: WireColorScheme = ${renderColorScheme("WireColorSchemeTypes.dark", overrides.dark)}

            internal val currentTheme: WireColorScheme
                @Composable get() = if (isSystemInDarkTheme()) dark else light
        }
    """.trimIndent()

    private fun defaultProviderSource(): String = """
        package $PACKAGE_NAME

        import androidx.compose.foundation.isSystemInDarkTheme
        import androidx.compose.runtime.Composable
        import com.wire.android.ui.theme.WireColorScheme
        import com.wire.android.ui.theme.WireColorSchemeTypes

        internal object $CLASS_NAME {
            internal val light: WireColorScheme = WireColorSchemeTypes.light

            internal val dark: WireColorScheme = WireColorSchemeTypes.dark

            internal val currentTheme: WireColorScheme
                @Composable get() = if (isSystemInDarkTheme()) dark else light
        }
    """.trimIndent()

    private fun renderColorScheme(defaultExpression: String, overrides: Map<String, String>): String =
        if (overrides.isEmpty()) {
            defaultExpression
        } else {
            overrides.entries.joinToString(
                prefix = "$defaultExpression.copy(\n",
                postfix = "\n)",
                separator = ",\n"
            ) { (propertyName, colorExpression) ->
                "    $propertyName = $colorExpression"
            }
        }

    private fun String.toLowerCamelCase(): String =
        split('_')
            .filter(String::isNotBlank)
            .mapIndexed { index, part ->
                if (index == 0) {
                    part.lowercase()
                } else {
                    part.lowercase().replaceFirstChar(Char::titlecase)
                }
            }
            .joinToString("")

    private fun String.toColorExpression(jsonKey: String, paletteFile: File): String {
        val normalized = trim()
        require(HEX_COLOR_REGEX.matches(normalized)) {
            "Palette key '$jsonKey' in '${paletteFile.absolutePath}' must use #RRGGBB or #AARRGGBB."
        }
        val hexDigits = normalized.removePrefix("#")
        val argbHex = if (hexDigits.length == 6) "FF$hexDigits" else hexDigits
        return "Color(0x${argbHex.uppercase()})"
    }

    private data class PaletteOverrides(
        val light: Map<String, String>,
        val dark: Map<String, String>
    )

    companion object {
        private const val DEFAULT_SCHEME = "default"
        private const val PACKAGE_NAME = "com.wire.android.generated"
        private const val CLASS_NAME = "GeneratedWireColorSchemeProvider"
        private val HEX_COLOR_REGEX = Regex("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$")
        private val SUPPORTED_COLOR_PROPERTIES = setOf(
            "primary",
            "onPrimary",
            "primaryVariant",
            "onPrimaryVariant",
            "inversePrimary",
            "error",
            "onError",
            "errorVariant",
            "onErrorVariant",
            "warning",
            "onWarning",
            "highlight",
            "onHighlight",
            "positive",
            "onPositive",
            "positiveVariant",
            "onPositiveVariant",
            "secondaryText",
            "background",
            "onBackground",
            "surface",
            "onSurface",
            "surfaceVariant",
            "onSurfaceVariant",
            "inverseSurface",
            "inverseOnSurface",
            "surfaceBright",
            "surfaceDim",
            "surfaceContainerLowest",
            "surfaceContainerLow",
            "surfaceContainer",
            "surfaceContainerHigh",
            "surfaceContainerHighest",
            "primaryButtonEnabled",
            "onPrimaryButtonEnabled",
            "primaryButtonDisabled",
            "onPrimaryButtonDisabled",
            "primaryButtonSelected",
            "onPrimaryButtonSelected",
            "primaryButtonRipple",
            "secondaryButtonEnabled",
            "onSecondaryButtonEnabled",
            "secondaryButtonEnabledOutline",
            "secondaryButtonDisabled",
            "onSecondaryButtonDisabled",
            "secondaryButtonDisabledOutline",
            "secondaryButtonSelected",
            "onSecondaryButtonSelected",
            "secondaryButtonSelectedOutline",
            "secondaryButtonRipple",
            "tertiaryButtonEnabled",
            "onTertiaryButtonEnabled",
            "tertiaryButtonDisabled",
            "onTertiaryButtonDisabled",
            "tertiaryButtonSelected",
            "onTertiaryButtonSelected",
            "tertiaryButtonSelectedOutline",
            "tertiaryButtonRipple",
            "outline",
            "divider",
            "scrim",
            "emojiBackgroundColor",
            "defaultSelectedItemInLoadingState",
            "onScrim",
            "bubblesBackground"
        )
    }
}
