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

import androidx.test.uiautomator.UiDevice
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils

data class GroupConversationDetailsPage(private val device: UiDevice) {

    private val showMoreOptionsButton = UiSelectorParams(description = "Open conversation options")

    private val deleteConversationButton = UiSelectorParams(text = "Delete Conversation")

    private val removeGroupButton = UiSelectorParams(text = "Remove")


    fun tapShowMoreOptionsButton() {
        UiWaitUtils.waitElement(showMoreOptionsButton).click()
    }

    fun tapDeleteConversationButton() {
        UiWaitUtils.waitElement(deleteConversationButton).click()
    }

    fun tapRemoveGroupButton() {
        UiWaitUtils.waitElement(removeGroupButton).click()
    }


}
