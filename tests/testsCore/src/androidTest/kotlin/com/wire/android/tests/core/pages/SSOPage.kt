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
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import uiautomatorutils.UiWaitUtils.toBySelector

data class SSOPage(private val device: UiDevice) {

    private val oktaUsernameField = UiSelectorParams(resourceId = "okta-signin-username")
    private val oktaPasswordField = UiSelectorParams(resourceId = "okta-signin-password")
    private val oktaSignInButton = UiSelectorParams(resourceId = "okta-signin-submit")
    fun enterOktaEmail(email: String): SSOPage {
        val usernameField = UiWaitUtils.waitElement(oktaUsernameField)
        usernameField.text = email
        return this
    }

    fun enterOktaPassword(password: String): SSOPage {
        val passwordField = UiWaitUtils.waitElement(oktaPasswordField)
        passwordField.text = password
        return this
    }

    fun tapOktaSignIn(): SSOPage {
        val signInBtn = UiWaitUtils.waitElement(oktaSignInButton)
        signInBtn.click()
        return this
    }

    fun waitUntilOktaPageLoaded(timeoutMs: Long = 20_000): SSOPage {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        try {
            val sel = oktaSignInButton.toBySelector()
            if (!device.wait(Until.hasObject(sel), timeoutMs)) {
                throw AssertionError()
            }
        } catch (e: AssertionError) {
            throw AssertionError(
                "Okta page did not load: Email and password input field is not visible",
                e
            )
        }
        return this
    }
}
