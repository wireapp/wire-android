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
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import kotlin.time.Duration.Companion.seconds

data class LoginPage(private val device: UiDevice) {
    val backendClient = BackendClient.loadBackend("STAGING")

    // Locators
    private val emailInputField = UiSelector().resourceId("userIdentifierInput")
    private val emailInputFieldSelector = UiSelectorParams(resourceId = "userIdentifierInput")
    private val passwordInputFieldSelector = UiSelectorParams(resourceId = "PasswordInput")
    private val loginButtonSelector = UiSelectorParams(resourceId = "loginButton")
    private val proceedButtonSelector = UiSelectorParams(text = "Proceed")
    private val proceedButtonGoneSelector = UiSelector().text("Proceed")
    private val confirmButtonSelector = UiSelectorParams(text = "Confirm")
    private val androidResolverWireDevSelector = UiSelectorParams(text = "Wire Dev")
    private val androidResolverJustOnceSelector = UiSelectorParams(text = "Just once")
    private val emailWelcomeSelector = UiSelectorParams(textMatches = "Enter your (email to start!|credentials to log in)")
    private val invalidEmailErrorSelector = UiSelectorParams(textContains = "Please enter a valid email or SSO code")
    private val incorrectCredentialsErrorSelector = UiSelectorParams(textContains = "These account credentials are incorrect")
    private val forgotPasswordSelector = UiSelectorParams(text = "Forgot password?")
    private val showPasswordButtonSelector = UiSelectorParams(description = "Show password")
    private val hidePasswordButtonSelector = UiSelectorParams(description = "Hide password")
    private val removedDeviceAlertTitleSelector = UiSelectorParams(text = "Removed Device")
    private val removedDeviceAlertMessageSelector = UiSelectorParams(textContains = "You were logged out because your device was removed")
    private val deletedAccountAlertTitleSelector = UiSelectorParams(text = "Deleted account")
    private val deletedAccountAlertMessageSelector = UiSelectorParams(textContains = "You were logged out because your account was deleted")
    private val okButtonSelector = UiSelectorParams(text = "OK")

    fun enterPersonalUserLoggingEmail(email: String): LoginPage {
        enterEmail(email)
        return this
    }

    fun enterPersonalUserLoginPassword(password: String): LoginPage {
        enterPassword(password)
        return this
    }

    fun enterTeamMemberLoggingPassword(password: String): LoginPage {
        enterPassword(password)
        return this
    }

    fun enterTeamOwnerLoggingEmail(email: String): LoginPage {
        enterEmail(email)
        return this
    }

    fun enterTeamMemberLoggingEmail(email: String): LoginPage {
        enterEmail(email)
        return this
    }

    fun enterSSOCodeOnSSOLoginTab(email: String): LoginPage {
        enterEmail(email)
        return this
    }

    fun enterTeamOwnerLoggingPassword(password: String): LoginPage {
        enterPassword(password)
        return this
    }

    fun clickStagingDeepLink(): LoginPage {
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

    fun openDeepLink(deepLinkUrl: String): LoginPage {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(deepLinkUrl)
            setPackage(UiAutomatorSetup.appPackage)
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

    fun clickLoginButton(): LoginPage {
        val nextButton = try {
            UiWaitUtils.waitElement(loginButtonSelector)
        } catch (e: AssertionError) {
            throw AssertionError("Login button not found or not clickable", e)
        }
        nextButton.click()
        return this
    }

    fun enterLoginVerificationCode(code: String): LoginPage {
        val codeInputField = UiWaitUtils.waitElement(UiSelectorParams(className = "android.widget.EditText"))
        codeInputField.click()
        codeInputField.text = code
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
                        val clickableTarget = proceedButton.parent ?: proceedButton
                        clickableTarget.click()
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

        waitForWelcomeScreenAfterBackendDeeplink()
        return this
    }

    fun clickConfirmButtonOnUsernameSetupPage(): LoginPage {
        val confirmButton = UiWaitUtils.waitElement(confirmButtonSelector)
        confirmButton.click()
        return this
    }

    fun clearEmailInputField(): LoginPage {
        enterEmail("")
        return this
    }

    fun assertInvalidEmailErrorVisible(): LoginPage {
        val error = UiWaitUtils.waitElement(invalidEmailErrorSelector)
        assertTrue("Invalid email error is not visible", !error.visibleBounds.isEmpty)
        return this
    }

    fun assertUserLoginScreenVisible(): LoginPage {
        val passwordInputField = UiWaitUtils.waitElement(passwordInputFieldSelector)
        assertTrue("Password input is not visible", !passwordInputField.visibleBounds.isEmpty)
        return this
    }

    fun assertIncorrectCredentialsErrorVisible(): LoginPage {
        val error = UiWaitUtils.waitElement(incorrectCredentialsErrorSelector, timeout = UiWaitUtils.LONG_TIMEOUT)
        assertTrue("Incorrect credentials error is not visible", !error.visibleBounds.isEmpty)
        return this
    }

    fun clickOkButtonOnIncorrectCredentialsAlertIfVisible(): LoginPage {
        UiWaitUtils.clickWhenClickable(okButtonSelector, timeout = UiWaitUtils.SHORT_TIMEOUT)
        return this
    }

    fun clickForgotPasswordButton(): LoginPage {
        UiWaitUtils.waitElement(forgotPasswordSelector).click()
        return this
    }

    fun clickShowPasswordButton(): LoginPage {
        UiWaitUtils.waitElement(showPasswordButtonSelector).click()
        return this
    }

    fun clickHidePasswordButton(): LoginPage {
        UiWaitUtils.waitElement(hidePasswordButtonSelector).click()
        return this
    }

    fun assertLoginPasswordVisible(expectedPassword: String): LoginPage {
        val input = passwordInput()
        assertTrue("Login password is not visible in cleartext", input.text == expectedPassword)
        return this
    }

    fun assertLoginPasswordHidden(): LoginPage {
        val showPasswordButton = UiWaitUtils.waitElement(showPasswordButtonSelector)
        assertTrue("Login password visibility toggle did not return to hidden state", !showPasswordButton.visibleBounds.isEmpty)
        return this
    }

    fun assertRemovedDeviceAlertVisible(): LoginPage {
        val title = UiWaitUtils.waitElement(removedDeviceAlertTitleSelector, timeout = UiWaitUtils.LONG_TIMEOUT)
        assertTrue("Removed device alert title is not visible", !title.visibleBounds.isEmpty)
        val message = UiWaitUtils.waitElement(removedDeviceAlertMessageSelector)
        assertTrue("Removed device alert message is not visible", !message.visibleBounds.isEmpty)
        return this
    }

    fun clickOkButtonOnRemovedDeviceAlert(): LoginPage {
        UiWaitUtils.waitElement(okButtonSelector).click()
        return this
    }

    fun assertDeletedAccountAlertVisible(): LoginPage {
        val title = UiWaitUtils.waitElement(deletedAccountAlertTitleSelector, timeout = UiWaitUtils.LONG_TIMEOUT)
        assertTrue("Deleted account alert title is not visible", !title.visibleBounds.isEmpty)
        val message = UiWaitUtils.waitElement(deletedAccountAlertMessageSelector)
        assertTrue("Deleted account alert message is not visible", !message.visibleBounds.isEmpty)
        return this
    }

    fun clickOkButtonOnDeletedAccountAlert(): LoginPage {
        UiWaitUtils.waitElement(okButtonSelector).click()
        return this
    }

    fun clickOkButtonOnRemovedDeviceAlertIfVisible(): LoginPage {
        val titleVisible = UiWaitUtils.findElementOrNull(removedDeviceAlertTitleSelector)
            ?.let { !it.visibleBounds.isEmpty } == true
        if (titleVisible) {
            UiWaitUtils.clickWhenClickable(okButtonSelector, timeout = UiWaitUtils.SHORT_TIMEOUT)
        }
        return this
    }

    private fun enterEmail(email: String) {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        // UiObject.setText is more reliable here than UiObject2.text for Compose text fields.
        device.findObject(emailInputField).click()
        device.findObject(emailInputField)
        device.findObject(emailInputField).setText(email)
    }

    private fun enterPassword(password: String) {
        val input = passwordInput()
        input.click()
        input.text = password
    }

    private fun passwordInput() =
        UiWaitUtils.waitElement(passwordInputFieldSelector)
            .findObject(By.clazz("android.widget.EditText"))

    private fun waitForWelcomeScreenAfterBackendDeeplink() {
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
            throw AssertionError("Welcome screen was not ready after staging backend deeplink.")
        }
        UiWaitUtils.waitElement(emailWelcomeSelector, timeout = UiWaitUtils.LONG_TIMEOUT)
        UiWaitUtils.waitElement(emailInputFieldSelector, timeout = UiWaitUtils.LONG_TIMEOUT)
    }
}
