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

package com.wire.android.interactionmodel

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import java.io.File
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InteractionModelArchitectureTest {

    @Test
    fun interactionModelProductionScopeIsNotEmpty() {
        assertTrue(
            productionSourceFiles().isNotEmpty(),
            "The :core:interaction-model production scope must not be empty.",
        )
    }

    @Test
    fun interactionModelProductionDeclarationsRemainInTheModelPackage() {
        assertTrue(
            productionSourceFiles().all { it.hasPackage(productionPackage) },
            "Every :core:interaction-model production file must declare $productionPackage.",
        )
    }

    @Test
    fun interactionModelProductionHasNoImports() {
        productionSourceFiles().assertFalse { sourceFile ->
            sourceFile.hasImport { true }
        }
    }

    @Test
    fun interactionModelDoesNotDeclareAUiCommonGradleDependency() {
        assertFalse(
            forbiddenUiCommonProjectEdges.any { it.containsMatchIn(moduleBuildScriptText()) },
            ":core:interaction-model must not depend on :core:ui-common.",
        )
    }

    @Test
    fun interactionModelDisablesComposeAndMetroConventionFeatures() {
        val buildScript = moduleBuildScriptText()

        assertTrue(
            composeOptOut.containsMatchIn(buildScript),
            ":core:interaction-model must set android.buildFeatures.compose to false.",
        )
        assertTrue(
            metroCompilerOptOut.containsMatchIn(buildScript),
            ":core:interaction-model must set metro.enabled to false.",
        )
        assertTrue(
            metroRuntimeOptOut.containsMatchIn(buildScript),
            ":core:interaction-model must set metro.automaticallyAddRuntimeDependencies to false.",
        )
    }

    private fun productionSourceFiles() = Konsist.scopeFromDirectory(productionSourceDirectory).files

    private fun moduleBuildScriptText(): String {
        assertTrue(moduleBuildScript.isFile, "Missing :core:interaction-model build.gradle.kts.")
        return moduleBuildScript.readText()
    }

    private companion object {
        const val productionSourceDirectory = "core/interaction-model/src/main/kotlin"
        const val productionPackage = "com.wire.android.model"
        val moduleBuildScript = File(Konsist.projectRootPath, "core/interaction-model/build.gradle.kts")
        val forbiddenUiCommonProjectEdges = listOf(
            Regex("""projects\s*\.\s*core\s*\.\s*uiCommon\b"""),
            Regex("""project\s*\(\s*(?:path\s*=\s*)?[\"']:\s*core\s*:\s*ui-common[\"']"""),
        )
        val composeOptOut = Regex("""buildFeatures\s*\{(?s:.*?)compose\s*=\s*false""")
        val metroCompilerOptOut = Regex("""metro\s*\{(?s:.*?)enabled\s*\.\s*set\s*\(\s*false\s*\)""")
        val metroRuntimeOptOut = Regex(
            """metro\s*\{(?s:.*?)automaticallyAddRuntimeDependencies\s*\.\s*set\s*\(\s*false\s*\)""",
        )
    }
}
