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

internal class WireNavigation3ProductionActionsSourceTest {

    @Test
    fun `production actions keep Activity and generated navigation behind semantic callbacks`() {
        val source = sourceFile().readText()

        assertFalse(source.contains("com.ramcosta.composedestinations.generated"))
        assertFalse(source.contains("NavController"))
        assertFalse(source.contains("android.content.Intent"))
        assertFalse(source.contains("WireActivity"))
        assertFalse(source.contains("PendingFolderResult"))
        assertFalse(source.contains("PendingDrawingResult"))
        assertFalse(source.contains("pendingFolderResults"))
        assertFalse(source.contains("pendingDrawingResults"))
        assertFalse(source.contains("callback:"))
        assertTrue(source.contains("override fun restartAfterLogout() = activity.restartAfterLogout()"))
        assertFalse(source.contains("override fun restartAfterLogout() = activity.hardLogout()"))
    }

    @Test
    fun `initial sync waits for typed Home before moving task to background`() {
        val source = sourceFile().readText()

        assertTrue(source.contains("runtime.navigator.currentRoute as? HomeRoute"))
        assertTrue(source.contains("activity.moveTaskToBackground()"))
    }

    private fun sourceFile() = File(
        "src/main/kotlin/com/wire/android/navigation/runtime/WireNavigation3ProductionActions.kt"
    )
}
