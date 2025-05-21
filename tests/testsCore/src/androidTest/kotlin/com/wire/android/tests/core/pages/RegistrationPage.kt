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

import utils.UiAutomatorUtils.waitForObject
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import com.wire.android.tests.support.TIMEOUT_IN_MILLISECONDS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import user.UserClient


data class RegistrationPage(private val device: UiDevice) {

    fun assertEmailWelcomePage(): RegistrationPage {

        val enterEmailText = device.findObject(UiSelector().text("Enter your email to start!"))
        val isVisible = enterEmailText.waitForExists(TIMEOUT_IN_MILLISECONDS)
        assertTrue("Expected 'Enter your email to start!' to be visible", isVisible)
        return this
    }

    fun loginWithEmail(email: String): RegistrationPage {
        val emailField = waitForObject(device, UiSelector().resourceId("userIdentifierInput"))
        emailField.click()
        emailField.setText(email)
        return this
    }

    fun assertAndClickLoginButton(): RegistrationPage {
        val loginButton = waitForObject(device, UiSelector().resourceId("loginButton"))
        assertTrue("Login button is not enabled", loginButton.isClickable)
        loginButton.click()
        return this
    }

    fun clickCreateAccountButton(): RegistrationPage {
        val loginButton = waitForObject(device, UiSelector().resourceId("Create account"))
        loginButton.click()
        return this
    }

    fun clickContinueButton(): RegistrationPage {
        val continueButton = waitForObject(device, UiSelector().text("Continue"))
        continueButton.click()
        return this
    }

    fun enterEmailOnCreatePersonalAccountPage(email: String): RegistrationPage {
        val emailInputField = waitForObject(device, UiSelector().className("android.widget.EditText").instance(0))
        emailInputField.click()
        emailInputField.setText(email)
        return this
    }

    fun assertAndClickContinueButtonOnCreatePersonalAccountPage(): RegistrationPage {
        val continueButton = waitForObject(device, UiSelector().text("Continue"))
        continueButton.click()
        return this
    }

    fun assertTermsOfUseModalVisible() {
        val termsTitle = waitForObject(device, UiSelector().text("Terms of Use"))
        val infoText = waitForObject(device, UiSelector().textContains("Terms of Use and Privacy Policy"))
        val cancelButton = waitForObject(device, UiSelector().text("Cancel"))
        val viewButton = waitForObject(device, UiSelector().text("View ToU and Privacy Policy"))
        val continueButton = waitForObject(device, UiSelector().text("Continue"))

        assertTrue("Terms of Use title is not visible", termsTitle.exists())
        assertTrue("Info text is not visible", infoText.exists())
        assertTrue("Cancel button is not visible", cancelButton.exists())
        assertTrue("View ToU and Privacy Policy button is not visible", viewButton.exists())
        assertTrue("Continue button is not visible", continueButton.exists())
    }

    fun enterPersonalDetails(
        firstName: String,
        lastName: String,
        password: String,
        confirmPassword: String
    ): RegistrationPage {
        waitForObject(
            device,
            UiSelector().resourceId("firstName").childSelector(UiSelector().className("android.widget.EditText"))
        ).setText(firstName)
        waitForObject(device, UiSelector().resourceId("lastName").childSelector(UiSelector().className("android.widget.EditText"))).setText(
            lastName
        )
        waitForObject(device, UiSelector().resourceId("password").childSelector(UiSelector().className("android.widget.EditText"))).setText(
            password
        )
        waitForObject(
            device,
            UiSelector().resourceId("confirmPassword").childSelector(UiSelector().className("android.widget.EditText"))
        ).setText(confirmPassword)
        return this
    }

    fun clickShowPasswordEyeIcon(): RegistrationPage {
        val eyeIcon = waitForObject(device, UiSelector().description("Show password"))
        eyeIcon.click()
        return this
    }

    fun verifyStaticPasswordIsCorrect(): RegistrationPage {
        val expectedPassword = "Aqa123456!"
        val actualPassword = UserClient.generateUniqueUserInfo().staticPassword
        assertEquals("Static password does not match expected value", expectedPassword, actualPassword)
        return this
    }

    fun clickHidePasswordEyeIcon(): RegistrationPage {
        val eyeIcon = waitForObject(device, UiSelector().description("Hide password"))
        eyeIcon.click()
        return this
    }

    fun enter2FAOnCreatePersonalAccountPage(code: String): RegistrationPage {
        val codeInputField = waitForObject(device, UiSelector().className("android.widget.EditText"))
        codeInputField.click()
        codeInputField.setText(code)
        return this
    }

    fun assertAccountCreationSuccessMessage() {
        val accountCreatedMessage =
            waitForObject(device, UiSelector().textContains("You have successfully created your personal account. Start communicating"))
        assertTrue("Account creation success message is not visible", accountCreatedMessage.exists())
    }

    fun clickGetStartedButton(): RegistrationPage {
        val getStartedButton = waitForObject(device, UiSelector().text("Get Started"))
        getStartedButton.click()
        return this
    }

    fun assertUserNamePageIsVisible() {
        val userNameIputPage = waitForObject(device, UiSelector().text("Your Username"))
        assertTrue("Your Username is not visible", userNameIputPage.exists())
    }

    fun assertEnterYourUserNameInfoText() {
        val enterUserNameInfoText = waitForObject(device, UiSelector().textContains("Enter your username. It helps others to find"))
        assertTrue("Your Username is not visible", enterUserNameInfoText.exists())

    }

    fun assertUserNameHelpText() {
        val userNameHelpText = waitForObject(device, UiSelector().textContains("At least 2 character"))
        assertTrue("Your Username is not visible", userNameHelpText.exists())
    }

    fun setUserName(username: String): RegistrationPage {
        val userName = waitForObject(
            device,
            UiSelector().textContains("USERNAME").className("android.widget.TextView").fromParent(
                UiSelector().className("android.widget.EditText")
            )
        )
        userName.click()
        userName.setText(username)
        return this
    }

    fun clickConfirmButton(): RegistrationPage {
        val confirmButton = waitForObject(device, UiSelector().text("Confirm"))
        confirmButton.click()
        return this
    }

    fun clickAllowNotificationmButton(): RegistrationPage {
        val allowNotificationButton =
            waitForObject(device, UiSelector().resourceId("com.android.permissioncontroller:id/permission_allow_button"))
        allowNotificationButton.click()
        return this
    }

    fun clickDeclineShareDataAlert(): RegistrationPage {
        val shareDataAlert = waitForObject(device, UiSelector().text("Decline"))
        shareDataAlert.click()
        return this
    }

    fun assertConversationPageVisible() {
        val assertConversationPage = waitForObject(device, UiSelector().text("Conversations"))
        assertTrue("Conversations page is not visible", assertConversationPage.exists())
    }
}


//    fun tapOnEmailField(): RegistrationPage {
//        val emailSsoCodeField = device.findObject(UiSelector().resourceId("userIdentifierInput"))
//        emailSsoCodeField.waitForExists(TIMEOUT_IN_MILLISECONDS)
//        emailSsoCodeField.click()
//        return this
//    }
