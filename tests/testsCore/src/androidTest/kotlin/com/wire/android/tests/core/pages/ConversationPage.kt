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
import kotlin.test.DefaultAsserter.assertTrue

data class ConversationPage(private val device: UiDevice) {
    fun clickMainMenuButtonOnConversationVeiwPage(): ConversationPage {
        UiWaitUtils.waitElement(UiSelectorParams(description = "Main navigation")).click()
        return this
    }

    fun clickSettingsButtonOnMenuEntry(): ConversationPage {
        UiWaitUtils.waitElement(UiSelectorParams(text = "Settings")).click()
        return this
    }

    fun assertConversationVisible(conversationName: String) {
        val conversation = UiWaitUtils.waitElement(UiSelectorParams(text = conversationName))
        assertTrue("Conversation '$conversationName' is not visible", !conversation.visibleBounds.isEmpty)
    }

}
