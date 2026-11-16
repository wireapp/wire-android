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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils

data class ImportMediaPage(private val device: UiDevice) {
    private fun conversationRow(name: String): UiObject2 {
        UiWaitUtils.waitElement(UiSelectorParams(text = name))
        return device.findObject(By.checkable(true).hasDescendant(By.text(name)))
            ?: throw AssertionError("Conversation '$name' is not visible in the import chooser")
    }

    fun assertViewerConversationDisabled(name: String): ImportMediaPage {
        val row = conversationRow(name)
        val selection = row.findObject(By.clazz("android.widget.RadioButton"))
        assertFalse("Viewer Conversation '$name' selection control is enabled", selection.isEnabled)
        assertFalse("Viewer Conversation '$name' is selected", row.isChecked)
        return this
    }

    fun tapConversation(name: String): ImportMediaPage {
        conversationRow(name).click()
        return this
    }

    fun assertEditorConversationSelected(name: String): ImportMediaPage {
        val row = conversationRow(name)
        assertTrue("Editor Conversation '$name' is disabled", row.isEnabled)
        assertTrue("Editor Conversation '$name' is not selected", row.isChecked)
        return this
    }

    fun assertSendButtonDisabled(): ImportMediaPage {
        val send = UiWaitUtils.waitElement(UiSelectorParams(text = "Send")).parent
        assertFalse("Send is enabled without an allowed Conversation", send.isEnabled)
        return this
    }

    fun assertSendButtonEnabled(): ImportMediaPage {
        val send = UiWaitUtils.waitElement(UiSelectorParams(text = "Send")).parent
        assertTrue("Send is disabled for the selected Editor Conversation", send.isEnabled)
        return this
    }

    fun tapSendButton(): ImportMediaPage {
        UiWaitUtils.waitElement(UiSelectorParams(text = "Send")).parent.click()
        return this
    }
}
