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

import android.graphics.Bitmap
import android.view.KeyEvent
import androidx.test.uiautomator.UiDevice
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils

data class DocumentEditorPage(private val device: UiDevice) {
    fun captureDocumentScreenshot(): Bitmap {
        UiWaitUtils.waitElement(UiSelectorParams(text = "Online Editor"), timeout = UiWaitUtils.VERY_LONG_TIMEOUT)
        val document = UiWaitUtils.waitElement(UiSelectorParams(resourceId = "document-canvas"), timeout = UiWaitUtils.SHORT_TIMEOUT)
        return captureRenderedDocumentScreenshot(document)
    }

    fun assertDocumentMatchesScreenshot(expected: Bitmap): DocumentEditorPage {
        UiWaitUtils.waitElement(UiSelectorParams(text = "Online Editor"), timeout = UiWaitUtils.VERY_LONG_TIMEOUT)
        val document = UiWaitUtils.waitElement(UiSelectorParams(resourceId = "document-canvas"), timeout = UiWaitUtils.SHORT_TIMEOUT)
        assertRenderedDocumentMatchesScreenshot(document, expected)
        return this
    }

    fun replaceDocumentText(text: String): DocumentEditorPage {
        UiWaitUtils.waitElement(UiSelectorParams(description = "Edit document")).click()
        UiWaitUtils.waitElement(UiSelectorParams(resourceId = "bold-button"))
        // Collabora focuses its hidden text input after a tap on the document canvas.
        UiWaitUtils.waitElement(UiSelectorParams(resourceId = "document-canvas"), timeout = UiWaitUtils.SHORT_TIMEOUT).click()
        device.pressKeyCode(KeyEvent.KEYCODE_A, KeyEvent.META_CTRL_ON)
        device.executeShellCommand("input text ${text.replace(" ", "%s")}")
        return this
    }

    fun saveDocument(): DocumentEditorPage {
        device.pressKeyCode(KeyEvent.KEYCODE_S, KeyEvent.META_CTRL_ON)
        return this
    }

    fun closeEditor(): DocumentEditorPage {
        UiWaitUtils.waitElement(UiSelectorParams(description = "Close tab")).click()
        return this
    }
}
