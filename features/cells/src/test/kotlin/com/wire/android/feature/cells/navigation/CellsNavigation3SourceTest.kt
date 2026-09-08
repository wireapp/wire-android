/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.feature.cells.navigation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.io.File

class CellsNavigation3SourceTest {

    @Test
    fun givenCellsEntryProvider_whenRegistrationsAreCounted_thenAllDestinationsAreCovered() {
        val source = sourceFile("CellsNavigation3Entries.kt").readText()
        val registrationCount = Regex("""wireEntry<""").findAll(source).count()

        assertEquals(CellsNavigation3Contribution.ROUTE_REGISTRATION_COUNT, registrationCount)
    }

    @Test
    fun givenKmpRouteContracts_whenInspected_thenTheyDoNotDependOnAndroidOrLegacyNavigation() {
        val source = sourceFile("CellsNavigation3.kt").readText()

        assertFalse(source.contains("import android."))
        assertFalse(source.contains("import com.ramcosta"))
        assertFalse(source.contains("SavedStateHandle"))
        assertFalse(source.contains("CellFilesNavArgs"))
    }

    @Test
    fun givenNavigation3RuntimeFiles_whenInspected_thenTheyDoNotDependOnComposeDestinationsOrNav2() {
        listOf(
            sourceFile("CellsNavigation3Entries.kt"),
            sourceFile("CellsNavigation3Renderer.kt"),
        ).forEach { file ->
            val source = file.readText()
            assertFalse(source.contains("com.ramcosta"), file.name)
            assertFalse(source.contains("WireNavigator"), file.name)
            assertFalse(source.contains("com.wire.android.navigation.NavigationCommand"), file.name)
        }
    }

    @Test
    fun givenCellsProductionSources_whenInspected_thenLegacyNavigationIsAbsent() {
        val navigationDirectory = requireNotNull(sourceFile("CellsNavigation3.kt").parentFile)
        val cellsSourceDirectory = requireNotNull(navigationDirectory.parentFile)
        val mainSources = cellsSourceDirectory
            .walkTopDown()
            .filter(File::isFile)
            .filter { it.extension == "kt" }

        mainSources.forEach { file ->
            val source = file.readText()
            assertFalse(source.contains("com.ramcosta"), file.path)
            assertFalse(source.contains("WireNavigator"), file.path)
            assertFalse(source.contains("WireCellsDestination"), file.path)
            assertFalse(source.contains("com.wire.android.navigation.NavigationCommand"), file.path)
        }
    }

    private fun sourceFile(name: String): File =
        generateSequence(File(System.getProperty("user.dir"))) { current ->
            current.parentFile
        }
            .map { File(it, "src/main/java/com/wire/android/feature/cells/navigation/$name") }
            .firstOrNull(File::isFile)
            ?: error("Could not find $name from ${System.getProperty("user.dir")}")
}
