/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.home.appLock

import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireSessionId
import java.io.File
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class AppLockNavigation3Test {

    @Test
    fun givenAppLockRoutes_whenReadingRouteIds_thenLegacyIdentitiesArePreserved() {
        assertEquals("app/set_lock_code_screen", SetLockCodeRoute.ROUTE_ID)
        assertEquals("app/forgot_lock_code_screen", ForgotLockCodeRoute.ROUTE_ID)
        assertEquals("app/enter_lock_code_screen", EnterLockCodeRoute.ROUTE_ID)
        assertEquals(
            "app/app_unlock_with_biometrics_screen",
            AppUnlockWithBiometricsRoute.ROUTE_ID,
        )
    }

    @Test
    fun givenBiometricRoute_whenSerializedAndRestored_thenSessionAndEntryIdentityArePreserved() {
        val route = AppUnlockWithBiometricsRoute(
            sessionId = WireSessionId("user", "wire.example"),
            entryId = WireNavEntryId("biometric-entry"),
        )

        assertEquals(
            route,
            Json.decodeFromString<AppUnlockWithBiometricsRoute>(Json.encodeToString(route)),
        )
    }

    @Test
    fun givenTeamAppLockSetup_whenResolvingStartRoute_thenSetLockWinsOverBiometrics() {
        val sessionId = WireSessionId("user", "wire.example")

        val route = resolveAppLockStartRoute(
            sessionId = sessionId,
            setTeamAppLock = true,
            canAuthenticateWithBiometrics = true,
        )

        assertInstanceOf(SetLockCodeRoute::class.java, route)
        assertEquals(sessionId, route.sessionId)
    }

    @Test
    fun givenBiometricsAvailable_whenResolvingUnlockStartRoute_thenBiometricRouteIsUsed() {
        val sessionId = WireSessionId("user", "wire.example")

        val route = resolveAppLockStartRoute(
            sessionId = sessionId,
            setTeamAppLock = false,
            canAuthenticateWithBiometrics = true,
        )

        assertInstanceOf(AppUnlockWithBiometricsRoute::class.java, route)
        assertEquals(sessionId, route.sessionId)
    }

    @Test
    fun givenBiometricsUnavailable_whenResolvingUnlockStartRoute_thenPasscodeRouteIsUsed() {
        val sessionId = WireSessionId("user", "wire.example")

        val route = resolveAppLockStartRoute(
            sessionId = sessionId,
            setTeamAppLock = false,
            canAuthenticateWithBiometrics = false,
        )

        assertInstanceOf(EnterLockCodeRoute::class.java, route)
        assertEquals(sessionId, route.sessionId)
    }

    @Test
    fun givenAppLockEntries_whenInspectingSource_thenFourTypedEntriesHaveNoNavigation2Bridge() {
        val source = sourceFile("AppLockNavigation3Entries.kt").readText()

        assertEquals(4, Regex("""wireEntry<""").findAll(source).count())
        assertEquals(true, "viewModel.onAppUnlocked()" in source)
        listOf(
            "com.ramcosta",
            "NavController",
            "Bundle",
            "SavedStateHandle",
            "DEFAULT_ARGS_KEY",
            "com.wire.android.navigation.Navigator",
            "com.wire.android.navigation.NavigationCommand",
        ).forEach { forbidden -> assertFalse(forbidden in source, forbidden) }
    }

    @Test
    fun givenAppLockActivity_whenInspectingSource_thenItUsesOnlyNavigation3HostAndMetroEnvironment() {
        val source = activitySourceFile().readText()

        listOf(
            "rememberWireNavigation3Runtime",
            "WireNav3Host",
            "MetroWireEntryEnvironment",
            "SessionGraphStoreViewModel",
            "finishAffinity()",
            "Intent.FLAG_ACTIVITY_CLEAR_TASK",
        ).forEach { expected -> assertEquals(true, expected in source, expected) }
        listOf(
            "com.ramcosta",
            "MainNavHost",
            "rememberNavigator",
            "NavController",
            "LoginTypeSelector",
            "generated.app.destinations",
        ).forEach { forbidden -> assertFalse(forbidden in source, forbidden) }
    }

    private fun sourceFile(name: String): File {
        val relative = "src/main/kotlin/com/wire/android/ui/home/appLock/$name"
        return sequenceOf(
            File(relative),
            File("app/$relative"),
            File("../app/$relative"),
        ).first(File::isFile)
    }

    private fun activitySourceFile(): File {
        val relative = "src/main/kotlin/com/wire/android/ui/AppLockActivity.kt"
        return sequenceOf(
            File(relative),
            File("app/$relative"),
            File("../app/$relative"),
        ).first(File::isFile)
    }
}
