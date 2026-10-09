/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
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
        maxAccounts.intValue = 1
        showDialog()
        composeRule.onNodeWithText("Remove an account").assertExists()
        composeRule.onNodeWithText(
            "You can only use one account at once on this device. " +
                "Log out of this account or select Later to log out of the other account."
        ).assertExists()
        composeRule.onNodeWithText("Log out of this account").assertExists()

        composeRule.runOnIdle { excessAccounts.value = 2 }
        composeRule.onNodeWithText("Remove 2 accounts").assertExists()
        composeRule.onNodeWithText(
            "You can only use one account at once on this device. " +
                "Log out of this account or select Later to log out of the other accounts."
        ).assertExists()

        composeRule.runOnIdle {
            maxAccounts.intValue = 2
            excessAccounts.value = 1
        }
        composeRule.onNodeWithText("Remove an account").assertExists()
        composeRule.onNodeWithText(
            "You can only use two accounts at once on this device. " +
                "Log out of this account or select Later to log out of the other account."
        ).assertExists()

        composeRule.runOnIdle { maxAccounts.intValue = 3 }
        composeRule.onNodeWithText(
            "You can only use 3 accounts at once on this device. " +
                "Log out of this account or select Later to log out of the other account."
        ).assertExists()
    }

    @Test
    fun approvedGermanCopyTracksAllowedAndExcessAccounts() {
        maxAccounts.intValue = 1
        showDialog(Locale.GERMAN)
        composeRule.onNodeWithText("Ein Konto entfernen").assertExists()
        composeRule.onNodeWithText(
            "Auf diesem Gerät können Sie jeweils nur ein Benutzerkonto verwenden. " +
                "Melden Sie sich von diesem Konto ab oder wählen Sie „Später“, um sich von dem anderen Konto abzumelden."
        ).assertExists()
        composeRule.onNodeWithText("Von diesem Konto abmelden").assertExists()
        composeRule.onNodeWithText("Später").assertExists()

        composeRule.runOnIdle { excessAccounts.value = 2 }
        composeRule.onNodeWithText("2 Konten entfernen").assertExists()
        composeRule.onNodeWithText(
            "Auf diesem Gerät können Sie jeweils nur ein Benutzerkonto verwenden. " +
                "Melden Sie sich von diesem Konto ab oder wählen Sie „Später“, um sich von den anderen Konten abzumelden."
        ).assertExists()

        composeRule.runOnIdle {
            maxAccounts.intValue = 2
            excessAccounts.value = 1
        }
        composeRule.onNodeWithText("Ein Konto entfernen").assertExists()
        composeRule.onNodeWithText(
            "Auf diesem Gerät können Sie nur zwei Benutzerkonten gleichzeitig verwenden. " +
                "Melden Sie sich von diesem Konto ab oder wählen Sie „Später“, um sich vom anderen Konto abzumelden."
        ).assertExists()

        composeRule.runOnIdle { maxAccounts.intValue = 3 }
        composeRule.onNodeWithText(
            "Auf diesem Gerät können Sie nur 3 Benutzerkonten gleichzeitig verwenden. " +
                "Melden Sie sich von diesem Konto ab oder wählen Sie „Später“, um sich vom anderen Konto abzumelden."
        ).assertExists()
    }
}
