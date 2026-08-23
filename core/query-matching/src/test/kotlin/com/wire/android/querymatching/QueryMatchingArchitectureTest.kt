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

package com.wire.android.querymatching

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import java.io.File
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class QueryMatchingArchitectureTest {

    @Test
    fun queryMatchingProductionScopeIsNotEmpty() {
        assertTrue(
            productionSourceFiles().isNotEmpty(),
            "The :core:query-matching production scope must not be empty.",
        )
    }

    @Test
    fun queryMatchingProductionDeclarationsRemainInTheUtilPackage() {
        assertTrue(
            productionSourceFiles().all { it.hasPackage(productionPackage) },
            "Every :core:query-matching production file must declare $productionPackage.",
        )
    }

    @Test
    fun queryMatchingProductionHasNoImports() {
        productionSourceFiles().assertFalse { sourceFile ->
            sourceFile.hasImport { true }
        }
    }

    @Test
    fun queryMatchingDoesNotDeclareAUiCommonGradleDependency() {
        val buildScript = moduleBuildScriptText()

        assertFalse(
            forbiddenUiCommonProjectEdges.any { it.containsMatchIn(buildScript) },
            ":core:query-matching must not depend on :core:ui-common.",
        )
    }

    @Test
    fun queryMatchingDisablesMetroCompilerParticipationAndRuntimeInjection() {
        val buildScript = moduleBuildScriptText()

        assertTrue(
            metroCompilerOptOut.containsMatchIn(buildScript),
            ":core:query-matching must set metro.enabled to false.",
        )
        assertTrue(
            metroRuntimeOptOut.containsMatchIn(buildScript),
            ":core:query-matching must set metro.automaticallyAddRuntimeDependencies to false.",
        )
    }

    private fun productionSourceFiles() = Konsist.scopeFromDirectory(productionSourceDirectory).files

    private fun moduleBuildScriptText(): String {
        assertTrue(moduleBuildScript.isFile, "Missing :core:query-matching build.gradle.kts.")
        return moduleBuildScript.readText()
    }

    private companion object {
        const val productionSourceDirectory = "core/query-matching/src/main/kotlin"
        const val productionPackage = "com.wire.android.util"
        val moduleBuildScript = File(Konsist.projectRootPath, "core/query-matching/build.gradle.kts")
        val forbiddenUiCommonProjectEdges = listOf(
            Regex("""projects\s*\.\s*core\s*\.\s*uiCommon\b"""),
            Regex("""project\s*\(\s*(?:path\s*=\s*)?[\"']:\s*core\s*:\s*ui-common[\"']"""),
        )
        val metroCompilerOptOut = Regex("""metro\s*\{(?s:.*?)enabled\s*\.\s*set\s*\(\s*false\s*\)""")
        val metroRuntimeOptOut = Regex(
            """metro\s*\{(?s:.*?)automaticallyAddRuntimeDependencies\s*\.\s*set\s*\(\s*false\s*\)""",
        )
    }
}
