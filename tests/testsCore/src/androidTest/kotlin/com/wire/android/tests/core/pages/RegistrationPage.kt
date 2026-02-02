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
    private val allowNotificationButton =
        UiSelectorParams(
            resourceId = "com.android.permissioncontroller:id/permission_allow_button"
        )
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
        val emailIputfield = UiWaitUtils.waitElement(emailInputField)
        emailIputfield.click()
        emailIputfield.text = email
        return this
    }

    fun clickLoginButton(): RegistrationPage {
        UiWaitUtils.waitElement(loginButton).click()
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
        return this
    }

    fun assertEnterYourUserNameInfoText(): RegistrationPage {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        waitUntilElementGone(device, UiSelector().text("Resend code"), timeoutMillis = 10_000)
        val info = UiWaitUtils.waitElement(userNameInfoText)
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

    fun clickAllowNotificationButton(): RegistrationPage {
        UiWaitUtils.waitElement(allowNotificationButton).click()
        return this
    }

    fun clickDeclineShareDataAlert(): RegistrationPage {
        UiWaitUtils.waitElement(declineButton).click()
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
        waitUntilElementGone(device, UiSelector().text("Confirm"), timeoutMillis = 14_000)
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
