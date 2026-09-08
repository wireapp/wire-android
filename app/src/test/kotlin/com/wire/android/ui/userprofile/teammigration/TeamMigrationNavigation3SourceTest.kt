/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.userprofile.teammigration

import java.io.File
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TeamMigrationNavigation3SourceTest {

    @Test
    fun givenTeamMigrationContract_whenInspectingImports_thenItIsKmpSourcePure() {
        val source = sourceFile("TeamMigrationNavigation3.kt").readText()
        val imports = source
            .lineSequence()
            .filter { it.startsWith("import ") }
            .map { it.removePrefix("import ") }
            .toList()

        listOf("android.", "androidx.", "com.ramcosta.", "com.wire.kalium.").forEach { forbidden ->
            assertFalse(imports.any { it.startsWith(forbidden) }, "Contract imports $forbidden")
        }
        listOf("Bundle", "SavedStateHandle", "DEFAULT_ARGS_KEY", "NavArgs").forEach { forbidden ->
            assertFalse(source.contains(forbidden), "Contract references $forbidden")
        }
    }

    @Test
    fun givenTeamMigrationContribution_whenInspectingEntries_thenAllRoutesAndFlowOwnerAreUsed() {
        val source = sourceFile("TeamMigrationNavigation3Entries.kt").readText()

        listOf(
            "wireEntry<TeamMigrationTeamPlanRoute>",
            "wireEntry<TeamMigrationTeamNameRoute>",
            "wireEntry<TeamMigrationConfirmationRoute>",
            "wireEntry<TeamMigrationDoneRoute>",
            "wireViewModelStoreOwner(WireViewModelOwner.Flow(flowId))",
            "teamMigrationViewModel(viewModelStoreOwner = flowOwner)",
            "WireBackStackMode.REMOVE_CURRENT_NESTED_FLOW",
        ).forEach { expected ->
            assertTrue(source.contains(expected), "Missing $expected")
        }
        assertFalse(source.contains("ScreenDestination"))
        assertFalse(source.contains("com.ramcosta.composedestinations"))
        assertFalse(source.contains("Bundle"))
        assertFalse(source.contains("DEFAULT_ARGS_KEY"))
    }

    @Test
    fun givenLegacyScreens_whenInspectingAdapters_thenBothRuntimesShareNeutralContent() {
        listOf(
            Triple(
                "step1/TeamMigrationTeamPlanStepScreen.kt",
                "TeamMigrationTeamPlanRouteScreen",
                "TeamMigrationTeamPlanStepScreenContent(",
            ),
            Triple(
                "step2/TeamMigrationTeamNameStepScreen.kt",
                "TeamMigrationTeamNameRouteScreen",
                "TeamMigrationTeamNameStepScreenContent(",
            ),
            Triple(
                "step3/TeamMigrationConfirmationStepScreen.kt",
                "TeamMigrationConfirmationRouteScreen",
                "TeamMigrationConfirmationStepScreenContent(",
            ),
            Triple(
                "step4/TeamMigrationDoneStepScreen.kt",
                "TeamMigrationDoneRouteScreen",
                "TeamMigrationDoneStepContent(",
            ),
        ).forEach { (path, adapter, content) ->
            val source = sourceFile(path).readText()
            assertTrue(source.contains("internal fun $adapter"))
            assertTrue(source.substringAfter("internal fun $adapter").contains(content))
        }
    }

    private fun sourceFile(relativePath: String): File {
        val projectDir = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "app/src/main/kotlin").isDirectory }
        return File(
            projectDir,
            "app/src/main/kotlin/com/wire/android/ui/userprofile/teammigration/$relativePath",
        ).also {
            assertTrue(it.isFile, "Missing source file $relativePath")
        }
    }
}
