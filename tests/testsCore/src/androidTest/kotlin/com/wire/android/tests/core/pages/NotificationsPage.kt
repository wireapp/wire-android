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

import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import uiautomatorutils.UiWaitUtils.waitUntilElementGone

class NotificationsPage(private val device: UiDevice) {
    fun waitUntilNotificationPopUpGone(timeoutMillis: Long = 10_000L) {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val replyButton = By.text("Reply")
        // Wait 1 second before checking for the notification pop-up
        SystemClock.sleep(1000L)
        // If the Reply button is found, wait until it's gone
        if (device.hasObject(replyButton)) {
            val disappeared = device.wait(Until.gone(replyButton), timeoutMillis)
            if (!disappeared) {
                throw AssertionError("Notification pop-up with 'Reply' did not disappear within $timeoutMillis ms")
            }
        }
    }

}
