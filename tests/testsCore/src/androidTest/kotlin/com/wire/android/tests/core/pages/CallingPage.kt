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
import androidx.test.uiautomator.UiObjectNotFoundException
import kotlinx.coroutines.runBlocking
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils

data class CallingPage(private val device: UiDevice) {


    private val hangUpCallButton = UiSelectorParams(description = "Hang up call")

    private val minimiseCallButton = UiSelectorParams(description = "Drop down arrow")

    private val restoreCallButton = UiSelectorParams(text = "RETURN TO CALL")

    fun iSeeOngoingGroupCall(): CallingPage {

        try {
            UiWaitUtils.waitElement(hangUpCallButton)
        } catch (e: AssertionError) {
            throw AssertionError("Ongoing call not displayed", e)
        }
        return this
    }

    fun iMinimiseOngoingCall(): CallingPage {
        UiWaitUtils.waitElement(minimiseCallButton).click()
        return this
    }

    fun iRestoreOngoingCall(): CallingPage {
        UiWaitUtils.waitElement(restoreCallButton).click()
        return this
    }
}
