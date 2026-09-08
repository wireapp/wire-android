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

class WireActivityProductionNavigation3HostSourceTest {

    @Test
    fun givenProductionWireActivity_whenInspectingSource_thenNavigation3OwnsTheRuntimeAndHost() {
        val activity = wireActivitySource()
        val source = navigation3HostSource()

        assertTrue(
            "WireActivityNavigation3Host(" in activity,
            "WireActivity must delegate Compose orchestration to its Navigation 3 host",
        )

        listOf(
            "rememberWireNavigation3Runtime(",
            "rememberWireNavigation3ActivityGraphContext(",
            "MetroWireEntryGraphResolver(",
            "WireActivityGraphContext",
            "rememberWireNavigation3ProductionActions(",
            "WireNavigation3ProductionHost(",
        ).forEach { required ->
            assertTrue(required in source, "WireActivity is missing Navigation 3 host element: $required")
        }
    }

    @Test
    fun givenProductionWireActivity_whenInspectingSource_thenNavigation3OwnsSetupAndEffects() {
        val source = navigation3HostSource()

        listOf(
            "WireNavigation3ActivityEffects(",
            "WireNavigation3ActivityCallbacks(",
            "HandleNavigation3SessionEffects(",
            "WireActivityNavigation3Setup(",
            "HandleNavigation3ViewActions(",
        ).forEach { required ->
            assertTrue(required in source, "WireActivity is missing Navigation 3 effect handling: $required")
        }
    }

    @Test
    fun givenProductionWireActivity_whenInspectingSource_thenLegacyHostCannotReturn() {
        val source = wireActivitySource() + navigation3HostSource()

        listOf(
            "MainNavHost",
            "rememberNavigator",
            "NavController",
            "com.ramcosta.composedestinations",
            ".generated.",
            "ScreenDestination",
            "LegacyStartupDirectionMapper",
            "toLegacyDirection",
            "LegacyWireActivityRouteClassifier",
            "rememberWireActivityGraphContext(",
            "resolveWireActivityNavHostStartDestination",
        ).forEach { forbidden ->
            assertFalse(forbidden in source, "WireActivity still references legacy navigation: $forbidden")
        }
    }

    private fun wireActivitySource(): String {
        return uiSource("WireActivity.kt")
    }

    private fun navigation3HostSource(): String =
        uiSource("WireActivityNavigation3Host.kt")

    private fun uiSource(name: String): String {
        val relative = "src/main/kotlin/com/wire/android/ui/$name"
        return sequenceOf(
            File(relative),
            File("app/$relative"),
            File("../app/$relative"),
        ).first(File::isFile).readText()
    }
}
