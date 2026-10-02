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
import org.junit.Assert.assertFalse
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils

data class MessageDetailsPage(private val device: UiDevice) {
    private val messageDetailsHeading = UiSelectorParams(text = "Message Details")
    private val readReceiptsTab = UiSelectorParams(textContains = "READ RECEIPTS (")

    fun tapReadReceiptsTab(): MessageDetailsPage {
        UiWaitUtils.waitElement(messageDetailsHeading)
        UiWaitUtils.waitElement(readReceiptsTab).click()
        return this
    }

    fun assertReadReceiptsCount(expectedCount: Int): MessageDetailsPage {
        UiWaitUtils.waitElement(UiSelectorParams(text = "READ RECEIPTS ($expectedCount)"))
        return this
    }

    fun assertUserReadMessage(userName: String): MessageDetailsPage {
        UiWaitUtils.waitElement(UiSelectorParams(text = userName))
        return this
    }

    fun assertUserDidNotReadMessage(userName: String): MessageDetailsPage {
        UiWaitUtils.waitElement(messageDetailsHeading)
        val receiptAppeared = UiWaitUtils.retryUntilTimeout(UiWaitUtils.DEFAULT_TIMEOUT) {
            device.hasObject(By.text(userName))
        }
        assertFalse("User '$userName' unexpectedly appears in the read receipts list", receiptAppeared)
        UiWaitUtils.waitElement(readReceiptsTab)
        return this
    }
}
