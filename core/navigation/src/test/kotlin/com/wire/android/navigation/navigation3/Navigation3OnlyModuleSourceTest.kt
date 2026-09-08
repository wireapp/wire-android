/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.navigation.navigation3

import java.io.File
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class Navigation3OnlyModuleSourceTest {

    @Test
    fun givenNavigationModule_whenInspectingSourcesAndDependencies_thenLegacyNavigationCannotReturn() {
        val moduleRoot = moduleRoot()
        val productionSources = File(moduleRoot, "src/main/kotlin")
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()

        productionSources.forEach { sourceFile ->
            val source = sourceFile.readText()
            listOf(
                "import com.ramcosta",
                "import androidx.navigation.NavController",
                "import androidx.navigation.NavBackStackEntry",
                "DestinationStyle",
                "DestinationWrapper",
            ).forEach { forbidden ->
                assertFalse(
                    forbidden in source,
                    "${sourceFile.relativeTo(moduleRoot)} still contains $forbidden",
                )
            }
        }

        val buildScript = File(moduleRoot, "build.gradle.kts").readText()
        listOf(
            "libs.compose.navigation",
            "libs.compose.destinations",
        ).forEach { forbidden ->
            assertFalse(forbidden in buildScript, "build.gradle.kts still contains $forbidden")
        }
    }

    private fun moduleRoot(): File =
        generateSequence(File(checkNotNull(System.getProperty("user.dir")))) { it.parentFile }
            .first { File(it, "src/main/kotlin/com/wire/android/navigation").isDirectory }
}
