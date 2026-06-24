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

import androidx.test.uiautomator.UiDevice
import org.junit.Assert.assertTrue
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils

data class CommonAppPage(private val device: UiDevice) {
    private val teamSettingsChangedAlert = UiSelectorParams(textContains = "Team Settings Changed")
    private val okButton = UiSelectorParams(text = "OK")

    private fun teamSettingsChangedAlertSubtext(text: String) = UiSelectorParams(textContains = text)

    fun assertTeamSettingsChangedAlertVisible(): CommonAppPage {
        val alert = UiWaitUtils.waitElement(teamSettingsChangedAlert)
        assertTrue("Team settings changed alert is not visible", !alert.visibleBounds.isEmpty)
        return this
    }

    fun assertTeamSettingsChangedAlertSubtextVisible(text: String): CommonAppPage {
        val subtext = UiWaitUtils.waitElement(teamSettingsChangedAlertSubtext(text))
        assertTrue("Team settings changed alert subtext is not visible", !subtext.visibleBounds.isEmpty)
        return this
    }

    fun tapOkButtonOnAlert(): CommonAppPage {
        UiWaitUtils.waitElement(okButton).click()
        device.waitForIdle()
        return this
    }
}
