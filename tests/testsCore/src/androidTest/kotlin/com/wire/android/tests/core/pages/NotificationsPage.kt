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

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import uiautomatorutils.UiWaitUtils

class NotificationsPage(private val device: UiDevice) {
    fun waitUntilNotificationPopUpGone(timeoutMillis: Long = 10_000L) {
        val replyButton = By.text("Reply")
        val appearedWithinProbeWindow = UiWaitUtils.retryUntilTimeout(timeoutMs = 1_000, pollingIntervalMs = 100) {
            device.hasObject(replyButton)
        }
        if (appearedWithinProbeWindow) {
            UiWaitUtils.waitUntilGoneOrThrow(
                selector = replyButton,
                timeoutMs = timeoutMillis,
                errorMessage = "Notification pop-up with 'Reply' did not disappear within $timeoutMillis ms"
            )
        }
    }
}
