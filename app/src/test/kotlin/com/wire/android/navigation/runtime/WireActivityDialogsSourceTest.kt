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

class WireActivityDialogsSourceTest {

    @Test
    fun givenExtractedDialogLayer_whenInspectingImports_thenItIsNavigationNeutral() {
        val source = uiSource("WireActivityDialogs.kt").readText()
        val imports = source.lineSequence()
            .filter { it.startsWith("import ") }
            .toList()

        listOf(
            "Navigator",
            "NavigationCommand",
            "NavController",
            "com.ramcosta.composedestinations.generated",
            "ScreenDestination",
        ).forEach { forbidden ->
            assertFalse(
                imports.any { it.contains(forbidden) },
                "Dialog layer imports legacy navigation API: $forbidden",
            )
        }

        assertTrue(source.contains("internal data class WireActivityDialogActions"))
        listOf(
            "openLoginIfEmptyWelcomeStart",
            "openSelfProfile",
            "openSelfDevices",
            "switchAccountAndOpenSelfDevices",
            "openE2EICertificateDetails",
            "openJoinedConversation",
            "startTeamAppLock",
            "hardLogout",
        ).forEach { callback ->
            assertTrue(source.contains("val $callback"), "Missing semantic callback $callback")
        }
    }

    @Test
    fun givenWireActivityNavigation3Host_whenInspectingSource_thenDialogRenderingIsSemantic() {
        val activity = uiSource("WireActivity.kt").readText()
        val host = uiSource("WireActivityNavigation3Host.kt").readText()
        val actions = uiSource("WireActivityNavigation3DialogActions.kt").readText()

        assertFalse(activity.contains("private fun HandleDialogs("))
        assertFalse(activity.contains("fun HandleDialogs("))
        assertFalse(activity.contains("legacyDialogActions("))
        assertFalse(host.contains("legacyDialogActions("))
        assertTrue(host.contains("WireActivityDialogs("))
        assertTrue(host.contains("navigation3DialogActions("))
        assertTrue(actions.contains("internal fun navigation3DialogActions("))
        assertTrue(actions.contains("internal data class WireActivityDialogActionDependencies"))
    }

    private fun uiSource(name: String): File {
        val projectDir = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "app/src/main/kotlin").isDirectory }
        return File(projectDir, "app/src/main/kotlin/com/wire/android/ui/$name").also {
            assertTrue(it.isFile, "Missing source file $name")
        }
    }
}
