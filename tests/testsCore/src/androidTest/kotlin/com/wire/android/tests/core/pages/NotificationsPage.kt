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
import org.junit.Assert
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class NotificationsPage(private val device: UiDevice) {
    private val incomingCallNotification = UiSelectorParams(textMatches = ".*(Calling|calling|Incoming call).*")

    fun waitUntilNotificationPopUpGone(timeout: Duration = 10.seconds) {
        val replyButton = By.text("Reply")
        val appearedWithinProbeWindow = UiWaitUtils.retryUntilTimeout(
            timeout = 1.seconds,
            pollingInterval = UiWaitUtils.POLLING_FAST
        ) {
            device.hasObject(replyButton)
        }
        if (appearedWithinProbeWindow) {
            UiWaitUtils.waitUntilGoneOrThrow(
                selector = replyButton,
                timeout = timeout,
                errorMessage = "Notification pop-up with 'Reply' did not disappear within ${timeout.inWholeMilliseconds} ms"
            )
        }
    }

    fun openNotificationCenter(): NotificationsPage {
        Assert.assertTrue("Notification center did not open.", device.openNotification())
        return this
    }

    fun iSeeOneOnOneIncomingCallNotification(userName: String): NotificationsPage {
        UiWaitUtils.waitElement(
            UiSelectorParams(textContains = userName),
            timeout = UiWaitUtils.MEDIUM_TIMEOUT
        )
        UiWaitUtils.waitElement(
            incomingCallNotification,
            timeout = UiWaitUtils.MEDIUM_TIMEOUT
        )
        return this
    }

    fun iSeeIncomingGroupCallNotification(groupName: String): NotificationsPage {
        UiWaitUtils.waitElement(
            UiSelectorParams(textContains = groupName),
            timeout = UiWaitUtils.MEDIUM_TIMEOUT
        )
        UiWaitUtils.waitElement(
            incomingCallNotification,
            timeout = UiWaitUtils.MEDIUM_TIMEOUT
        )
        return this
    }

    fun iOpenCallNotificationToBringCallToForeground(): NotificationsPage {
        Assert.assertTrue("Notification center did not open.", device.openNotification())
        UiWaitUtils.waitElement(
            incomingCallNotification,
            timeout = UiWaitUtils.MEDIUM_TIMEOUT
        ).click()
        return this
    }

    fun assertMessageNotVisibleInNotificationCenter(
        message: String,
        timeout: Duration = UiWaitUtils.SHORT_TIMEOUT
    ): NotificationsPage {
        val messageAppeared = UiWaitUtils.retryUntilTimeout(
            timeout = timeout,
            pollingInterval = UiWaitUtils.POLLING_DEFAULT
        ) {
            device.hasObject(By.text(message))
        }
        Assert.assertFalse("Message '$message' is visible in notification center.", messageAppeared)
        return this
    }
}
