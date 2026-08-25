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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WireActivityStructureSourceTest {

    @Test
    fun givenWireActivity_whenInspectingStructure_thenComposeAndDialogOrchestrationStayExtracted() {
        val activity = uiSource("WireActivity.kt")
        val host = uiSource("WireActivityNavigation3Host.kt")
        val dialogActions = uiSource("WireActivityNavigation3DialogActions.kt")

        assertTrue(activity.lineSequence().count() < 500, "WireActivity became an orchestration container again")
        assertTrue("WireActivityNavigation3Host(" in activity)
        assertFalse("@Composable" in activity, "WireActivity must not own composable host functions")
        assertFalse("WireActivityDialogs(" in activity, "WireActivity must not assemble global dialogs")
        assertTrue("WireNavigation3ProductionHost(" in host)
        assertTrue("navigation3DialogActions(" in host)
        assertTrue("WireActivityDialogActionDependencies(" in dialogActions)
    }

    @Test
    fun givenExtractedActivityHost_whenInspectingSources_thenLegacyHostApisCannotReturn() {
        val sources = listOf(
            uiSource("WireActivity.kt"),
            uiSource("WireActivityNavigation3Host.kt"),
            uiSource("WireActivityNavigation3DialogActions.kt"),
        )
        listOf(
            "MainNavHost",
            "DestinationsNavHost",
            "rememberNavController",
            "NavController",
            "com.ramcosta",
            ".generated.",
            "ScreenDestination",
            "com.wire.android.navigation.NavigationCommand",
        ).forEach { forbidden ->
            assertFalse(
                sources.any { forbidden in it },
                "WireActivity host layer contains legacy API: $forbidden",
            )
        }
    }

    private fun uiSource(name: String): String {
        val root = generateSequence(File(checkNotNull(System.getProperty("user.dir")))) { it.parentFile }
            .first { File(it, "app/src/main/kotlin").isDirectory }
        return File(root, "app/src/main/kotlin/com/wire/android/ui/$name").readText()
    }
}
