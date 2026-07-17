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

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class SSOPage(private val device: UiDevice) {

    private val keycloakUsernameLabel = UiSelectorParams(text = "Username or email")
    private val keycloakPasswordLabel = UiSelectorParams(text = "Password")
    private val keycloakSignInButton = UiSelectorParams(text = "Sign In")

    fun enterKeycloakEmail(email: String): SSOPage {
        val usernameField = inputFieldBelow(keycloakUsernameLabel, "Keycloak username field")
        usernameField.click()
        usernameField.text = email
        return this
    }

    fun enterKeycloakPassword(password: String): SSOPage {
        val passwordField = inputFieldBelow(keycloakPasswordLabel, "Keycloak password field")
        passwordField.click()
        passwordField.text = password
        return this
    }

    fun tapKeycloakSignIn(): SSOPage {
        val signInBtn = UiWaitUtils.waitElement(keycloakSignInButton)
        signInBtn.click()
        return this
    }

    fun waitUntilKeycloakPageLoaded(timeout: Duration = 20.seconds): SSOPage {
        UiWaitUtils.waitElement(keycloakUsernameLabel, timeout = timeout)
        return this
    }

    private fun inputFieldBelow(label: UiSelectorParams, fieldName: String): UiObject2 {
        val labelElement = UiWaitUtils.waitElement(label, timeout = 15.seconds)
        return device.findObjects(By.clazz("android.widget.EditText"))
            .firstOrNull { editText ->
                !editText.visibleBounds.isEmpty &&
                    editText.visibleBounds.top >= labelElement.visibleBounds.bottom
            }
            ?: throw AssertionError("$fieldName was not visible.")
    }
}
