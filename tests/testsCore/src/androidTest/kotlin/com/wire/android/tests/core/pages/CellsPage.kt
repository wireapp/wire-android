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
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import kotlin.time.Duration

data class CellsPage(private val device: UiDevice) {
    private val sharedDriveTitle = UiSelectorParams(text = "Shared Drive")
    private val cancelLoadingLabel = UiSelectorParams(text = "Tap to cancel loading")
    private val readyToOpenLabel = UiSelectorParams(text = "Ready to open")

    fun assertSharedDriveVisible(timeout: Duration = UiWaitUtils.VERY_LONG_TIMEOUT): CellsPage {
        UiWaitUtils.waitElement(sharedDriveTitle, timeout)
        return this
    }

    fun tapFile(fileName: String, timeout: Duration = UiWaitUtils.VERY_LONG_TIMEOUT): CellsPage {
        if (!UiWaitUtils.clickWhenClickable(UiSelectorParams(text = fileName), timeout)) {
            throw AssertionError("Cells file '$fileName' was not clickable within ${timeout.inWholeMilliseconds}ms.")
        }
        return this
    }

    fun assertDownloadStarted(timeout: Duration = UiWaitUtils.VERY_LONG_TIMEOUT): CellsPage {
        UiWaitUtils.waitElement(cancelLoadingLabel, timeout)
        return this
    }

    fun cancelActiveDownload(): CellsPage {
        UiWaitUtils.waitElement(cancelLoadingLabel).click()
        return this
    }

    fun assertDownloadCancelled(timeout: Duration = UiWaitUtils.VERY_LONG_TIMEOUT): CellsPage {
        UiWaitUtils.waitUntilGoneOrThrow(
            selector = By.text("Tap to cancel loading"),
            timeout = timeout,
            errorMessage = "Cells download remained active after cancellation."
        )
        return this
    }

    fun assertFileReadyToOpen(timeout: Duration): CellsPage {
        UiWaitUtils.waitElement(readyToOpenLabel, timeout)
        return this
    }
}
