/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.navigation.runtime

import java.io.File
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class Navigation3LegacyScreenCleanupSourceTest {

    @Test
    fun givenNavigation3Runtime_whenInspectingReleaseConfiguration_thenComposeDestinationsArtifactsCannotReturn() {
        listOf(
            File(projectRoot(), "app/proguard-rules.pro"),
            File(projectRoot(), "app/src/main/baseline-prof.txt"),
            File(projectRoot(), "app/src/main/startup-prof.txt"),
        ).forEach { configurationFile ->
            assertFalse(
                "com.ramcosta.composedestinations" in configurationFile.readText(),
                "${configurationFile.relativeTo(projectRoot())} still references Compose Destinations",
            )
        }
    }

    @Test
    fun givenMigratedScreenScopes_whenInspectingProductionSources_thenLegacyNavigationCannotReturn() {
        migratedSources().forEach { sourceFile ->
            val source = sourceFile.readText()
            listOf(
                "import com.ramcosta",
                "import com.wire.android.navigation.Navigator",
                "import com.wire.android.navigation.NavigationCommand",
                "import com.wire.android.navigation.BackStackMode",
                "import com.wire.android.navigation.rememberNavigator",
                "import com.wire.android.navigation.annotation",
                "@WireRootDestination",
                "@WireHomeDestination",
                "@WirePersonalToTeamMigrationDestination",
            ).forEach { forbidden ->
                assertFalse(
                    forbidden in source,
                    "${sourceFile.relativeTo(projectRoot())} still contains $forbidden",
                )
            }
        }
    }

    private fun migratedSources(): List<File> {
        val main = File(projectRoot(), "app/src/main/kotlin/com/wire/android/ui")
        val roots = listOf(
            File(main, "userprofile"),
            File(main, "debug"),
            File(main, "initialsync"),
            File(main, "sharing"),
            File(main, "e2eiEnrollment"),
            File(main, "home/vault"),
            File(main, "home/archive"),
            File(main, "home/conversationslist/all"),
            File(main, "home/whatsnew"),
            File(main, "home/settings"),
            File(main, "settings"),
        )
        val files = roots.flatMap { root ->
            root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        }
        return files + listOf(
            File(main, "home/cell/GlobalCellsScreen.kt"),
        )
    }

    private fun projectRoot(): File =
        generateSequence(File(requireNotNull(System.getProperty("user.dir")))) { it.parentFile }
            .first { File(it, "app/src/main/kotlin").isDirectory }
}
