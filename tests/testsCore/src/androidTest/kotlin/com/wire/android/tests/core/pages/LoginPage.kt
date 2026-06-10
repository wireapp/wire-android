/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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

import android.content.Intent
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import backendUtils.BackendClient
import org.junit.Assert.assertTrue
import java.io.ByteArrayOutputStream
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class LoginPage(private val device: UiDevice) {
    val backendClient = BackendClient.loadBackend("STAGING")

    // Locators
    private val emailInputField = UiSelector().resourceId("userIdentifierInput")
    private val passwordInputFieldSelector = UiSelectorParams(resourceId = "PasswordInput")
    private val loginButtonSelector = UiSelectorParams(resourceId = "loginButton")
    private val newLoginPasswordInputFieldSelector = UiSelectorParams(resourceId = "passwordField")
    private val newLoginButtonSelector = UiSelectorParams(resourceId = "LoginNextButton")
    private val proceedButtonSelector = UiSelectorParams(text = "Proceed")
    private val confirmButtonSelector = UiSelectorParams(text = "Confirm")
    private val removeDeviceLabelSelector = UiSelectorParams(text = "YOUR DEVICES")
    private val removeDeviceButtonSelector = UiSelectorParams(description = "Remove device")
    private val removeDeviceDialogSelector = UiSelectorParams(text = "Remove the following device?")
    private val removeDevicePasswordSelector = UiSelectorParams(className = "android.widget.EditText")
    private val removeButtonSelector = UiSelectorParams(text = "Remove")

    fun enterPersonalUserLoggingEmail(email: String): LoginPage {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        // Click the input field (waits until visible)
        device.findObject(emailInputField).click()
        // Wait again to avoid stale object
        device.findObject(emailInputField)
        // Set text via UiObject (more reliable than UiObject2.text=)
        device.findObject(emailInputField).setText(email)

        return this
    }

    fun enterUserIdentifier(email: String): LoginPage {
        val input = UiWaitUtils.findElementOrNull(UiSelectorParams(resourceId = "userIdentifierInput"))
            ?: UiWaitUtils.waitAnyVisible(
                selectors = listOf(
                    UiSelectorParams(resourceId = "userIdentifierInput"),
                    UiSelectorParams(resourceId = "emailField")
                )
            )
            ?: throw AssertionError("Login email/user identifier input was not visible.")
        val bounds = input.visibleBounds
        device.click(bounds.centerX(), bounds.centerY())
        device.waitForIdle()
        val userIdentifierInput = runCatching { device.findObject(emailInputField) }.getOrNull()
        if (userIdentifierInput != null) {
            userIdentifierInput.setText("")
            userIdentifierInput.setText(email)
        } else {
            input.text = email
        }
        return this
    }

    fun enterPersonalUserLoginPassword(password: String): LoginPage {
        val passwordInputField = UiWaitUtils.waitElement(passwordInputFieldSelector)
        passwordInputField.click()
        passwordInputField.text = password
        return this
    }

    fun enterUserPassword(password: String, timeout: Duration = 30.seconds): LoginPage {
        val input = UiWaitUtils.waitAnyVisible(
            selectors = listOf(passwordInputFieldSelector, newLoginPasswordInputFieldSelector),
            timeout = timeout,
            pollingInterval = UiWaitUtils.POLLING_FAST
        ) ?: throw AssertionError("Login password input was not visible.")
        input.click()
        input.text = password
        return this
    }

    fun enterTeamMemberLoggingPassword(password: String): LoginPage {
        val passwordInputField = UiWaitUtils.waitElement(passwordInputFieldSelector)
        passwordInputField.click()
        passwordInputField.text = password
        return this
    }

    fun enterTeamOwnerLoggingEmail(email: String): LoginPage {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        // Click the input field (waits until visible)
        device.findObject(emailInputField).click()
        // Wait again to avoid stale object
        device.findObject(emailInputField)
        // Set text via UiObject (more reliable than UiObject2.text=)
        device.findObject(emailInputField).setText(email)

        return this
    }

    fun enterTeamMemberLoggingEmail(email: String): LoginPage {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        // Click the input field (waits until visible)
        device.findObject(emailInputField).click()
        // Wait again to avoid stale object
        device.findObject(emailInputField)
        // Set text via UiObject (more reliable than UiObject2.text=)
        device.findObject(emailInputField).setText(email)

        return this
    }

    fun enterSSOCodeOnSSOLoginTab(email: String): LoginPage {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        // Click the input field (waits until visible)
        device.findObject(emailInputField).click()
        // Wait again to avoid stale object
        device.findObject(emailInputField)
        // Set text via UiObject (more reliable than UiObject2.text=)
        device.findObject(emailInputField).setText(email)

        return this
    }

    fun enterTeamOwnerLoggingPassword(password: String): LoginPage {
        val passwordInputField = UiWaitUtils.waitElement(passwordInputFieldSelector)
        passwordInputField.click()
        passwordInputField.text = password
        return this
    }

    fun clickStagingDeepLink(): LoginPage {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val deepLinkUrl = "wire://access/?config=${backendClient.deeplink}"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(deepLinkUrl)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
        return this
    }

    fun assertLoggingPageVisible(): LoginPage {
        val loginPage = UiWaitUtils.waitElement(loginButtonSelector)
        assertTrue("Login page is not visible", !loginPage.visibleBounds.isEmpty)
        return this
    }

    fun assertIdentifierInputVisible(timeout: Duration = 30.seconds): LoginPage {
        UiWaitUtils.waitAnyVisible(
            selectors = listOf(
                UiSelectorParams(resourceId = "userIdentifierInput"),
                UiSelectorParams(resourceId = "emailField")
            ),
            timeout = timeout
        ) ?: throw AssertionError("Login email/user identifier input was not visible.")
        return this
    }

    fun clickLoginButton(): LoginPage {
        val clicked = UiWaitUtils.retryUntilTimeout(
            timeout = 30.seconds,
            pollingInterval = UiWaitUtils.POLLING_FAST
        ) {
            listOf(loginButtonSelector, newLoginButtonSelector).any { selector ->
                UiWaitUtils.clickWhenClickable(
                    params = selector,
                    timeout = UiWaitUtils.POLLING_FAST,
                    pollingInterval = UiWaitUtils.POLLING_FAST
                )
            }
        }
        if (!clicked) {
            throw AssertionError("Login button not found or not clickable.\n${loginScreenDiagnostics()}")
        }
        return this
    }

    fun assertRemoveDeviceScreenVisible(timeout: Duration = 30.seconds): LoginPage {
        UiWaitUtils.waitUntilVisibleOrThrow(
            params = UiSelectorParams(text = "Remove a Device"),
            timeout = timeout,
            errorMessage = "Remove a Device screen was not visible."
        )
        return this
    }

    fun assertRemoveDeviceListVisible(timeout: Duration = 30.seconds): LoginPage {
        UiWaitUtils.waitUntilVisibleOrThrow(
            params = removeDeviceLabelSelector,
            timeout = timeout,
            errorMessage = "Remove Device list was not visible."
        )
        return this
    }

    fun clickFirstRemoveDeviceButton(): LoginPage {
        val clicked = UiWaitUtils.clickWhenClickable(
            params = removeDeviceButtonSelector,
            timeout = 30.seconds,
            pollingInterval = UiWaitUtils.POLLING_FAST
        )
        assertTrue("Remove device button was not clickable.", clicked)
        return this
    }

    fun assertRemoveDeviceDialogVisible(timeout: Duration = 30.seconds): LoginPage {
        UiWaitUtils.waitUntilVisibleOrThrow(
            params = removeDeviceDialogSelector,
            timeout = timeout,
            errorMessage = "Remove Device confirmation dialog was not visible."
        )
        return this
    }

    fun enterRemoveDevicePassword(password: String): LoginPage {
        val input = UiWaitUtils.waitElement(removeDevicePasswordSelector)
        input.click()
        input.text = password
        return this
    }

    fun confirmRemoveDevice(): LoginPage {
        val clicked = UiWaitUtils.clickWhenClickable(
            params = removeButtonSelector,
            timeout = 30.seconds,
            pollingInterval = UiWaitUtils.POLLING_FAST
        )
        assertTrue("Remove Device confirmation button was not clickable.", clicked)
        return this
    }

    fun clickProceedButtonOnDeeplinkOverlay(): LoginPage {
        val proceedButton = UiWaitUtils.waitElement(proceedButtonSelector)
        proceedButton.click()
        return this
    }

    fun clickConfirmButtonOnUsernameSetupPage(): LoginPage {
        val confirmButton = UiWaitUtils.waitElement(confirmButtonSelector)
        confirmButton.click()
        return this
    }

    private fun loginScreenDiagnostics(): String {
        val output = ByteArrayOutputStream()
        device.dumpWindowHierarchy(output)
        val hierarchy = output.toString()
        val nodes = Regex("<node [^>]+>").findAll(hierarchy)
            .map { it.value }
            .filter {
                it.contains("login", ignoreCase = true) ||
                        it.contains("email", ignoreCase = true) ||
                        it.contains("identifier", ignoreCase = true) ||
                        it.contains("password", ignoreCase = true) ||
                        it.contains("button", ignoreCase = true) ||
                        it.contains("continue", ignoreCase = true) ||
                        it.contains("next", ignoreCase = true)
            }
            .take(30)
            .joinToString(separator = "\n")

        return if (nodes.isBlank()) {
            "No login-related nodes found in current window hierarchy."
        } else {
            "Current login-related nodes:\n$nodes"
        }
    }
}
