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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WireNavigation3ProductionHostSourceTest {

    @Test
    fun givenProductionHost_whenInspectingSource_thenItHasOneTypedCatalogAndNoLegacyHost() {
        val source = sourceFile().readText()

        assertTrue("WireNavigation3Contributions.create(runtime, actions, authenticationRouter)" in source)
        assertTrue("WireNav3Host(" in source)
        assertTrue("onRootBack: () -> Unit" in source)
        assertTrue("navigateBackOrRunRootFallback(runtime.navigator::goBack, onRootBack)" in source)
        listOf(
            "MainNavHost",
            "DestinationsNavHost",
            "NavController",
            "com.ramcosta",
            "NavigationCommand",
            "Bundle",
        ).forEach { forbidden -> assertFalse(forbidden in source, forbidden) }
    }

    @Test
    fun givenBackStackCanPop_whenHandlingSystemBack_thenRootFallbackIsNotCalled() {
        var rootBackCalls = 0

        navigateBackOrRunRootFallback(
            navigateBack = { true },
            onRootBack = { rootBackCalls++ },
        )

        assertEquals(0, rootBackCalls)
    }

    @Test
    fun givenBackStackIsAtRoot_whenHandlingSystemBack_thenRootFallbackIsCalled() {
        var rootBackCalls = 0

        navigateBackOrRunRootFallback(
            navigateBack = { false },
            onRootBack = { rootBackCalls++ },
        )

        assertEquals(1, rootBackCalls)
    }

    private fun sourceFile(): File {
        val relative = "src/main/kotlin/com/wire/android/navigation/runtime/WireNavigation3ProductionHost.kt"
        return sequenceOf(
            File(relative),
            File("app/$relative"),
            File("../app/$relative"),
        ).first(File::isFile)
    }
}
