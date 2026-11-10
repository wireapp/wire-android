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

import android.graphics.Point
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import com.wire.android.tests.support.UiAutomatorSetup
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import java.util.regex.Pattern
import kotlin.time.Duration.Companion.milliseconds

data class DocumentsUIPage(private val device: UiDevice) {
    private val sendButton = UiSelectorParams(text = "Send")
    private val addButton = UiSelectorParams(textContains = "Add")
    private val doneButton = UiSelectorParams(textContains = "Done")
    private val imagePreview = UiSelectorParams(className = "android.widget.ImageView")
    private val mediaGrid = UiSelectorParams(description = "Media grid")
    private val androidMediaGrid = UiSelectorParams(className = "android.widget.GridView")
    private val downloadsOption = UiSelectorParams(textContains = "Download")
    private val showRootsButton = UiSelectorParams(description = "Show roots")

    fun openDownloadsInFilesApp(): DocumentsUIPage {
        device.executeShellCommand(
            "am start -f 0x10008000 -a android.intent.action.VIEW " +
                "-d content://com.android.providers.downloads.documents/root/downloads -t vnd.android.document/root"
        )
        UiWaitUtils.waitElement(UiSelectorParams(description = "Search"))
        UiWaitUtils.findElementOrNull(UiSelectorParams(description = "List view"))?.click()
        return this
    }

    fun shareFileFromFilesApp(fileName: String): DocumentsUIPage {
        UiWaitUtils.waitElement(UiSelectorParams(description = "Search")).click()
        UiWaitUtils.waitElement(
            UiSelectorParams(resourceId = "com.google.android.documentsui:id/search_src_text")
        ).text = fileName.substringBeforeLast('.')
        device.pressEnter()
        val file = UiWaitUtils.waitElement(UiSelectorParams(text = fileName))
        file.longClick()
        UiWaitUtils.waitElement(UiSelectorParams(resourceId = "com.google.android.documentsui:id/action_menu_share")).click()
        return this
    }

    @Suppress("MagicNumber")
    fun selectWireFromAndroidShareSheet(): DocumentsUIPage {
        val packageManager = InstrumentationRegistry.getInstrumentation().context.packageManager
        val application = packageManager.getApplicationInfo(UiAutomatorSetup.appPackage, 0)
        val appName = packageManager.getApplicationLabel(application).toString()
        if (UiWaitUtils.findElementOrNull(UiSelectorParams(text = appName)) == null) {
            val handle = UiWaitUtils.waitElement(UiSelectorParams(resourceId = "android:id/drag"))
            handle.drag(Point(handle.visibleCenter.x, device.displayHeight / 4))
        }
        UiWaitUtils.waitElement(UiSelectorParams(text = appName), timeout = UiWaitUtils.SHORT_WAIT).click()
        return this
    }

    fun iSeeQrCodeImage(fileName: String = "my-test-qr.png"): DocumentsUIPage {
        val qrCodeImage = UiSelectorParams(text = fileName)

        // Picker may open in Recent folder; switch to Downloads so the generated QR file is visible.
        if (UiWaitUtils.findElementOrNull(qrCodeImage) == null) {
            if (!clickWithRetry(downloadsOption)) {
                clickWithRetry(showRootsButton)
                clickWithRetry(downloadsOption)
            }
        }

        try {
            UiWaitUtils.waitElement(qrCodeImage)
        } catch (e: AssertionError) {
            throw AssertionError("QR code '$fileName' is not visible", e)
        }
        return this
    }

    private fun clickWithRetry(selector: UiSelectorParams, attempts: Int = 3): Boolean {
        repeat(attempts) {
            try {
                UiWaitUtils.waitElement(selector, timeout = 1500.milliseconds).click()
                return true
            } catch (_: StaleObjectException) {
                // Retry with a fresh node.
            } catch (_: AssertionError) {
                // Selector not present in current picker pane.
            }
        }
        return false
    }

    fun iOpenDisplayedQrCodeImage(fileName: String = "my-test-qr.png"): DocumentsUIPage {
        val qrCodeImage = UiSelectorParams(text = fileName)
        UiWaitUtils.waitElement(qrCodeImage).click()
        return this
    }

    fun selectFileInDocumentsUI(fileName: String): DocumentsUIPage {
        iSeeQrCodeImage(fileName)
        iOpenDisplayedQrCodeImage(fileName)
        return this
    }

    fun selectMostRecentImageInPhotoPicker(): DocumentsUIPage {
        val grid = UiWaitUtils.waitAnyVisible(listOf(mediaGrid, androidMediaGrid))
            ?: throw AssertionError("Media grid is not visible in the photo picker.")
        val image = grid.findObject(By.desc(Pattern.compile("Photo taken on.*")))
            ?: throw AssertionError("No image was visible in the photo picker.")
        if (image.isClickable) image.click() else image.parent.click()
        return this
    }

    fun tapAddOrDoneButtonIfVisible(): DocumentsUIPage {
        UiWaitUtils.waitAnyVisible(
            selectors = listOf(addButton, doneButton),
            timeout = UiWaitUtils.SHORT_WAIT
        )?.click()
        return this
    }

    fun assertImagePreviewPageVisible(): DocumentsUIPage {
        UiWaitUtils.waitElement(imagePreview)
        UiWaitUtils.waitElement(sendButton)
        return this
    }

    fun assertFilePreviewPageVisible(fileName: String): DocumentsUIPage {
        UiWaitUtils.waitElement(UiSelectorParams(text = fileName))
        UiWaitUtils.waitElement(sendButton)
        return this
    }

    fun iTapSendButtonOnPreviewImage(): DocumentsUIPage {
        UiWaitUtils.waitElement(sendButton).click()
        return this
    }
}
