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
package com.wire.android.tests.core.pages

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import org.junit.Assert.assertTrue
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import kotlin.time.Duration.Companion.seconds

data class AppLockPage(private val device: UiDevice) {
    private val appLockPageTitle = UiSelectorParams(text = "Enter passcode to unlock Wire")
    private val passcodeField = UiSelectorParams(resourceId = "password")
    private val unlockButton = UiSelectorParams(text = "Unlock")
    private val wrongPasscodeErrorMessage = UiSelectorParams(text = "Check your passcode and try again")
    private val biometricPromptTitle = UiSelectorParams(text = "Authenticate with biometrics")
    private val biometricPromptSubtitle = UiSelectorParams(text = "To unlock Wire")
    private val usePasscodeButton = UiSelectorParams(text = "Use passcode")
    private val editTextClass = By.clazz("android.widget.EditText")
    private val appLockGateSelectors = listOf(appLockPageTitle, biometricPromptTitle, biometricPromptSubtitle)

    fun assertAppLockGateVisible(): AppLockPage {
        val gate = UiWaitUtils.waitAnyVisible(
            selectors = appLockGateSelectors,
            timeout = UiWaitUtils.VERY_LONG_TIMEOUT
        ) ?: throw AssertionError("Neither the biometric prompt nor the app lock passcode page is visible")
        assertTrue("App lock gate is not visible", !gate.visibleBounds.isEmpty)
        return this
    }

    /**
     * Asserts that the app lock gate is the first screen to become visible, failing when
     * [otherScreen] appears before it. Polling-based, so a flash shorter than the polling
     * interval can still go unnoticed.
     */
    fun assertAppLockGateVisibleBefore(otherScreen: UiSelectorParams, otherScreenName: String): AppLockPage {
        val winner = UiWaitUtils.waitFirstVisibleSelector(
            selectors = appLockGateSelectors + otherScreen,
            timeout = UiWaitUtils.VERY_LONG_TIMEOUT
        ) ?: throw AssertionError("Neither the app lock gate nor $otherScreenName became visible")
        if (winner == otherScreen) {
            throw AssertionError("$otherScreenName became visible before the app lock gate")
        }
        return this
    }

    fun assertAppLockPageVisible(): AppLockPage {
        val title = UiWaitUtils.waitElement(appLockPageTitle)
        assertTrue("App lock page is not visible", !title.visibleBounds.isEmpty)
        return this
    }

    fun tapUsePasscodeOnBiometricPromptIfVisible(): AppLockPage {
        if (UiWaitUtils.clickWhenClickable(usePasscodeButton, timeout = 1.seconds)) {
            device.waitForIdle()
        }
        return this
    }

    fun enterPasscode(passcode: String): AppLockPage {
        val parent = UiWaitUtils.waitElement(passcodeField)
        val codeInputField = parent.findObject(editTextClass)
        codeInputField.click()
        codeInputField.text = passcode
        device.waitForIdle()
        return this
    }

    fun clearPasscodeField(): AppLockPage {
        val parent = UiWaitUtils.waitElement(passcodeField)
        val codeInputField = parent.findObject(editTextClass)
        codeInputField.click()
        codeInputField.text = ""
        device.waitForIdle()
        return this
    }

    fun tapUnlockButtonOnAppLockPage(): AppLockPage {
        UiWaitUtils.waitElement(unlockButton).click()
        device.waitForIdle()
        return this
    }

    fun assertWrongPasscodeErrorMessageVisible(): AppLockPage {
        val errorMessage = UiWaitUtils.waitElement(wrongPasscodeErrorMessage)
        assertTrue("Wrong passcode error message is not visible", !errorMessage.visibleBounds.isEmpty)
        return this
    }
}
