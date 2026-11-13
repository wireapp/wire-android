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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils

data class ChromePage(private val device: UiDevice) {

    private val useWithoutAccountLocator = UiSelectorParams(text = "Use without an account")
    private val noThanksLocator = UiSelectorParams(textMatches = "No,? thanks")
    private val declineCookiesLocator = UiSelectorParams(text = "Decline")
    private val networkQualityArticleTitle = UiSelectorParams(textContains = "Check network quality")
    private val sharedDriveArticleTitle = UiSelectorParams(textContains = "Use Drive in conversations")
    private val globalDriveArticleTitle = UiSelectorParams(
        textContains = "Access all files shared across conversations (Drive)"
    )

    fun clickUseWithoutAccount(): ChromePage {
        UiWaitUtils.waitElement(useWithoutAccountLocator).click()
        return this
    }

    fun dismissFirstRunIfVisible(): ChromePage {
        runCatching {
            UiWaitUtils.waitElement(useWithoutAccountLocator, timeout = UiWaitUtils.SHORT_WAIT).click()
        }
        return this
    }

    fun completeFirstRunForOnlineEditorIfShown(): ChromePage {
        val signedOutButton = "com.android.chrome:id/signin_fre_dismiss_button"
        val screen = UiWaitUtils.waitAnyVisible(
            selectors = listOf(
                UiSelectorParams(resourceId = signedOutButton),
                useWithoutAccountLocator,
                UiSelectorParams(text = "Online Editor")
            ),
            timeout = UiWaitUtils.MEDIUM_TIMEOUT
        ) ?: throw AssertionError("Neither the Online Editor nor Chrome's first-run button is visible")
        if (screen.resourceName == signedOutButton || screen.text == "Use without an account") {
            screen.click()
            dismissNotificationsPromptIfVisible()
        }
        return this
    }

    fun dismissNotificationsPromptIfVisible(): ChromePage {
        runCatching {
            UiWaitUtils.waitElement(noThanksLocator, timeout = UiWaitUtils.SHORT_WAIT).click()
        }
        return this
    }

    fun dismissCookieConsentIfVisible(): ChromePage {
        runCatching {
            UiWaitUtils.waitElement(declineCookiesLocator, timeout = UiWaitUtils.SHORT_WAIT).click()
        }
        return this
    }

    fun assertHtmlFileHandlerAvailable(): ChromePage {
        val handlers = runShellCommand(
            "cmd package query-activities --brief -a android.intent.action.VIEW " +
                "-t text/html -d content://test-fixture/external-opening.html"
        )
        assertTrue("Chrome is not available to open HTML files", handlers.contains("com.android.chrome/"))
        return this
    }

    fun selectChromeFromOpenWithDialog(): ChromePage {
        val chromeAddressBar = UiSelectorParams(resourceId = "com.android.chrome:id/url_bar")
        val justOnce = UiSelectorParams(text = "Just once")
        val chromeScreens = listOf(chromeAddressBar, noThanksLocator)
        val chrome = UiWaitUtils.waitAnyVisible(
            selectors = chromeScreens + listOf(
                UiSelectorParams(text = "Chrome"),
                UiSelectorParams(text = "Open with Chrome"),
                justOnce
            ),
            timeout = UiWaitUtils.VERY_LONG_TIMEOUT
        ) ?: throw AssertionError("Chrome is not open or shown in the Open with dialog")
        if (device.currentPackageName == "com.android.chrome") return this
        if (chrome.text == "Just once") {
            chrome.click()
        } else {
            if (chrome.text == "Chrome") chrome.click()
            val choice = UiWaitUtils.waitAnyVisible(
                selectors = chromeScreens + justOnce,
                timeout = UiWaitUtils.STABLE_TIMEOUT
            ) ?: throw AssertionError("Neither Chrome nor Just once is visible after selecting Chrome")
            if (choice.text == "Just once") choice.click()
        }
        return this
    }

    fun assertHtmlFileOpenInChrome(fileName: String, expectedText: String): ChromePage {
        UiWaitUtils.waitElement(
            UiSelectorParams(resourceId = "com.android.chrome:id/url_bar", textContains = fileName),
            timeout = UiWaitUtils.VERY_LONG_TIMEOUT
        )
        UiWaitUtils.waitElement(UiSelectorParams(text = expectedText), timeout = UiWaitUtils.VERY_LONG_TIMEOUT)
        assertEquals("The HTML file is not open in Chrome", "com.android.chrome", device.currentPackageName)
        return this
    }

    fun navigateBack(): ChromePage {
        device.pressBack()
        return this
    }

    fun assertTextFileOpenInChrome(fileName: String, expectedText: String): ChromePage {
        UiWaitUtils.waitElement(
            UiSelectorParams(resourceId = "com.android.chrome:id/url_bar", textContains = fileName),
            timeout = UiWaitUtils.SHORT_TIMEOUT
        )
        UiWaitUtils.waitElement(UiSelectorParams(textContains = expectedText), timeout = UiWaitUtils.SHORT_TIMEOUT)
        assertEquals("The TXT file is not open in Chrome", "com.android.chrome", device.currentPackageName)
        return this
    }

    fun assertNetworkQualitySupportArticleVisible(): ChromePage {
        dismissFirstRunIfVisible()
        dismissNotificationsPromptIfVisible()
        UiWaitUtils.waitUntilVisibleOrThrow(
            params = networkQualityArticleTitle,
            timeout = UiWaitUtils.MEDIUM_TIMEOUT,
            errorMessage = "Network quality support article is not visible in browser."
        )
        return this
    }

    fun assertSharedDriveSupportArticleVisible(): ChromePage {
        dismissFirstRunIfVisible()
        dismissNotificationsPromptIfVisible()
        dismissCookieConsentIfVisible()
        UiWaitUtils.waitUntilVisibleOrThrow(
            params = sharedDriveArticleTitle,
            timeout = UiWaitUtils.MEDIUM_TIMEOUT,
            errorMessage = "Shared Drive support article is not visible in browser."
        )
        return this
    }

    fun assertGlobalDriveSupportArticleVisible(): ChromePage {
        dismissFirstRunIfVisible()
        dismissNotificationsPromptIfVisible()
        dismissCookieConsentIfVisible()
        UiWaitUtils.waitUntilVisibleOrThrow(
            params = globalDriveArticleTitle,
            timeout = UiWaitUtils.MEDIUM_TIMEOUT,
            errorMessage = "Global Drive support article is not visible in browser."
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
}
