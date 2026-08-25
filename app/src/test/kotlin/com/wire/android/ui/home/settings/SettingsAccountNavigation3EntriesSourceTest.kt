/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.home.settings

import java.io.File
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SettingsAccountNavigation3EntriesSourceTest {

    @Test
    fun givenSettingsAccountContribution_whenInspectingSource_thenAllTypedEntriesAndResultAreRegistered() {
        val entries = sourceFile("SettingsAccountNavigation3Entries.kt").readText()

        listOf(
            "wireEntry<AboutThisAppRoute>",
            "wireEntry<MyAccountRoute>",
            "wireEntry<ChangeEmailRoute>",
            "wireEntry<VerifyEmailRoute>",
            "wireEntry<ChangeUserColorRoute>",
            "wireEntry<ChangeHandleRoute>",
            "wireEntry<ChangeDisplayNameRoute>",
        ).forEach { registration ->
            assertTrue(entries.contains(registration), "Missing $registration")
        }
        assertTrue(entries.contains("SettingsAccountUpdateNavigation3ResultType"))

        val contribution = sourceFile("SettingsNavigation3Entries.kt").readText()
        assertTrue(contribution.contains("settingsAccountNavigation3Entries(runtime, actions)"))
        assertTrue(contribution.contains("listOf(SettingsAccountUpdateNavigation3ResultType)"))
    }

    @Test
    fun givenSettingsAccountEntries_whenInspectingSource_thenNoLegacyArgumentBridgeIsUsed() {
        val entries = sourceFile("SettingsAccountNavigation3Entries.kt").readText()

        listOf(
            "com.ramcosta.composedestinations",
            "ScreenDestination",
            "SavedStateHandle",
            "Bundle",
            "DEFAULT_ARGS_KEY",
            ".navArgs()",
        ).forEach { forbidden ->
            assertFalse(entries.contains(forbidden), "Entries must not reference $forbidden")
        }
        assertTrue(entries.contains("verifyEmailViewModel(route.toViewModelArgs())"))
        assertTrue(entries.contains("navigateForResult("))
        assertTrue(entries.contains("completeCurrentAndPop("))
    }

    @Test
    fun givenSettingsRouteContract_whenInspectingImports_thenItRemainsKmpSourcePure() {
        val source = sourceFile("SettingsNavigation3.kt").readText()
        listOf("android.", "androidx.", "com.ramcosta.", "com.wire.kalium.").forEach { prefix ->
            assertFalse(
                source.lineSequence()
                    .filter { it.startsWith("import ") }
                    .any { it.removePrefix("import ").startsWith(prefix) },
                "SettingsNavigation3.kt must not import $prefix",
            )
        }
    }

    @Test
    fun givenEmailUpdateCompletes_whenInspectingScreen_thenTerminalNavigationRunsAsAnEffect() {
        val source = sourceFile(
            "account/email/updateEmail/ChangeEmailScreen.kt"
        ).readText()

        assertTrue(source.contains("LaunchedEffect(flowState)"))
        assertTrue(source.contains("onVerifyEmail(flowState.newEmail)"))
    }

    private fun sourceFile(relativePath: String): File {
        val userDir = requireNotNull(System.getProperty("user.dir"))
        val projectDir = generateSequence(File(userDir)) { it.parentFile }
            .first { File(it, "app/src/main/kotlin").isDirectory }
        return File(
            projectDir,
            "app/src/main/kotlin/com/wire/android/ui/home/settings/$relativePath",
        ).also {
            assertTrue(it.isFile, "Missing source file $relativePath")
        }
    }
}
