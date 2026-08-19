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
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils

data class CreateNewGroupPage(private val device: UiDevice) {
    private val createNewGroupHeading = UiSelectorParams(text = "New Group")
    private val groupNameInputFieldDescription = UiSelectorParams(description = "Type group name")
    private val continueButton = UiSelectorParams(text = "Continue")
    private val allowGuestsSection = UiSelectorParams(text = "Allow guests")
    private val createGroupButton = UiSelectorParams(text = "Create Group")

    fun assertCreateNewGroupDetailsPageVisible(): CreateNewGroupPage {
        UiWaitUtils.waitElement(createNewGroupHeading)
        return this
    }

    fun enterNewGroupName(groupName: String): CreateNewGroupPage {
        UiWaitUtils.waitElement(groupNameInputFieldDescription).parent.apply {
            click()
            text = groupName
        }
        return this
    }

    fun tapContinueButtonOnGroupDetailsPage(): CreateNewGroupPage {
        UiWaitUtils.waitElement(continueButton).click()
        return this
    }

    fun assertCreateNewGroupSettingsPageVisible(): CreateNewGroupPage {
        UiWaitUtils.waitElement(allowGuestsSection)
        return this
    }

    fun tapCreateGroupButtonOnGroupSettingsPage(): CreateNewGroupPage {
        UiWaitUtils.waitElement(createGroupButton).click()
        return this
    }
}
