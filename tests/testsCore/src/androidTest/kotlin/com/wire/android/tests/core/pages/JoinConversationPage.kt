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

data class JoinConversationPage(private val device: UiDevice) {

    private val joinConversationTitle = UiSelectorParams(text = "Join conversation?")
    private val joinConversationMessage = UiSelectorParams(textContains = "You have been invited to a conversation.")
    private val passwordField = UiSelectorParams(resourceId = "remove device password field")
    private val invalidPasswordError = UiSelectorParams(text = "Invalid password")
    private val joinButton = UiSelectorParams(text = "Join")
    private val cancelButton = UiSelectorParams(text = "Cancel")
    private val cannotJoinTitle = UiSelectorParams(text = "Unable to join conversation")
    private val okButton = UiSelectorParams(text = "OK")

    fun assertJoinConversationAlertVisible(conversationName: String): JoinConversationPage {
        val title = UiWaitUtils.waitElement(joinConversationTitle)
        assertTrue("Join conversation alert is not visible.", !title.visibleBounds.isEmpty)
        UiWaitUtils.waitElement(joinConversationMessage)
        UiWaitUtils.waitElement(UiSelectorParams(textContains = conversationName))
        return this
    }

    fun enterPassword(password: String): JoinConversationPage {
        val field = UiWaitUtils.waitElement(passwordField)
        field.click()
        field.text = password
        return this
    }

    fun tapJoinButton(): JoinConversationPage {
        UiWaitUtils.waitElement(joinButton).click()
        return this
    }

    fun tapCancelButton(): JoinConversationPage {
        UiWaitUtils.waitElement(cancelButton).click()
        return this
    }

    fun assertCannotJoinAlertVisible(message: String): JoinConversationPage {
        val title = UiWaitUtils.waitElement(cannotJoinTitle)
        assertTrue("Cannot-join conversation alert is not visible.", !title.visibleBounds.isEmpty)
        UiWaitUtils.waitElement(UiSelectorParams(text = message))
        return this
    }

    fun assertInvalidPasswordErrorVisible(): JoinConversationPage {
        UiWaitUtils.waitElement(invalidPasswordError)
        return this
    }

    fun tapOkButton(): JoinConversationPage {
        UiWaitUtils.waitElement(okButton).click()
        return this
    }
}
