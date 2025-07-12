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
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertTrue
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import uiautomatorutils.UiWaitUtils.waitUntilElementGone

data class RegistrationPage(private val device: UiDevice) {

    fun assertEmailWelcomePage(): RegistrationPage {
        val emailPrompt = UiWaitUtils.waitElement(UiSelectorParams(text = "Enter your email to start!"))
        assertTrue("Expected 'Enter your email to start!' to be visible", !emailPrompt.visibleBounds.isEmpty)
        return this
    }

    fun enterPersonalUserRegistrationEmail(email: String): RegistrationPage {
        val emailField = UiWaitUtils.waitElement(UiSelectorParams(resourceId = "userIdentifierInput"))
        emailField.click()
        emailField.text = email
        return this
    }

    fun assertAndClickLoginButton(): RegistrationPage {
        val loginButton = UiWaitUtils.waitElement(UiSelectorParams(resourceId = "loginButton"))
        assertTrue("Login button is not clickable", loginButton.isClickable)
        loginButton.click()
        return this
    }

    fun clickCreateAccountButton(): RegistrationPage {
        val createAccountButton = UiWaitUtils.waitElement(UiSelectorParams(text = "Create account or team"))
        assertTrue("Create account button is not clickable", createAccountButton.isClickable)
        createAccountButton.click()
        return this
    }

    fun clickCreatePersonalAccountButton(): RegistrationPage {
        val createPersonalAccountButton = UiWaitUtils.waitElement(UiSelectorParams(text = "Create Personal Account"))
        assertTrue("Button is not enabled", createPersonalAccountButton.isEnabled)
        createPersonalAccountButton.click()
        return this
    }

    fun clickContinueButton(): RegistrationPage {
        val continueButton = UiWaitUtils.waitElement(UiSelectorParams(text = "Continue"))
        continueButton.click()
        return this
    }

    fun assertTermsOfUseModalVisible() {
        val termsTitle = UiWaitUtils.waitElement(UiSelectorParams(text = "Terms of Use"))

        val infoText = UiWaitUtils.waitElement(
            UiSelectorParams(
                textContains = "Terms of Use and Privacy Policy"
            )
        )

        val cancelButton = UiWaitUtils.waitElement(
            UiSelectorParams(text = "Cancel")
        )

        val viewButton = UiWaitUtils.waitElement(
            UiSelectorParams(text = "View ToU and Privacy Policy")
        )

        val continueButton = UiWaitUtils.waitElement(
            UiSelectorParams(text = "Continue")
        )

        assertTrue("Terms of Use title is not visible", !termsTitle.visibleBounds.isEmpty)
        assertTrue("Info text is not visible", !infoText.visibleBounds.isEmpty)
        assertTrue("Cancel button is not visible", !cancelButton.visibleBounds.isEmpty)
        assertTrue(
            "View ToU and Privacy Policy button is not visible",
            !viewButton.visibleBounds.isEmpty
        )
        assertTrue("Continue button is not visible", !continueButton.visibleBounds.isEmpty)
    }

    fun enterFirstName(firstName: String): RegistrationPage {
        val parent = UiWaitUtils.waitElement(UiSelectorParams(resourceId = "name"))
        val editText = parent.findObject(By.clazz("android.widget.EditText"))
        editText.text = firstName
        return this
    }

    fun enterPassword(password: String): RegistrationPage {
        val parent = UiWaitUtils.waitElement(UiSelectorParams(resourceId = "password"))
        val inputPassword = parent.findObject(By.clazz("android.widget.EditText"))
        inputPassword.text = password
        return this
    }

    fun enterConfirmPassword(confirmPassword: String): RegistrationPage {
        val parent = UiWaitUtils.waitElement(UiSelectorParams(resourceId = "confirmPassword"))
        val inputPassword = parent.findObject(By.clazz("android.widget.EditText"))
        inputPassword.text = confirmPassword
        return this
    }

    fun clickShowPasswordEyeIcon(): RegistrationPage {
        UiWaitUtils.waitElement(UiSelectorParams(description = "Show password")).click()
        return this
    }

    fun verifyConfirmPasswordIsCorrect(expectedPassword: String): RegistrationPage {
        val parent = UiWaitUtils.waitElement(UiSelectorParams(resourceId = "confirmPassword"))
        val passwordField = parent.findObject(By.clazz("android.widget.EditText"))
        val actualPassword = passwordField.text
        assertThat("Static password does not match expected value", actualPassword, `is`(expectedPassword))
        return this
    }

    fun clickHidePasswordEyeIcon(): RegistrationPage {
        UiWaitUtils.waitElement(UiSelectorParams(description = "Hide password")).click()
        return this
    }

    fun enter2FAOnCreatePersonalAccountPage(code: String): RegistrationPage {
        val codeInputField = UiWaitUtils.waitElement(UiSelectorParams(className = "android.widget.EditText"))
        codeInputField.click()
        codeInputField.text = code
        return this
    }

    fun assertEnterYourUserNameInfoText() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        // Wait until "Resend code" is gone before continuing
        val resendCodeSelector = UiSelector().text("Resend code")
        waitUntilElementGone(device, resendCodeSelector, timeoutMillis = 10_000)
        // Now assert the presence of the username info text
        val infoText = UiWaitUtils.waitElement(
            UiSelectorParams(
                textContains = "Enter your username. It helps others to find"
            )
        )
        assertTrue("Username info text is not visible", !infoText.visibleBounds.isEmpty)
    }

    fun assertUserNameHelpText() {
        val helpText = UiWaitUtils.waitElement(UiSelectorParams(textContains = "At least 2 character"))
        assertTrue("Username help text is not visible", !helpText.visibleBounds.isEmpty)
    }

    fun setUserName(username: String): RegistrationPage {
        val userNameInput = UiWaitUtils.waitElement(UiSelectorParams(className = "android.widget.EditText"))
        userNameInput.click()
        userNameInput.text = username
        return this
    }

    fun clickConfirmButton(): RegistrationPage {
        UiWaitUtils.waitElement(UiSelectorParams(text = "Confirm")).click()
        return this
    }

    fun clickAllowNotificationButton(): RegistrationPage {
        UiWaitUtils.waitElement(UiSelectorParams(resourceId = "com.android.permissioncontroller:id/permission_allow_button")).click()
        return this
    }

    fun clickDeclineShareDataAlert(): RegistrationPage {
        UiWaitUtils.waitElement(UiSelectorParams(text = "Decline")).click()
        return this
    }

    fun clickAgreeShareDataAlert(): RegistrationPage {
        UiWaitUtils.waitElement(UiSelectorParams(text = "Agree")).click()
        return this
    }

    fun assertConversationPageVisible() {
        val conversationPage = UiWaitUtils.waitElement(UiSelectorParams(text = "Conversations"))
        assertTrue("Conversations page is not visible", !conversationPage.visibleBounds.isEmpty)
    }

    fun waitUntilLoginFlowIsComplete() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // Wait for "loginButton" to disappear (timeout: 10s)
        val loginButtonSelector = UiSelector().resourceId("loginButton")
        waitUntilElementGone(device, loginButtonSelector, timeoutMillis = 10_000)

        // Wait for "Setting up Wire" text to disappear (timeout: 30s)
        val settingUpWireSelector = UiSelector()
            .className("android.widget.TextView")
            .text("Setting up Wire")
        waitUntilElementGone(device, settingUpWireSelector, timeoutMillis = 30_000)
    }

    fun waitUntilRegistrationFlowIsComplete() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val loginButtonSelector = UiSelector().text("Confirm")
        waitUntilElementGone(device, loginButtonSelector, timeoutMillis = 14_000)
    }

    fun checkIagreeToShareAnonymousUsageData() {
        val checkbox = device.findObject(By.clazz("android.widget.CheckBox"))
            ?: throw AssertionError("Checkbox not found in view hierarchy")
        if (!checkbox.isChecked) {
            checkbox.click()
        }
    }
}
