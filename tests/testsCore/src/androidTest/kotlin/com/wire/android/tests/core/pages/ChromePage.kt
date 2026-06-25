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
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import kotlin.time.Duration.Companion.seconds

data class ChromePage(private val device: UiDevice) {

    private val useWithoutAccountLocator = UiSelectorParams(text = "Use without an account")
    private val noThanksLocator = UiSelectorParams(text = "No thanks")
    private val resetLoginLocator = UiSelectorParams(text = "Reset login")
    private val signInLocator = UiSelectorParams(text = "Sign In")
    private val editTextLocator = UiSelectorParams(className = "android.widget.EditText")

    fun clickUseWithoutAccount(): ChromePage {
        UiWaitUtils.waitElement(useWithoutAccountLocator).click()
        return this
    }

    fun dismissFirstRunIfVisible(): ChromePage {
        runCatching {
            UiWaitUtils.waitElement(useWithoutAccountLocator, timeout = 2.seconds).click()
        }
        return this
    }

    fun dismissNotificationsPromptIfVisible(): ChromePage {
        runCatching {
            UiWaitUtils.waitElement(noThanksLocator, timeout = 2.seconds).click()
        }
        return this
    }

    fun clickUseWithoutAccountIfVisible(): ChromePage {
        UiWaitUtils.clickWhenClickable(useWithoutAccountLocator, timeout = 2.seconds)
        return this
    }

    fun clickResetLoginIfVisible(): ChromePage {
        UiWaitUtils.clickWhenClickable(resetLoginLocator, timeout = 2.seconds)
        return this
    }

    fun enterKeycloakEmail(email: String): ChromePage {
        val inputs = waitForKeycloakInputs()
        inputs.first().click()
        inputs.first().text = email
        return this
    }

    fun enterKeycloakPassword(password: String): ChromePage {
        val inputs = waitForKeycloakInputs()
        inputs.last().click()
        inputs.last().text = password
        return this
    }

    fun clickKeycloakSignIn(): ChromePage {
        UiWaitUtils.waitElement(signInLocator, timeout = UiWaitUtils.VERY_LONG_TIMEOUT).click()
        return this
    }

    fun assertUrlContains(expectedUrlPart: String): ChromePage {
        UiWaitUtils.waitUntilVisibleOrThrow(
            params = UiSelectorParams(textContains = expectedUrlPart),
            timeout = UiWaitUtils.LONG_TIMEOUT,
            errorMessage = "Expected URL '$expectedUrlPart' was not found in Chrome"
        )
        return this
    }

    private fun isInstalled(pkg: String): Boolean {
        val output = runShellCommand("pm list packages $pkg")
        return output.contains(pkg)
    }

    fun clearInstalledBrowsers() {
        listOf(
            "com.android.chrome",
            "app.vanadium.browser",
            "org.lineageos.jelly"
        )
            .filter(::isInstalled)
            .forEach { pkg ->
                val result = runShellCommand("pm clear $pkg")
                println("Cleared $pkg -> $result")
            }
    }

    private fun runShellCommand(command: String): String =
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            .executeShellCommand(command)
            .trim()

    private fun waitForKeycloakInputs() = UiWaitUtils.retryUntilTimeout(
        timeout = UiWaitUtils.VERY_LONG_TIMEOUT,
        pollingInterval = UiWaitUtils.POLLING_DEFAULT
    ) {
        device.findObjects(By.clazz("android.widget.EditText")).size >= 2 ||
            UiWaitUtils.findElementOrNull(editTextLocator) != null
    }.let {
        if (!it) {
            throw AssertionError("Keycloak login inputs were not visible.")
        }
        device.findObjects(By.clazz("android.widget.EditText"))
    }
}
