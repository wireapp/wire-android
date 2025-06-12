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

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import uiautomatorutils.UiWaitUtils
import uiautomatorutils.UiWaitUtils.waitUntilElementGone
import user.UserClient


data class RegistrationPage(private val device: UiDevice) {

    fun assertEmailWelcomePage(): RegistrationPage {
        val emailPrompt = UiWaitUtils.waitElement(text = "Enter your email to start!")
        assertTrue("Expected 'Enter your email to start!' to be visible", !emailPrompt.visibleBounds.isEmpty)
        return this

    }

    fun enterPersonalUserRegistrationEmail(email: String): RegistrationPage {
        val emailField = UiWaitUtils.waitElement(resourceId = "userIdentifierInput")
        emailField.click()
        emailField.text = email  // UiObject2 uses `.text =` instead of `setText()`
        return this
    }


    fun assertAndClickLoginButton(): RegistrationPage {
        val loginButton = UiWaitUtils.waitElement(resourceId = "loginButton")
        assertTrue("Login button is not clickable", loginButton.isClickable)
        loginButton.click()
        return this
    }


    fun clickCreateAccountButton(): RegistrationPage {
        val createAccountButton = UiWaitUtils.waitElement(text = "Create account")
        assertTrue("Create account button is not clickable", createAccountButton.isClickable)
        createAccountButton.click()
        return this
    }


    fun clickContinueButton(): RegistrationPage {
        val continueButton = UiWaitUtils.waitElement(text = "Continue")
        continueButton.click()
        return this
    }


    fun enterEmailOnCreatePersonalAccountPage(email: String): RegistrationPage {
        val emailInputField = UiWaitUtils.waitElement(className = "android.widget.EditText")
        //timeout = 10000L // optional, if EditText takes longer to load
        emailInputField.click()
        emailInputField.text = email
        return this
    }

    fun assertAndClickContinueButtonOnCreatePersonalAccountPage(): RegistrationPage {
        val continueButton = UiWaitUtils.waitElement(text = "Continue")
        continueButton.click()
        return this
    }

    fun assertTermsOfUseModalVisible() {
        val termsTitle = UiWaitUtils.waitElement(
            text = "Terms of Use"
        )

        val infoText = UiWaitUtils.waitElement(
            textContains = "Terms of Use and Privacy Policy"
        )

        val cancelButton = UiWaitUtils.waitElement(
            text = "Cancel"
        )

        val viewButton = UiWaitUtils.waitElement(
            text = "View ToU and Privacy Policy"
        )

        val continueButton = UiWaitUtils.waitElement(
            text = "Continue"
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
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // Click the parent container to focus, if needed
        device.findObject(UiSelector().resourceId("firstName")).click()

        // Find the EditText inside the parent and set the text
        device.findObject(
            UiSelector().resourceId("firstName")
                .childSelector(UiSelector().className("android.widget.EditText"))
        ).setText(firstName)

        return this
    }




    fun enterLastName(lastName: String): RegistrationPage {
        val parent = UiWaitUtils.waitElement(resourceId = "lastName")
        val editText = parent.findObject(By.clazz("android.widget.EditText"))
        editText.text = lastName
        return this
    }

    fun enterPassword(password: String): RegistrationPage {
        val parent = UiWaitUtils.waitElement(resourceId = "password")
        val editText = parent.findObject(By.clazz("android.widget.EditText"))
        editText.text = password
        return this
    }

    fun enterConfirmPassword(confirmPassword: String): RegistrationPage {
        val parent = UiWaitUtils.waitElement(resourceId = "confirmPassword")
        val editText = parent.findObject(By.clazz("android.widget.EditText"))
        editText.text = confirmPassword
        return this
    }


    fun clickShowPasswordEyeIcon(): RegistrationPage {
        UiWaitUtils.waitElement(description = "Show password").click()
        return this
    }

    fun verifyStaticPasswordIsCorrect(): RegistrationPage {
        val expectedPassword = "Aqa123456!"
        val actualPassword = UserClient.generateUniqueUserInfo().staticPassword
        assertEquals("Static password does not match expected value", expectedPassword, actualPassword)
        return this
    }

    fun clickHidePasswordEyeIcon(): RegistrationPage {
        UiWaitUtils.waitElement(description = "Hide password").click()
        return this
    }

    fun enter2FAOnCreatePersonalAccountPage(code: String): RegistrationPage {
        val codeInputField = UiWaitUtils.waitElement(className = "android.widget.EditText")
        codeInputField.click()
        codeInputField.text = code
        return this
    }

    fun assertAccountCreationSuccessMessage() {
        val successMessage = UiWaitUtils.waitElement(
            textContains = "You have successfully created your personal account. Start communicating"
        )
        assertTrue("Account creation success message is not visible", !successMessage.visibleBounds.isEmpty)
    }

    fun clickGetStartedButton(): RegistrationPage {
        UiWaitUtils.waitElement(text = "Get Started").click()
        return this
    }

    fun assertUserNamePageIsVisible() {
        val userNamePage = UiWaitUtils.waitElement(text = "Your Username")
        assertTrue("Your Username is not visible", !userNamePage.visibleBounds.isEmpty)
    }

    fun assertEnterYourUserNameInfoText() {
        val infoText = UiWaitUtils.waitElement(textContains = "Enter your username. It helps others to find")
        assertTrue("Username info text is not visible", !infoText.visibleBounds.isEmpty)
    }

    fun assertUserNameHelpText() {
        val helpText = UiWaitUtils.waitElement(textContains = "At least 2 character")
        assertTrue("Username help text is not visible", !helpText.visibleBounds.isEmpty)
    }

    fun setUserName(username: String): RegistrationPage {
        val userNameInput = UiWaitUtils.waitElement(className = "android.widget.EditText")
        userNameInput.click()
        userNameInput.text = username
        return this
    }

    fun clickConfirmButton(): RegistrationPage {
        UiWaitUtils.waitElement(text = "Confirm").click()
        return this
    }

    fun clickAllowNotificationButton(): RegistrationPage {
        UiWaitUtils.waitElement(resourceId = "com.android.permissioncontroller:id/permission_allow_button").click()
        return this
    }


//    fun clickAllowNotificationButton(): RegistrationPage {
//        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
//        device.findObject(UiSelector().resourceId("com.android.permissioncontroller:id/permission_allow_button")).click()
//        return this
//    }

    fun clickDeclineShareDataAlert(): RegistrationPage {
        UiWaitUtils.waitElement(text = "Decline").click()
        return this
    }

    fun clickAgreeShareDataAlert(): RegistrationPage {
        UiWaitUtils.waitElement(text = "Agree").click()
        return this
    }

    fun assertConversationPageVisible() {
        val conversationPage = UiWaitUtils.waitElement(text = "Conversations")
        assertTrue("Conversations page is not visible", !conversationPage.visibleBounds.isEmpty)
    }


    fun waitUntilLoginFlowIsComplete(timeoutMillis: Long = 30_000, pollingInterval: Long = 500) {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // First: wait for loginButton to disappear
        val loginButtonSelector = UiSelector().resourceId("loginButton")
        waitUntilElementGone(device, loginButtonSelector, timeoutMillis, pollingInterval)

        // Then: wait for "Setting up Wire" TextView to disappear
        val settingUpWireSelector = UiSelector().className("android.widget.TextView").text("Setting up Wire")
        waitUntilElementGone(device, settingUpWireSelector, timeoutMillis, pollingInterval)
    }

}


//[@resource-id=\"PasswordInput\"]"
