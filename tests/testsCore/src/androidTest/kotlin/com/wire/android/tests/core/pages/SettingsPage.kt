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
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import junit.framework.TestCase.assertFalse
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import kotlin.test.DefaultAsserter.assertTrue

data class SettingsPage(private val device: UiDevice) {

    private val privacySettingsButton = UiSelectorParams(text = "Privacy Settings")
    private val debugSettingsButton = UiSelectorParams(text = "Debug Settings")
    private val analyticsInitializedLabel = UiSelectorParams(text = "Analytics Initialized")
    private val enableLoggingText = UiSelector().text("Enable Logging")

    private val lockWithPasscodeText = UiSelector().text("Lock with passcode")
    private val appLockPassCode = UiSelectorParams(text = "Set a passcode")

    private val accountDetails = UiSelectorParams(text = "Account Details")
    private val toggle = UiSelector().className("android.view.View")

    private val analyticsTrackingLabel = UiSelector().text("Analytics Tracking Identifier")
    private val anonymousUsageDataText = UiSelector().text("Send anonymous usage data")

    private val setAppLockInfoText = UiSelectorParams(
        textContains = "The app will lock itself after 1 minute of inactivity"
    )

    private val resetPasswordButton = UiSelectorParams(text = "Reset Password")
    private val passcodeField = UiSelectorParams(resourceId = "password")

    private val displayedEmail = UiSelectorParams(textContains = "@wire.engineering")

    private val displayedDomain = UiSelectorParams(textContains = "staging.zinfra")

    private val emailVerificationNotification = UiSelectorParams(
        textContains = "A verification email has been sent to your email"
    )

    private val editTextClass = By.clazz("android.widget.EditText")
    private val toggleOn = UiSelector()
        .className("android.view.View")
        .clickable(true)
        .checked(true)

    private val toggleOff = UiSelector()
        .className("android.view.View")
        .clickable(true)
        .checked(false)

    private val saveButton = UiSelectorParams(text = "Save")
    fun assertSendAnonymousUsageDataToggleIsOn(): SettingsPage {
        val container = device.findObject(
            UiSelector().className("android.view.View").childSelector(anonymousUsageDataText)
        )
        val toggle = container.getFromParent(UiSelector().text("ON"))
        assertTrue("'Send anonymous usage data' label is not visible", !toggle.visibleBounds.isEmpty)
        return this
    }

    fun clickBackButtonOnSettingsPage() {
        device.pressBack()
    }

    fun assertAnalyticsInitializedIsSetToTrue(): SettingsPage {
        val label = UiWaitUtils.waitElement(analyticsInitializedLabel)
        val parent = label.parent
        val value = parent?.children?.find { it.text == "true" }
        assertTrue("'Analytics Initialized' is not set to true", value != null && value.visibleBounds.width() > 0)
        return this
    }
    fun clickBackButtonOnPrivacySettingsPage() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.pressBack()
    }
    fun clickPrivacySettingsButtonOnSettingsPage(): SettingsPage {
        UiWaitUtils.waitElement(privacySettingsButton).click()
        return this
    }
    fun clickDebugSettingsButton(): SettingsPage {
        UiWaitUtils.waitElement(debugSettingsButton).click()
        return this
    }
    fun assertAnalyticsTrackingIdentifierIsDispayed(): SettingsPage {
        val container = device.findObject(
            UiSelector().className("android.view.View").childSelector(analyticsTrackingLabel)
        )
        val identifierView = container.getFromParent(
            UiSelector().className("android.widget.TextView").instance(1)
        )
        val value = identifierView.text
        assertTrue("Analytics tracking ID is missing or blank", value.isNotBlank())
        return this
    }

    fun tapEnableLoggingToggle(): SettingsPage {
        val label = device.findObject(enableLoggingText)
        val toggle = label.getFromParent(toggle)
        toggle.click()
        return this
    }

    fun assertLoggingToggleIsOff(): SettingsPage {
        val toggle = device.findObject(toggleOff)
        assertFalse("Toggle should be OFF", toggle.isChecked)
        return this
    }

    fun assertLoggingToggleIsOn(): SettingsPage {
        val toggle = device.findObject(toggleOn)
        assertTrue("Toggle should be ON", toggle.isChecked)
        return this
    }

    fun assertLockWithPasswordToggleIsOff(): SettingsPage {
        val toggle = device.findObject(toggleOff)
        assertFalse("Lock with passcode toggle should be OFF", toggle.isChecked)
        return this
    }

    fun turnOnLockWithPasscodeToggle(): SettingsPage {
        val label = device.findObject(lockWithPasscodeText)
        val toggle = label.getFromParent(toggle)
        toggle.click()
        return this
    }

    fun assertAppLockDescriptionText(): SettingsPage {
        val appLockInfo = UiWaitUtils.waitElement(setAppLockInfoText)
        Assert.assertTrue("Username help text is not visible", !appLockInfo.visibleBounds.isEmpty)
        return this
    }

    fun enterPasscode(passcode: String): SettingsPage {
        val parent = UiWaitUtils.waitElement(passcodeField)
        val codeInputField = parent.findObject(editTextClass)
        codeInputField.click()
        codeInputField.text = passcode
        return this
    }

    fun tapSetPasscodeButton(): SettingsPage {
        val passcodeButton = UiWaitUtils.waitElement(appLockPassCode)
        passcodeButton.click()
        return this
    }

    fun assertLockWithPasswordToggleIsOn(): SettingsPage {
        val toggle = device.findObject(toggleOn)
        assertTrue("Lock with passcode toggle should be ON", toggle.isChecked)
        return this
    }

    fun tapAccountDetailsButton(): SettingsPage {
        val accountDetailsButton = UiWaitUtils.waitElement(accountDetails)
        accountDetailsButton.click()
        return this
    }

    fun verifyDisplayedEmailAddress(expectedEmail: String): SettingsPage {
        val emailElement = UiWaitUtils.waitElement(displayedEmail)
        val actualEmail = emailElement.text
        assertThat("Displayed email does not match expected", actualEmail, `is`(expectedEmail))
        return this
    }

    fun verifyDisplayedDomain(expectedDomain: String): SettingsPage {
        val domainElement = UiWaitUtils.waitElement(displayedDomain)
        val actualDomain = domainElement.text
        assertThat("Displayed domain does not match expected", actualDomain, `is`(expectedDomain))
        return this
    }

    fun clickDisplayedEmailAddress(): SettingsPage {
        val emailElement = UiWaitUtils.waitElement(displayedEmail)
        emailElement.click()
        return this
    }

    fun changeToNewEmailAddress(newEmail: String): SettingsPage {
        val emailElement = UiWaitUtils.waitElement(displayedEmail)
        emailElement.text = "" // Clear the input field
        emailElement.text = newEmail
        return this
    }

    fun clickSaveButton(): SettingsPage {
        val button = UiWaitUtils.waitElement(saveButton)
        button.click()
        return this
    }

    fun assertNotificationWithNewEmail(expectedEmail: String): SettingsPage {
        val expectedText = "A verification email has been sent to your email $expectedEmail"

        val emailNotificationTextView = UiWaitUtils.waitElement(emailVerificationNotification)
        val actualText = emailNotificationTextView.text ?: ""

        assertTrue(
            "Expected verification message to contain: $expectedText\nBut got: $actualText",
            actualText.contains(expectedText)
        )

        return this
    }

    fun assertResetPasswordButtonIsDisplayed(): SettingsPage {
        val resetPasswordButton = UiWaitUtils.waitElement(resetPasswordButton)
        Assert.assertTrue("Reset password button is not visible", !resetPasswordButton.visibleBounds.isEmpty)
        return this
    }

    fun tapResetPasswordButton(): SettingsPage {
        UiWaitUtils.waitElement(resetPasswordButton).click()
        return this
    }

    fun clickEmailVerificationLink(deepLinkUrl: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(deepLinkUrl)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun assertEmailVerifiedMessageVisibleOnChrome(timeoutMillis: Long = 15_000): SettingsPage {
        val emailVerifiedText = device.wait(
            Until.findObject(By.textContains("Email verified")),
            timeoutMillis
        )

        if (emailVerifiedText == null) {
            throw AssertionError("Email Verified text not found in Chrome after 15 seconds.")
        }
        return this
    }

    fun assertDisplayedEmailAddressIsNewEmail(expectedEmail: String): SettingsPage {
        val emailElement = UiWaitUtils.waitElement(displayedEmail)
        val actualEmail = emailElement.text
        assertThat("Displayed email does not match expected", actualEmail, `is`(expectedEmail))
        return this
    }

    fun waitUntilNewEmailIsVisible(expectedEmail: String): SettingsPage {
        UiWaitUtils.waitElement(UiSelectorParams(text = expectedEmail))
        return this
    }

    fun assertChromeUrlIsDisplayed(expectedUrl: String): SettingsPage {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        // Wait and find the URL element in the address bar by partial text
        val urlElement = device.wait(
            Until.findObject(By.textContains(expectedUrl)),
            5_000
        )
        assertTrue("Expected URL '$expectedUrl' was not found in Chrome", urlElement != null)
        return this
    }
}
