/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */

package com.wire.android.ui

import java.io.File
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WireActivityNavigation3EffectsSourceTest {

    @Test
    fun givenNavigation3Effects_whenInspectingSource_thenNoLegacyNavigationApiLeaksIn() {
        val source = source()
        listOf(
            "Navigator",
            "NavController",
            "ScreenDestination",
            "SavedStateHandle",
            "safeRoute",
            "com.ramcosta.composedestinations",
        ).forEach { forbidden ->
            assertFalse(source.contains(forbidden), "Navigation 3 effects contain $forbidden")
        }
        assertTrue(source.contains("WireNavigation3Runtime"))
        assertTrue(source.contains("current.copy("))
        assertTrue(source.contains("entryId = current.entryId"))
        assertTrue(source.contains("flowId = current.flowId"))
    }

    @Test
    fun givenAllActivityActions_whenInspectingResolver_thenEveryActionIsHandled() {
        val source = source()
        listOf(
            "OnAuthorizationNeeded",
            "OnMigrationLogin",
            "OnAutomaticLogin",
            "OnCustomBackendLogin",
            "OnOpenUserProfile",
            "OnSSOLogin",
            "OnShowImportMediaScreen",
            "OpenConversation",
            "OnUnknownDeepLink",
            "ShowToast",
        ).forEach { action ->
            assertTrue(source.contains(action), "Missing typed handling for $action")
        }
    }

    private fun source(): String {
        val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "app/src/main/kotlin").isDirectory }
        return File(
            root,
            "app/src/main/kotlin/com/wire/android/ui/WireActivityNavigation3Effects.kt",
        ).readText()
    }
}
