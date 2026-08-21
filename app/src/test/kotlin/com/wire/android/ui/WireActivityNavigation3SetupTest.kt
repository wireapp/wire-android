/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui

import com.wire.android.feature.SwitchAccountActions
import com.wire.android.util.SwitchAccountObserver
import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class WireActivityNavigation3SetupTest {

    @Test
    fun givenRegisteredSwitchActions_whenObserverEmits_thenActionsStopReceivingEventsAfterUnregister() {
        val observer = SwitchAccountObserver()
        val received = mutableListOf<String>()
        val actions = object : SwitchAccountActions {
            override fun switchedToAnotherAccount() {
                received += "switched"
            }

            override fun noOtherAccountToSwitch() {
                received += "none"
            }
        }
        val unregister = registerWireActivityNavigation3SwitchAccountActions(observer, actions)

        observer.switchedToAnotherAccount()
        observer.noOtherAccountToSwitch()
        unregister()
        observer.switchedToAnotherAccount()
        observer.noOtherAccountToSwitch()

        assertEquals(listOf("switched", "none"), received)
    }

    @Test
    fun givenNavigation3SetupSource_whenInspectingEffects_thenLifecycleAndTypedBoundariesAreExplicit() {
        val source = navigation3SetupSource().readText()

        assertTrue(source.contains("ObserveNavigation3Routes("))
        assertTrue(source.contains("currentKeyboardController?.hide()"))
        assertTrue(source.contains("repeatOnLifecycle(Lifecycle.State.STARTED)"))
        assertTrue(source.contains("currentIntentHandler(request)"))
        assertTrue(source.contains("repeatOnLifecycle(Lifecycle.State.RESUMED)"))
        assertTrue(source.contains("registerWireActivityNavigation3SwitchAccountActions("))
        assertTrue(source.contains("onDispose(unregister)"))
    }

    @Test
    fun givenNavigation3SetupSource_whenInspectingImports_thenNoLegacyNavigationApiIsReferenced() {
        val source = navigation3SetupSource().readText()

        listOf(
            "com.ramcosta.composedestinations",
            "androidx.navigation.NavController",
            "com.ramcosta.composedestinations.generated",
            "Navigator",
        ).forEach { forbidden ->
            assertFalse(source.contains(forbidden), "Navigation 3 setup must not reference $forbidden")
        }
    }

    private fun navigation3SetupSource(): File {
        val projectDir = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "app/src/main/kotlin").isDirectory }
        return File(
            projectDir,
            "app/src/main/kotlin/com/wire/android/ui/WireActivityNavigation3Setup.kt",
        ).also {
            assertTrue(it.isFile, "Missing Navigation 3 setup source")
        }
    }
}
