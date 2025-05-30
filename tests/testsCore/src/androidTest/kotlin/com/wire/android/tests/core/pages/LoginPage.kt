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
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiSelector
//import com.wire.android.tests.support.TIMEOUT_IN_MILLISECONDS
import org.junit.Assert.assertTrue
import uiautomatorutils.UiAutomatorUtils


data class LoginPage(private val device: UiDevice) {
    val backendClient = Backend.loadBackend("STAGING")


    fun enterPersonalUserLoggingEmail(email: String): LoginPage {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        // Click the input field (waits until visible)
        device.findObject(UiSelector().resourceId("userIdentifierInput")).click()
        // Wait again to avoid stale object
        device.findObject(UiSelector().resourceId("userIdentifierInput"))
        // Set text via UiObject (more reliable than UiObject2.text=)
        device.findObject(UiSelector().resourceId("userIdentifierInput")).setText(email)

        return this
    }

    fun clickStagingDeepLink(): LoginPage {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val deepLinkUrl = "wire://access/?config=${backendClient?.deeplink}"

        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(deepLinkUrl)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
        return this
    }


    fun enterPersonalUserLoginPassword(password: String): LoginPage {
        val passwordInputField = UiAutomatorUtils.waitElement(resourceId = "PasswordInput")
        passwordInputField.click()
        passwordInputField.text = password
        return this
    }

    fun assertLoggingPageVisible(): LoginPage {
        val loginPage = UiAutomatorUtils.waitElement(resourceId = "loginButton")
        assertTrue("Login page is not visible", !loginPage.visibleBounds.isEmpty)
        return this

    }

    fun clickLoginButton(): LoginPage {
        val nextButton = UiAutomatorUtils.waitElement(resourceId = "loginButton")
        assertTrue("Login button is not clickable", nextButton.isClickable)
        nextButton.click()
        return this
    }

    fun clickProceedButtonOnDeeplinkOverlay(): LoginPage {
        val proceedButton = UiAutomatorUtils.waitElement(text = "Proceed")
        proceedButton.click()
        return this
    }

    fun clickConfirmButtonOnUsernameSetupPage(): LoginPage {
        val confirmButton = UiAutomatorUtils.waitElement(text = "Confirm")
        confirmButton.click()
        return this
    }


//    fun tapOnEmailField(): LoginPage {
//        val emailSsoCodeField = device.findObject(UiSelector().resourceId("userIdentifierInput"))
//        emailSsoCodeField.waitForExists(TIMEOUT_IN_MILLISECONDS)
//        emailSsoCodeField.click()
//        return this
//    }


    }
