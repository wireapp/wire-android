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

package com.wire.android.designsystem

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import java.io.File
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DesignSystemArchitectureTest {

    @Test
    fun designSystemProductionScopeIsNotEmpty() {
        val sourceFiles = productionSourceFiles()

        assertTrue(sourceFiles.isNotEmpty(), "The :core:design-system production scope must not be empty.")
    }

    @Test
    fun designSystemProductionDeclarationsRemainInTheThemePackage() {
        val sourceFiles = productionSourceFiles()

        assertTrue(
            sourceFiles.all { it.hasPackage(themePackage) },
            "Every :core:design-system production file must declare $themePackage.",
        )
    }

    @Test
    fun designSystemDoesNotImportForbiddenArchitectureApis() {
        val sourceFiles = productionSourceFiles()

        sourceFiles.assertFalse { sourceFile ->
            sourceFile.hasImport { importedDeclaration ->
                forbiddenImportPrefixes.any { forbiddenPrefix ->
                    importedDeclaration.name == forbiddenPrefix ||
                            importedDeclaration.name.startsWith("$forbiddenPrefix.")
                } || importedDeclaration.name.substringAfterLast('.') in forbiddenSimpleImportNames
            }
        }
    }

    @Test
    fun designSystemDoesNotDeclareAUiCommonGradleDependency() {
        val buildScript = moduleBuildScriptText()
        assertFalse(
            forbiddenUiCommonProjectEdges.any { it.containsMatchIn(buildScript) },
            ":core:design-system must not depend on :core:ui-common.",
        )
    }

    @Test
    fun designSystemDisablesMetroCompilerParticipationAndRuntimeInjection() {
        assertTrue(
            metroCompilerOptOut.containsMatchIn(moduleBuildScriptText()),
            ":core:design-system must set metro.enabled to false.",
        )
        assertTrue(
            metroRuntimeOptOut.containsMatchIn(moduleBuildScriptText()),
            ":core:design-system must set metro.automaticallyAddRuntimeDependencies to false.",
        )
    }

    private companion object {
        const val productionSourceDirectory = "core/design-system/src/main/kotlin"
        const val themePackage = "com.wire.android.ui.theme"
        val moduleBuildScript = File(Konsist.projectRootPath, "core/design-system/build.gradle.kts")
        val forbiddenImportPrefixes = listOf(
            "com.wire.kalium",
            "com.wire.android.ui.common",
            "com.wire.android.di",
            "androidx.lifecycle",
            "dev.zacsweers.metro",
        )
        val forbiddenSimpleImportNames = setOf("CoreLogic")
        val forbiddenUiCommonProjectEdges = listOf(
            Regex("""projects\s*\.\s*core\s*\.\s*uiCommon\b"""),
            Regex("""project\s*\(\s*(?:path\s*=\s*)?[\"']:\s*core\s*:\s*ui-common[\"']"""),
        )
        val metroRuntimeOptOut = Regex(
            """metro\s*\{(?s:.*?)automaticallyAddRuntimeDependencies\s*\.\s*set\s*\(\s*false\s*\)""",
        )
        val metroCompilerOptOut = Regex(
            """metro\s*\{(?s:.*?)enabled\s*\.\s*set\s*\(\s*false\s*\)""",
        )
    }

    private fun productionSourceFiles() = Konsist.scopeFromDirectory(productionSourceDirectory).files

    private fun moduleBuildScriptText(): String {
        assertTrue(moduleBuildScript.isFile, "Missing :core:design-system build.gradle.kts.")
        return moduleBuildScript.readText()
    }
}
