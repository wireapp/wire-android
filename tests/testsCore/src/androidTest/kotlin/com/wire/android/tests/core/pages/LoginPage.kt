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
import androidx.test.uiautomator.By
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import com.wire.android.tests.support.UiAutomatorSetup
import backendUtils.BackendClient
import org.junit.Assert.assertTrue
import java.io.ByteArrayOutputStream
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class LoginPage(private val device: UiDevice) {
    // Locators
    private val emailInputField = UiSelector().resourceId("userIdentifierInput")
    private val emailInputFieldSelector = UiSelectorParams(resourceId = "userIdentifierInput")
    private val passwordInputFieldSelector = UiSelectorParams(resourceId = "PasswordInput")
    private val loginButtonSelector = UiSelectorParams(resourceId = "loginButton")
    private val newLoginPasswordInputFieldSelector = UiSelectorParams(resourceId = "passwordField")
    private val newLoginButtonSelector = UiSelectorParams(resourceId = "LoginNextButton")
    private val forgotPasswordLinkSelector = UiSelectorParams(text = "Forgot password?")
    private val showPasswordButtonSelector = UiSelectorParams(description = "Show password")
    private val hidePasswordButtonSelector = UiSelectorParams(description = "Hide password")
    private val ssoLoginTabSelector = UiSelectorParams(text = "SSO LOGIN")
    private val oldLoginSsoCodeLabelSelector = UiSelectorParams(textContains = "SSO CODE")
    private val oldLoginSsoButtonSelector = UiSelectorParams(text = "Login")
    private val invalidInformationAlertSelector = UiSelectorParams(text = "Invalid information")
    private val incorrectCredentialsAlertOkButtonSelector = UiSelectorParams(text = "OK")
    private val proceedButtonSelector = UiSelectorParams(text = "Proceed")
    private val proceedButtonGoneSelector = UiSelector().text("Proceed")
    private val backendConfigSuccessContinueButtonSelector =
        UiSelectorParams(resourceId = "backendConfigSuccessContinueButton")
    private val confirmButtonSelector = UiSelectorParams(text = "Confirm")
    private val androidResolverWireDevSelector = UiSelectorParams(text = "Wire Dev")
    private val androidResolverJustOnceSelector = UiSelectorParams(text = "Just once")
    private val emailWelcomeSelector = UiSelectorParams(textMatches = "Enter your (email to start!|credentials to log in)")
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

    fun enterPersonalUserLoggingUsername(username: String): LoginPage {
        return enterPersonalUserLoggingEmail(username)
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
        enterPassword(password)
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
        enterPassword(password)
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

    fun clearUserIdentifierInput(): LoginPage {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.findObject(emailInputField).click()
        device.findObject(emailInputField).setText("")
        return this
    }

    fun clearLoginPasswordInput(): LoginPage {
        val passwordContainer = waitForLoginPasswordInput(UiWaitUtils.SHORT_WAIT)
        val passwordInput = passwordContainer.findObject(By.clazz("android.widget.EditText")) ?: passwordContainer
        passwordInput.click()
        passwordInput.text = ""
        return this
    }

    fun enterTeamOwnerLoggingPassword(password: String): LoginPage {
        enterPassword(password)
        return this
    }

    fun clickStagingDeepLink(backendName: String = DEFAULT_BACKEND_NAME): LoginPage {
        val backendClient = BackendClient.loadBackend(backendName)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val deepLinkUrl = "wire://access/?config=${backendClient.deeplink}"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(deepLinkUrl)
            setPackage(UiAutomatorSetup.appPackage)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
        return this
    }

    fun clickStagingDeepLinkForOldLoginFlow(backendName: String = DEFAULT_BACKEND_NAME): LoginPage {
        val backendClient = BackendClient.loadBackend(backendName)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val deepLinkUrl = "wire://access/?config=${backendClient.deeplink}&login-type=old"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(deepLinkUrl)
            setPackage(UiAutomatorSetup.appPackage)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
        return this
    }

    fun assertOldLoginWelcomePageVisible(): LoginPage {
        val loginButton = UiWaitUtils.waitElement(
            loginButtonSelector,
            timeout = UiWaitUtils.SHORT_WAIT
        )
        assertTrue("Old login Welcome Page is not visible", !loginButton.visibleBounds.isEmpty)
        return this
    }

    fun assertLoggingPageVisible(): LoginPage {
        val loginPage = UiWaitUtils.waitElement(loginButtonSelector)
        assertTrue("Login page is not visible", !loginPage.visibleBounds.isEmpty)
        return this
    }

    fun assertUserLoginScreenVisible(timeout: Duration = 5.seconds): LoginPage {
        val passwordInput = waitForLoginPasswordInput(timeout)
        assertTrue("Login password input is not visible", !passwordInput.visibleBounds.isEmpty)
        return this
    }

    fun assertLoginPasswordVisible(expectedPassword: String, timeout: Duration = 5.seconds): LoginPage {
        UiWaitUtils.waitElement(hidePasswordButtonSelector, timeout = timeout)
        val actualPassword = waitForLoginPasswordInput(timeout).text.orEmpty()
        assertTrue(
            "Login password is not visible in cleartext",
            actualPassword.contains(expectedPassword)
        )
        return this
    }

    fun assertLoginPasswordHidden(timeout: Duration = 5.seconds): LoginPage {
        val showPasswordButton = UiWaitUtils.waitElement(showPasswordButtonSelector, timeout = timeout)
        assertTrue(
            "Login password visibility toggle did not return to hidden state",
            !showPasswordButton.visibleBounds.isEmpty
        )
        return this
    }

    fun tapForgotPasswordLink(): LoginPage {
        UiWaitUtils.waitElement(
            forgotPasswordLinkSelector,
            timeout = UiWaitUtils.SHORT_WAIT
        ).click()
        return this
    }

    fun assertInvalidUserIdentifierErrorVisible(expectedMessage: String): LoginPage {
        val error = UiWaitUtils.waitElement(
            UiSelectorParams(text = expectedMessage),
            timeout = UiWaitUtils.SHORT_WAIT
        )
        assertTrue("Invalid user identifier error is not visible", !error.visibleBounds.isEmpty)
        return this
    }

    fun assertInvalidInformationAlertVisible(): LoginPage {
        val alert = UiWaitUtils.waitElement(
            invalidInformationAlertSelector,
            timeout = UiWaitUtils.SHORT_WAIT
        )
        assertTrue("Invalid information alert is not visible", !alert.visibleBounds.isEmpty)
        return this
    }

    fun assertIncorrectCredentialsErrorVisible(expectedMessage: String): LoginPage {
        val error = UiWaitUtils.waitElement(
            UiSelectorParams(text = expectedMessage),
            timeout = UiWaitUtils.SHORT_WAIT
        )
        assertTrue("Incorrect credentials error is not visible", !error.visibleBounds.isEmpty)
        return this
    }

    fun clickOkButtonOnIncorrectCredentialsAlertIfVisible(): LoginPage {
        UiWaitUtils.clickWhenClickable(
            incorrectCredentialsAlertOkButtonSelector,
            timeout = UiWaitUtils.SHORT_WAIT
        )
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

    fun clickSsoLoginTab(): LoginPage {
        val clicked = UiWaitUtils.clickWhenClickable(
            ssoLoginTabSelector,
            timeout = UiWaitUtils.SHORT_WAIT
        )
        assertTrue("SSO login tab was not clickable", clicked)
        return this
    }

    fun enterSSOCodeOnOldLoginInputField(ssoCode: String): LoginPage {
        val ssoCodeLabel = UiWaitUtils.waitElement(
            oldLoginSsoCodeLabelSelector,
            timeout = UiWaitUtils.SHORT_WAIT
        )
        val input = ssoCodeLabel.parent.findObject(By.clazz("android.widget.EditText"))
            ?: throw AssertionError("SSO code input on old login flow was not visible.")
        input.click()
        input.text = ssoCode
        return this
    }

    fun clickOldSsoLoginButton(): LoginPage {
        val clicked = UiWaitUtils.clickWhenClickable(
            oldLoginSsoButtonSelector,
            timeout = UiWaitUtils.SHORT_WAIT
        )
        assertTrue("Login button on old SSO login tab was not clickable", clicked)
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
        val proceeded = UiWaitUtils.retryUntilTimeout(timeout = UiWaitUtils.LONG_TIMEOUT) {
            UiWaitUtils.findElementOrNull(androidResolverWireDevSelector)?.let { wireDevOption ->
                runCatching { wireDevOption.click() }
                val resolverHandled = UiWaitUtils.clickWhenClickable(androidResolverJustOnceSelector, timeout = 5.seconds)
                if (!resolverHandled) {
                    throw AssertionError("Android app resolver was visible, but 'Just once' could not be clicked.")
                }
            }

            try {
                UiWaitUtils.findElementOrNull(proceedButtonSelector)?.let { proceedButton ->
                    if (!proceedButton.visibleBounds.isEmpty && proceedButton.isEnabled) {
                        proceedButton.click()
                        return@retryUntilTimeout true
                    }
                }
            } catch (_: StaleObjectException) {
                return@retryUntilTimeout false
            }

            false
        }

        if (!proceeded) {
            throw AssertionError("Staging backend deeplink confirmation was not shown.")
        }

        waitForBackendConfigSuccessScreenAfterBackendDeeplink()
        return this
    }

    private fun waitForBackendConfigSuccessScreenAfterBackendDeeplink() {
        runCatching {
            UiWaitUtils.waitElement(
                backendConfigSuccessContinueButtonSelector,
                timeout = UiWaitUtils.STABLE_TIMEOUT
            )
        }
    }

    fun clickContinueButtonOnBackendConfigSuccess(): LoginPage {
        runCatching {
            UiWaitUtils.findElementOrNull(backendConfigSuccessContinueButtonSelector)?.click()
        }
        waitForWelcomeScreenAfterBackendConfigContinue()
        return this
    }

    fun assertSsoValidationErrorVisible(expectedMessage: String): LoginPage {
        val error = UiWaitUtils.waitElement(UiSelectorParams(text = expectedMessage))
        assertTrue(
            "Expected SSO validation error '$expectedMessage' to be visible",
            !error.visibleBounds.isEmpty
        )
        return this
    }

    fun clickConfirmButtonOnUsernameSetupPage(): LoginPage {
        val confirmButton = UiWaitUtils.waitElement(confirmButtonSelector)
        confirmButton.click()
        return this
    }

    private fun enterPassword(password: String) {
        val passwordInputField = UiWaitUtils.waitElement(passwordInputFieldSelector)
        val input = passwordInputField.findObject(By.clazz("android.widget.EditText"))
        input.click()
        input.text = password
    }

    private fun waitForLoginPasswordInput(timeout: Duration) =
        UiWaitUtils.waitAnyVisible(
            selectors = listOf(passwordInputFieldSelector, newLoginPasswordInputFieldSelector),
            timeout = timeout,
            pollingInterval = UiWaitUtils.POLLING_FAST
        ) ?: throw AssertionError("Login password input was not visible.")

    private fun waitForWelcomeScreenAfterBackendConfigContinue() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.wait(Until.hasObject(By.pkg(UiAutomatorSetup.appPackage).depth(0)), 10_000)
        val welcomeReady = UiWaitUtils.retryUntilTimeout(timeout = UiWaitUtils.LONG_TIMEOUT) {
            val proceedGone = runCatching {
                UiWaitUtils.waitUntilElementGone(device, proceedButtonGoneSelector, timeout = UiWaitUtils.POLLING_FAST)
            }.isSuccess
            val welcomeVisible = UiWaitUtils.findElementOrNull(emailWelcomeSelector)?.let { !it.visibleBounds.isEmpty } == true
            val emailInputVisible = UiWaitUtils.findElementOrNull(emailInputFieldSelector)?.let {
                !it.visibleBounds.isEmpty
            } == true
            proceedGone && welcomeVisible && emailInputVisible
        }
        if (!welcomeReady) {
            throw AssertionError("Welcome screen was not ready after backend config success Continue.")
        }
        UiWaitUtils.waitElement(emailWelcomeSelector, timeout = UiWaitUtils.LONG_TIMEOUT)
        UiWaitUtils.waitElement(emailInputFieldSelector, timeout = UiWaitUtils.LONG_TIMEOUT)
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

    private companion object {
        const val DEFAULT_BACKEND_NAME = "STAGING"
    }
}
