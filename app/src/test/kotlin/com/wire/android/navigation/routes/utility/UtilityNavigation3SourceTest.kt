/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.navigation.routes.utility

import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UtilityNavigation3SourceTest {

    @Test
    fun givenUtilityEntries_whenInspectingSource_thenAllEntriesAreTypedAndNavigation2Free() {
        val source = sourceFile("UtilityNavigation3Entries.kt").readText()

        assertEquals(6, Regex("""wireEntry<""").findAll(source).count())
        listOf(
            "wireEntry<InitialSyncRoute>",
            "wireEntry<DebugRoute>",
            "wireEntry<LogManagementRoute>",
            "wireEntry<DebugFeatureFlagsRoute>",
            "wireEntry<ConversationCryptoStatsRoute>",
            "wireEntry<SecurityProvidersRoute>",
        ).forEach { registration -> assertTrue(registration in source, registration) }
        listOf(
            "com.ramcosta",
            "NavController",
            "com.wire.android.navigation.Navigator",
            "com.wire.android.navigation.NavigationCommand",
            "Bundle",
            "SavedStateHandle",
            "DEFAULT_ARGS_KEY",
        ).forEach { forbidden -> assertFalse(forbidden in source, forbidden) }
    }

    @Test
    fun givenUtilityRoutes_whenInspectingSource_thenContractsRemainKmpReady() {
        val source = sourceFile("UtilityRoutes.kt").readText()
        val imports = source.lineSequence()
            .filter { it.startsWith("import ") }
            .toList()

        assertEquals(6, Regex("""\) : SessionRoute""").findAll(source).count())
        assertFalse(imports.any { it.startsWith("import android.") })
        assertFalse(imports.any { it.startsWith("import androidx.") })
        assertFalse(imports.any { it.startsWith("import com.ramcosta") })
        assertFalse(imports.any { it.startsWith("import com.wire.kalium") })
    }

    private fun sourceFile(name: String): File {
        val relative = "src/main/kotlin/com/wire/android/navigation/routes/utility/$name"
        return sequenceOf(
            File(relative),
            File("app/$relative"),
            File("../app/$relative"),
        ).first(File::isFile)
    }
}
