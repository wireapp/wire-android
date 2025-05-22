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

import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiSelector
import com.wire.android.tests.support.TIMEOUT_IN_MILLISECONDS
import org.junit.Assert.assertTrue
import utils.UiAutomatorUtils.waitForObject

data class LoginPage(private val device: UiDevice) {

    fun assertEmailWelcomePage(): LoginPage {

    val enterEmailText = device.findObject(UiSelector().text("Enter your email to start!"))
    val isVisible = enterEmailText.waitForExists(TIMEOUT_IN_MILLISECONDS)
    assertTrue("Expected 'Enter your email to start!' to be visible", isVisible)
    return this
}

fun loginWithEmail(email: String): LoginPage {
    val emailField = waitForObject(device, UiSelector().resourceId("userIdentifierInput"))
    emailField.click()
    emailField.setText(email)
    return this
}

fun assertAndClickLoginButton(): LoginPage {
    val loginButton = waitForObject(device, UiSelector().resourceId("loginButton"))
    assertTrue("Login button is not enabled", loginButton.isClickable)
    loginButton.click()
    return this
}
    fun tapOnEmailField(): LoginPage {
        val emailSsoCodeField = device.findObject(UiSelector().resourceId("userIdentifierInput"))
        emailSsoCodeField.waitForExists(TIMEOUT_IN_MILLISECONDS)
        emailSsoCodeField.click()
        return this
    }

    fun typeEmail(email: String): LoginPage {
        val emailSsoCodeField: UiObject =
            device.findObject(UiSelector().resourceId("userIdentifierInput"))
        emailSsoCodeField.waitForExists(TIMEOUT_IN_MILLISECONDS)
        emailSsoCodeField.setText(email)
        return this
    }

    fun shouldEnableTheLoginButtonWhenValid(): LoginPage {
        val loginButton = device.findObject(UiSelector().resourceId("loginButton"))
        loginButton.waitForExists(TIMEOUT_IN_MILLISECONDS)
        assertTrue("LoginButton not found or not enabled", loginButton.isClickable)
        return this
    }

    fun clickLoginButton(): LoginPage {
        val loginButton = device.findObject(UiSelector().resourceId("loginButton"))
        loginButton.click()
        return this
    }

    fun clickCreateAccountButton(): LoginPage {
        val loginButton = device.findObject(UiSelector().resourceId("Create account"))
        loginButton.click()
        return this
    }

//    fun enterEmailOnCreatePersonalAccountPage(email: String): LoginPage {
//       // val emailInputField = device.findObject(UiSelector().className("android.view.View").description("EMAIL"))
//
//        val emailInputField = device.findObject(UiSelector().className("android.view.View").descriptionContains("EMAIL"))
//
//        //emailInputField.waitForExists(TIMEOUT_IN_MILLISECONDS)
//        //emailInputField.click()
//        emailInputField.setText(email)
//
//        return this
//    }

    fun enterEmail(email: String): LoginPage {
        val emailField: UiObject =  device.findObject(UiSelector().className("android.widget.EditText").instance(0))
            //device.findObject(UiSelector().text("EMAIL"))
            //device.findObject(UiSelector().resourceId("userIdentifierInput")
        //emailField.waitForExists(TIMEOUT_IN_MILLISECONDS)
        //emailField.click()

        emailField.setText(email)
        return this
    }
//     fun enterEmailOnCreate(email: String): LoginPage {
//            val emailInputField = device.findObject(UiSelector().className("android.view.View").description("EMAIL"))
//         //emailInputField.waitForExists(TIMEOUT_IN_MILLISECONDS)
//         emailInputField.setText(email)
//
//        return this
//    }
}
