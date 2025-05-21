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
package utils


import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiSelector


object UiAutomatorUtils {
    private const val TIMEOUT_IN_MILLISECONDS = 5000L

    fun waitForObject(device: UiDevice, selector: UiSelector): UiObject {
        val obj = device.findObject(selector)
        if (!obj.waitForExists(TIMEOUT_IN_MILLISECONDS)) {
            throw AssertionError("Element with selector $selector not found within $TIMEOUT_IN_MILLISECONDS ms")
        }
        return obj
    }
}
