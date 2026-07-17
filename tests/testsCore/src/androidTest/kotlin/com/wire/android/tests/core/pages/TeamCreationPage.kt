/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

import android.view.KeyEvent
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import org.junit.Assert.assertTrue
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import kotlin.time.Duration.Companion.seconds

class TeamCreationPage(private val device: UiDevice) {

    private val createTeamHeading = UiSelectorParams(text = "Create a team")
    private val profileNameInputField = UiSelectorParams(resourceId = "display-name")
    private val teamNameInputField = UiSelectorParams(resourceId = "team-name")
    private val passwordLabel = UiSelectorParams(text = "Password")
    private val confirmPasswordLabel = UiSelectorParams(text = "Confirm password")
    private val organizationSizeDropdown = UiSelectorParams(text = "Please select")
    private val firstOrganizationSizeOption = UiSelectorParams(text = "1 - 25")
    private val continueButton = UiSelectorParams(text = "Continue")
    private val youHaveGotMailHeading = UiSelectorParams(textContains = "got mail")
    private val teamCreatedHeading = UiSelectorParams(textContains = "you created a new")
    private val closeButton = UiSelector().resourceId("com.android.chrome:id/close_button")

    fun assertCreateTeamPageVisible(): TeamCreationPage {
        val heading = UiWaitUtils.waitElement(createTeamHeading, timeout = 15.seconds)
        assertTrue("Create a team page is not visible", !heading.visibleBounds.isEmpty)
        return this
    }

    fun enterEmail(email: String?): TeamCreationPage {
        val emailField = device.findObjects(By.clazz("android.widget.EditText"))[0]
        emailField.click()
        emailField.text = email
        return this
    }

    fun enterProfileName(name: String?): TeamCreationPage {
        val profileNameField = UiWaitUtils.waitElement(profileNameInputField, timeout = 15.seconds)
        profileNameField.click()
        profileNameField.text = name
        return this
    }

    fun enterTeamName(teamName: String): TeamCreationPage {
        val teamNameField = UiWaitUtils.waitElement(teamNameInputField, timeout = 15.seconds)
        teamNameField.click()
        teamNameField.text = teamName
        return this
    }

    fun enterPassword(password: String?): TeamCreationPage {
        val passwordField = inputFieldAfter(passwordLabel, "Password")
        passwordField.click()
        passwordField.text = password
        return this
    }

    fun enterConfirmPassword(password: String?): TeamCreationPage {
        val confirmPasswordField = inputFieldAfter(confirmPasswordLabel, "Confirm password")
        confirmPasswordField.click()
        confirmPasswordField.text = password
        return this
    }

    private fun inputFieldAfter(label: UiSelectorParams, text: String): UiObject2 {
        val labelElement = UiWaitUtils.waitElement(label, timeout = 15.seconds)
        val children = labelElement.parent.children
        return children[children.indexOfFirst { it.text == text } + 1]
            .findObject(By.clazz("android.widget.EditText"))
    }

    fun selectFirstOrganizationSizeOption(): TeamCreationPage {
        UiWaitUtils.waitElement(organizationSizeDropdown).click()
        UiWaitUtils.waitElement(firstOrganizationSizeOption).click()
        return this
    }

    fun checkIAcceptTermsAndConditions(): TeamCreationPage {
        checkBoxAt(0)
        return this
    }

    fun checkIAgreeToShareAnonymousUsageData(): TeamCreationPage {
        checkBoxAt(1)
        return this
    }

    fun scrollToContinueButton(): TeamCreationPage {
        UiScrollable(UiSelector().scrollable(true)).apply {
            setAsVerticalList()
            scrollForward()
        }
        return this
    }

    @Suppress("MagicNumber")
    fun scrollDownALittle(): TeamCreationPage {
        device.swipe(
            device.displayWidth / 2,
            (device.displayHeight * 0.45).toInt(),
            device.displayWidth / 2,
            (device.displayHeight * 0.55).toInt(),
            10
        )
        return this
    }

    fun clickContinueButton(): TeamCreationPage {
        val button = UiWaitUtils.waitElement(continueButton)
        assertTrue("Continue button is not enabled", button.isEnabled)
        button.click()
        return this
    }

    fun assertYouHaveGotMailPageVisible(): TeamCreationPage {
        val heading = UiWaitUtils.waitElement(youHaveGotMailHeading, timeout = 30.seconds)
        assertTrue("You've got mail page is not visible", !heading.visibleBounds.isEmpty)
        return this
    }

    fun enterVerificationCode(code: String): TeamCreationPage {
        val codeFields = device.findObjects(By.clazz("android.widget.EditText"))
        val codeField = codeFields[0]
        codeField.click()
        code.forEach { digit ->
            device.pressKeyCode(KeyEvent.KEYCODE_0 + digit.digitToInt())
        }
        return this
    }

    fun assertTeamCreatedPageVisible(): TeamCreationPage {
        val heading = UiWaitUtils.waitElement(teamCreatedHeading, timeout = 30.seconds)
        assertTrue("Team created page is not visible", !heading.visibleBounds.isEmpty)
        return this
    }

    fun closeTeamCreatedPage(): TeamCreationPage {
        device.findObject(closeButton).click()
        return this
    }

    private fun checkBoxAt(index: Int) {
        val checkbox = device.findObjects(By.clazz("android.widget.CheckBox")).getOrNull(index)
            ?: throw AssertionError("Checkbox at index $index not found on Create a team page")
        if (!checkbox.isChecked) {
            checkbox.click()
        }
    }
}
