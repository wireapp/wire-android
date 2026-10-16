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

import android.content.res.Configuration
import android.graphics.Rect
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.wire.android.BuildConfig
import com.wire.android.ui.WireTestTheme
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StartupAccountLimitDialogTest {
    @get:Rule
    val composeRule = createComposeRule()
    private val excessAccounts = mutableStateOf<Int?>(1)
    private val maxAccounts = mutableIntStateOf(BuildConfig.MAX_ACCOUNTS)
    private val logoutChoices = mutableListOf<Boolean>()

    private fun showDialog(locale: Locale = Locale.ENGLISH) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(context.resources.configuration).apply { setLocale(locale) }
        val localizedContext = context.createConfigurationContext(configuration)
        composeRule.setContent {
            CompositionLocalProvider(LocalContext provides localizedContext) {
                WireTestTheme {
                    StartupAccountLimitDialogFlow(
                        excessAccounts = excessAccounts.value,
                        onDismiss = { excessAccounts.value = null },
                        logout = logoutChoices::add,
                        maxAccounts = maxAccounts.intValue,
                    )
                }
            }
        }
    }

    @Test
    fun laterDismissesWithoutLogout() {
        showDialog()
        composeRule.onNodeWithText("Later").performClick()
        composeRule.onNodeWithText("Remove an account").assertIsNotDisplayed()
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
        composeRule.onNodeWithText("Remove an account").assertIsNotDisplayed()
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
        composeRule.waitUntil(timeoutMillis = 5_000) { excessAccounts.value == null }
        composeRule.onNodeWithText("Remove an account").assertIsNotDisplayed()
        composeRule.runOnIdle { assertTrue(logoutChoices.isEmpty()) }
    }

    @Test
    fun logoutOpensConfirmationAndCancelPreservesSession() {
        showDialog()
        composeRule.onNodeWithText("Log out of this account").performClick()
        composeRule.runOnIdle { assertTrue(logoutChoices.isEmpty()) }
        composeRule.onNodeWithContentDescription("Cancel logout").performClick()
        composeRule.onNodeWithText("Remove an account").assertIsNotDisplayed()
        composeRule.runOnIdle { assertTrue(logoutChoices.isEmpty()) }
    }

    @Test
    fun confirmationDefaultsToKeepingData() {
        showDialog()
        composeRule.onNodeWithText("Log out of this account").performClick()
        composeRule.onNodeWithText("Log out").performClick()
        composeRule.runOnIdle { assertEquals(listOf(false), logoutChoices) }
    }

    @Test
    fun confirmationCanClearData() {
        showDialog()
        composeRule.onNodeWithText("Log out of this account").performClick()
        composeRule.onNodeWithText("Delete all your personal information and conversations on this device").performClick()
        composeRule.onNodeWithText("Log out").performClick()
        composeRule.runOnIdle { assertEquals(listOf(true), logoutChoices) }
    }

    @Test
    fun approvedEnglishCopyTracksAllowedAndExcessAccounts() {
        showDialog()
        composeRule.onNodeWithText("Log out of this account").assertExists()
        composeRule.onNodeWithText("Later").assertExists()
        for ((allowed, allowedText) in listOf(1 to "one", 2 to "two", 3 to "3")) {
            for (excess in listOf(1, 2)) {
                composeRule.runOnIdle {
                    maxAccounts.intValue = allowed
                    excessAccounts.value = excess
                }
                val title = if (excess == 1) "Remove an account" else "Remove 2 accounts"
                val otherAccounts = if (excess == 1) "the other account" else "the other accounts"
                composeRule.onNodeWithText(title).assertExists()
                composeRule.onNodeWithText(
                    "You can only use $allowedText account${if (allowed == 1) "" else "s"} at once on this device. " +
                        "Log out of this account or select Later to log out of $otherAccounts.\n" +
                        "Create a backup to preserve your conversation history."
                ).assertExists()
            }
        }
    }

    @Test
    fun approvedGermanCopyTracksAllowedAndExcessAccounts() {
        showDialog(Locale.GERMAN)
        composeRule.onNodeWithText("Von diesem Konto abmelden").assertExists()
        composeRule.onNodeWithText("Später").assertExists()
        val introductions = listOf(
            1 to "Auf diesem Gerät können Sie jeweils nur ein Benutzerkonto verwenden.",
            2 to "Auf diesem Gerät können Sie jeweils nur zwei Benutzerkonten gleichzeitig verwenden.",
            3 to "Auf diesem Gerät können Sie jeweils nur 3 Benutzerkonten verwenden.",
        )
        for ((allowed, introduction) in introductions) {
            for (excess in listOf(1, 2)) {
                composeRule.runOnIdle {
                    maxAccounts.intValue = allowed
                    excessAccounts.value = excess
                }
                val title = if (excess == 1) "Konto entfernen" else "2 Konten entfernen"
                val otherAccounts = if (excess == 2) "von den anderen Konten" else "vom anderen Konto"
                composeRule.onNodeWithText(title).assertExists()
                composeRule.onNodeWithText(
                    "$introduction Melden Sie sich von diesem Konto ab oder wählen Sie Später, " +
                        "um sich $otherAccounts abzumelden.\n" +
                        "Erstellen Sie eine Datensicherung, um Ihren Gesprächsverlauf zu erhalten."
                ).assertExists()
            }
        }
    }
}
