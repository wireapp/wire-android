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

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import org.junit.Assert.assertTrue
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils

data class CreateNewGroupPage(private val device: UiDevice) {
    private val createNewGroupHeading = UiSelectorParams(text = "New Group")
    private val createNewChannelHeading = UiSelectorParams(text = "New Channel")
    private val groupNameInputFieldDescription = UiSelectorParams(description = "Type group name")
    private val channelNameInputFieldDescription = UiSelectorParams(description = "Type channel name")
    private val continueButton = UiSelectorParams(text = "Continue")
    private val allowGuestsSection = UiSelectorParams(text = "Allow guests")
    private val createGroupButton = UiSelectorParams(text = "Create Group")
    private val createChannelButton = UiSelectorParams(text = "Create Channel")
    private val sharedDriveOption = UiSelectorParams(text = "Shared Drive")
    private val learnMoreLink = UiSelectorParams(text = "Learn more")

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
        UiWaitUtils.waitElement(createGroupButton)
        return this
    }

    fun tapCreateGroupButtonOnGroupSettingsPage(): CreateNewGroupPage {
        UiWaitUtils.waitElement(createGroupButton).click()
        return this
    }

    fun assertCreateNewChannelDetailsPageVisible(): CreateNewGroupPage {
        UiWaitUtils.waitElement(createNewChannelHeading)
        UiWaitUtils.waitElement(channelNameInputFieldDescription)
        return this
    }

    fun enterNewChannelName(channelName: String): CreateNewGroupPage {
        UiWaitUtils.waitElement(channelNameInputFieldDescription).parent.apply {
            click()
            text = channelName
        }
        return this
    }

    fun tapContinueButtonOnChannelDetailsPage(): CreateNewGroupPage {
        UiWaitUtils.waitElement(continueButton).click()
        return this
    }

    fun assertCreateNewChannelSettingsPageVisible(): CreateNewGroupPage {
        UiWaitUtils.waitElement(allowGuestsSection)
        UiWaitUtils.waitElement(createChannelButton)
        return this
    }

    fun assertSharedDriveIsOffOnCreationSettingsPage(): CreateNewGroupPage {
        val disabled = UiWaitUtils.retryUntilTimeout(UiWaitUtils.VERY_SHORT_TIMEOUT) {
            !sharedDriveToggle().isChecked
        }
        assertTrue("Shared Drive is enabled by default", disabled)
        return this
    }

    fun assertSharedDriveIsOnOnCreationSettingsPage(): CreateNewGroupPage {
        val enabled = UiWaitUtils.retryUntilTimeout(UiWaitUtils.VERY_SHORT_TIMEOUT) {
            sharedDriveToggle().isChecked
        }
        assertTrue("Shared Drive is not enabled", enabled)
        return this
    }

    fun tapSharedDriveToggleOnCreationSettingsPage(): CreateNewGroupPage {
        sharedDriveToggle().click()
        return this
    }

    fun tapLearnMoreLinkInSharedDriveExplanation(): CreateNewGroupPage {
        UiWaitUtils.waitElement(learnMoreLink).click()
        return this
    }

    fun tapCreateChannelButtonOnChannelSettingsPage(): CreateNewGroupPage {
        UiWaitUtils.waitElement(createChannelButton).click()
        return this
    }

    private fun sharedDriveToggle(): UiObject2 {
        val sharedDriveRow = UiWaitUtils.waitElement(
            sharedDriveOption,
            timeout = UiWaitUtils.SHORT_WAIT
        ).parent
        return sharedDriveRow.findObject(By.checkable(true))
            ?: throw AssertionError("Shared Drive toggle is not visible on the conversation creation settings page")
    }
}
