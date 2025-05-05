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
package com.wire.android.tests.core.login.pages

import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiSelector
import com.wire.android.tests.support.TIMEOUT_IN_MILLISECONDS
import org.junit.Assert.assertTrue

data class LoginPage(private val device: UiDevice) {

    fun tapOnEmailField(): LoginPage {
        val emailSsoCodeField = device.findObject(UiSelector().resourceId("userIdentifierInput"))
        emailSsoCodeField.waitForExists(TIMEOUT_IN_MILLISECONDS)
        emailSsoCodeField.click()
        return this
    }

    fun typeEmail(email: String): LoginPage {
        val emailSsoCodeField: UiObject = device.findObject(UiSelector().resourceId("userIdentifierInput"))
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

}
