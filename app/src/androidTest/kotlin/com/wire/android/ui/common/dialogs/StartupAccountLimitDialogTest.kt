/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.common.dialogs

import android.graphics.Rect
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.wire.android.ui.WireTestTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StartupAccountLimitDialogTest {
    @get:Rule
    val composeRule = createComposeRule()
    private val visible = mutableStateOf(true)
    private val logoutChoices = mutableListOf<Boolean>()

    private fun showDialog() {
        composeRule.setContent {
            WireTestTheme {
                StartupAccountLimitDialogFlow(
                    visible = visible.value,
                    onDismiss = { visible.value = false },
                    logout = logoutChoices::add,
                )
            }
        }
    }

    @Test
    fun laterDismissesWithoutLogout() {
        showDialog()
        composeRule.onNodeWithText("Later").performClick()
        composeRule.onNodeWithText("Account limit reached").assertIsNotDisplayed()
        composeRule.runOnIdle { assertTrue(logoutChoices.isEmpty()) }
    }

    private fun waitForNativeWindow() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString() == instrumentation.targetContext.packageName
        }
    }

    @Test
    fun backDismissesWithoutLogout() {
        showDialog()
        waitForNativeWindow()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Account limit reached").assertIsNotDisplayed()
        composeRule.runOnIdle { assertTrue(logoutChoices.isEmpty()) }
    }

    @Test
    fun outsideTapDismissesWithoutLogout() {
        showDialog()
        waitForNativeWindow()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val dialogBounds = Rect().also { bounds ->
            instrumentation.uiAutomation.rootInActiveWindow.getBoundsInScreen(bounds)
        }
        val screenHeight = instrumentation.targetContext.resources.displayMetrics.heightPixels
        val x = dialogBounds.centerX().toFloat()
        val y = if (dialogBounds.top > screenHeight - dialogBounds.bottom) {
            dialogBounds.top - 1f
        } else {
            dialogBounds.bottom + 1f
        }
        val time = SystemClock.uptimeMillis()
        for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
            val event = MotionEvent.obtain(time, SystemClock.uptimeMillis(), action, x, y, 0)
            event.source = InputDevice.SOURCE_TOUCHSCREEN
            instrumentation.uiAutomation.injectInputEvent(event, true)
            event.recycle()
        }
        composeRule.waitUntil(timeoutMillis = 5_000) { !visible.value }
        composeRule.onNodeWithText("Account limit reached").assertIsNotDisplayed()
        composeRule.runOnIdle { assertTrue(logoutChoices.isEmpty()) }
    }

    @Test
    fun logoutOpensConfirmationAndCancelPreservesSession() {
        showDialog()
        composeRule.onNodeWithText("Log out of current account").performClick()
        composeRule.runOnIdle { assertTrue(logoutChoices.isEmpty()) }
        composeRule.onNodeWithContentDescription("Cancel logout").performClick()
        composeRule.onNodeWithText("Account limit reached").assertIsNotDisplayed()
        composeRule.runOnIdle { assertTrue(logoutChoices.isEmpty()) }
    }

    @Test
    fun confirmationDefaultsToKeepingData() {
        showDialog()
        composeRule.onNodeWithText("Log out of current account").performClick()
        composeRule.onNodeWithText("Log out").performClick()
        composeRule.runOnIdle { assertEquals(listOf(false), logoutChoices) }
    }

    @Test
    fun confirmationCanClearData() {
        showDialog()
        composeRule.onNodeWithText("Log out of current account").performClick()
        composeRule.onNodeWithText("Delete all your personal information and conversations on this device").performClick()
        composeRule.onNodeWithText("Log out").performClick()
        composeRule.runOnIdle { assertEquals(listOf(true), logoutChoices) }
    }
}
