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

import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertTrue
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import uiautomatorutils.UiWaitUtils.waitUntilElementGone

class RegistrationPage(private val device: UiDevice) {

    private val welcomePage = UiSelectorParams(text = "Enter your email to start!")
    private val emailInputField = UiSelectorParams(resourceId = "userIdentifierInput")
    private val loginButton = UiSelectorParams(resourceId = "loginButton")
    private val createAccountButton = UiSelectorParams(text = "Create account or team")
    private val createPersonalAccountButton = UiSelectorParams(text = "Create Personal Account")
    private val continueButton = UiSelectorParams(text = "Continue")
    private val termsTitle = UiSelectorParams(text = "Terms of Use")
    private val termsOfUseText = UiSelectorParams(textContains = "Terms of Use and Privacy Policy")
    private val cancelButton = UiSelectorParams(text = "Cancel")
    private val viewButton = UiSelectorParams(text = "View ToU and Privacy Policy")
    private val termsContinueButton = UiSelectorParams(text = "Continue")
    private val nameField = UiSelectorParams(resourceId = "name")
    private val passwordField = UiSelectorParams(resourceId = "password")
    private val confirmPasswordField = UiSelectorParams(resourceId = "confirmPassword")
    private val showPasswordButton = UiSelectorParams(description = "Show password")
    private val hidePasswordButton = UiSelectorParams(description = "Hide password")
    private val userNameInfoText = UiSelectorParams(textContains = "Enter your username. It helps others to find")
    private val userNameHelpText = UiSelectorParams(textContains = "At least 2 character")
    private val editTextClass = By.clazz("android.widget.EditText")
    private val confirmButton = UiSelectorParams(text = "Confirm")
    private val allowNotificationButtons = listOf(
        UiSelectorParams(resourceId = "com.android.permissioncontroller:id/permission_allow_button"),
        UiSelectorParams(text = "Allow")
    )
    private val consentDialogTitle = UiSelectorParams(textContains = "Consent to share user data")
    private val declineButton = UiSelectorParams(text = "Decline")
    private val loginButtonGoneSelector = UiSelector().resourceId("loginButton")
    private val settingUpWireGoneSelector = UiSelector()
        .className("android.widget.TextView")
        .text("Setting up Wire")
    private val agreeButton = UiSelectorParams(text = "Agree")
    private val conversationsPage = UiSelectorParams(text = "Conversations")

    fun assertEmailWelcomePage(): RegistrationPage {
        val element = UiWaitUtils.waitElement(welcomePage)
        assertTrue("Expected 'Enter your email to start!' to be visible", !element.visibleBounds.isEmpty)
        return this
    }

    fun enterPersonalUserRegistrationEmail(email: String): RegistrationPage {
        val success = UiWaitUtils.retryUntilTimeout(timeoutMs = 6_000, pollingIntervalMs = 150) {
            runCatching {
                UiWaitUtils.waitElement(emailInputField, timeoutMillis = 2_000).click()
                UiWaitUtils.waitElement(emailInputField, timeoutMillis = 2_000).text = email
            }.isSuccess
        }

        if (!success) {
            throw AssertionError("Could not enter registration email: email input field was unstable.")
        }
        return this
    }

    @Suppress("NestedBlockDepth")
    fun clickLoginButton(timeoutMs: Long = 10_000): RegistrationPage {
        var lastError: AssertionError? = null

        val success = UiWaitUtils.retryUntilTimeout(timeoutMs = timeoutMs, pollingIntervalMs = 200) {
            try {
                UiWaitUtils.waitElement(loginButton, timeoutMillis = 1_500).click()
                true
            } catch (e: AssertionError) {
                lastError = e
                try {
                    val button = UiWaitUtils.findElementOrNull(loginButton)
                    if (button != null && !button.visibleBounds.isEmpty && button.isEnabled) {
                        button.click()
                        true
                    } else {
                        false
                    }
                } catch (_: StaleObjectException) {
                    false
                }
            } catch (_: StaleObjectException) {
                false
            }
        }

        if (!success) {
            throw AssertionError(
                "Login button was not clickable within ${timeoutMs}ms.",
                lastError
            )
        }
        return this
    }

    fun clickCreateAccountButton(): RegistrationPage {
        val button = UiWaitUtils.waitElement(createAccountButton)
        assertTrue("Create account button is not clickable", button.isClickable)
        button.click()
        return this
    }

    fun clickCreatePersonalAccountButton(): RegistrationPage {
        val button = UiWaitUtils.waitElement(createPersonalAccountButton)
        assertTrue("Button is not enabled", button.isEnabled)
        button.click()
        return this
    }

    fun clickContinueButton(): RegistrationPage {
        val button = UiWaitUtils.waitElement(continueButton)
        button.click()
        return this
    }

    fun assertTermsOfUseModalVisible(): RegistrationPage {
        val title = UiWaitUtils.waitElement(termsTitle)
        val info = UiWaitUtils.waitElement(termsOfUseText)
        val cancel = UiWaitUtils.waitElement(cancelButton)
        val view = UiWaitUtils.waitElement(viewButton)
        val cont = UiWaitUtils.waitElement(termsContinueButton)

        assertTrue("Terms of Use title is not visible", !title.visibleBounds.isEmpty)
        assertTrue("Info text is not visible", !info.visibleBounds.isEmpty)
        assertTrue("Cancel button is not visible", !cancel.visibleBounds.isEmpty)
        assertTrue("View ToU and Privacy Policy button is not visible", !view.visibleBounds.isEmpty)
        assertTrue("Continue button is not visible", !cont.visibleBounds.isEmpty)
        return this
    }

    fun enterFirstName(firstName: String): RegistrationPage {
        val parent = UiWaitUtils.waitElement(nameField)
        val inputName = parent.findObject(editTextClass)
        inputName.text = firstName
        return this
    }

    fun enterPassword(password: String): RegistrationPage {
        val parent = UiWaitUtils.waitElement(passwordField)
        val inputPassword = parent.findObject(editTextClass)
        inputPassword.text = password
        return this
    }

    fun enterConfirmPassword(confirmPassword: String): RegistrationPage {
        val parent = UiWaitUtils.waitElement(confirmPasswordField)
        val inputConfirmPassword = parent.findObject(editTextClass)
        inputConfirmPassword.text = confirmPassword
        return this
    }

    fun clickShowPasswordEyeIcon(): RegistrationPage {
        UiWaitUtils.waitElement(showPasswordButton).click()
        return this
    }

    fun clickHidePasswordEyeIcon(): RegistrationPage {
        UiWaitUtils.waitElement(hidePasswordButton).click()
        return this
    }

    fun verifyConfirmPasswordIsCorrect(expectedPassword: String): RegistrationPage {
        val parent = UiWaitUtils.waitElement(confirmPasswordField)
        val passwordField = parent.findObject(editTextClass)
        val actualPassword = passwordField.text
        assertThat("Static password does not match expected value", actualPassword, `is`(expectedPassword))
        return this
    }

    fun enter2FAOnCreatePersonalAccountPage(code: String): RegistrationPage {
        val codeInputField = UiWaitUtils.waitElement(UiSelectorParams(className = "android.widget.EditText"))
        codeInputField.click()
        codeInputField.text = code
        UiWaitUtils.waitElement(userNameInfoText, timeoutMillis = 15_000)
        return this
    }

    fun assertEnterYourUserNameInfoText(): RegistrationPage {
        val info = UiWaitUtils.waitElement(userNameInfoText, timeoutMillis = 15_000)
        assertTrue("Username info not visible", !info.visibleBounds.isEmpty)
        return this
    }

    fun assertUserNameHelpText(): RegistrationPage {
        val helpText = UiWaitUtils.waitElement(userNameHelpText)
        assertTrue("Username help text is not visible", !helpText.visibleBounds.isEmpty)
        return this
    }

    fun setUserName(username: String): RegistrationPage {
        val userName = UiWaitUtils.waitElement(UiSelectorParams(className = "android.widget.EditText"))
        userName.click()
        userName.text = username
        return this
    }

    fun clickConfirmButton(): RegistrationPage {
        UiWaitUtils.waitElement(confirmButton).click()
        return this
    }

    // Fallback for runs where the PermissionUtils pre-grant does not suppress the Android notification permission dialog.
    fun clickAllowNotificationButton(): RegistrationPage {
        allowNotificationButtons
            .asSequence()
            .mapNotNull(UiWaitUtils::findElementOrNull)
            .firstOrNull { !it.visibleBounds.isEmpty && it.isEnabled }
            ?.let { runCatching { it.click() } }
        return this
    }

    @Suppress("MagicNumber")
    fun clickDeclineShareDataAlert(timeoutMs: Long = 10_000): RegistrationPage {
        val dismissed = UiWaitUtils.retryUntilTimeout(timeoutMs = timeoutMs, pollingIntervalMs = 150) {
            val decline = UiWaitUtils.findElementOrNull(declineButton)
            if (decline != null && !decline.visibleBounds.isEmpty && decline.isEnabled) {
                val bounds = decline.visibleBounds
                runCatching { decline.click() }
                val stillVisibleAfterClick = UiWaitUtils.findElementOrNull(declineButton)?.let { !it.visibleBounds.isEmpty } == true
                if (stillVisibleAfterClick && !bounds.isEmpty) {
                    device.click(bounds.centerX(), bounds.centerY())
                }
                device.waitForIdle(300)
            }

            val dialogVisible = UiWaitUtils.findElementOrNull(consentDialogTitle)?.let { !it.visibleBounds.isEmpty } == true
            val declineVisible = UiWaitUtils.findElementOrNull(declineButton)?.let { !it.visibleBounds.isEmpty } == true
            !dialogVisible && !declineVisible
        }
        if (!dismissed) {
            throw AssertionError("Share data consent alert was not dismissed within ${timeoutMs}ms.")
        }
        return this
    }

    fun clickAgreeShareDataAlert(): RegistrationPage {
        UiWaitUtils.waitElement(agreeButton).click()
        return this
    }

    fun assertConversationPageVisible(): RegistrationPage {
        val page = UiWaitUtils.waitElement(conversationsPage)
        assertTrue("Conversations page is not visible", !page.visibleBounds.isEmpty)
        return this
    }

    fun waitUntilLoginFlowIsCompleted(): RegistrationPage {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        try {
            waitUntilElementGone(device, loginButtonGoneSelector, timeoutMillis = 15_000)
            waitUntilElementGone(device, settingUpWireGoneSelector, timeoutMillis = 35_000)
        } catch (e: AssertionError) {
            throw AssertionError(
                "Login flow did not complete: login button or 'Setting up Wire' is still visible",
                e
            )
        }
        return this
    }

    fun waitUntilRegistrationFlowIsCompleted(): RegistrationPage {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        waitUntilElementGone(device, UiSelector().text("Confirm"), timeoutMillis = 16_000)
        return this
    }

    fun checkIAgreeToShareAnonymousUsageData(): RegistrationPage {
        val checkbox = device.findObject(By.clazz("android.widget.CheckBox"))
            ?: throw AssertionError("Checkbox not found in view hierarchy")
        if (!checkbox.isChecked) {
            checkbox.click()
        }
        return this
    }
}
