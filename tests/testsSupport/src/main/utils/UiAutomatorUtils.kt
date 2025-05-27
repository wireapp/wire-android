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
    /**
     * Waits until the element exists (useful for non-click interactions or assertions).
     */
    fun waitForObject(device: UiDevice, selector: UiSelector, timeout: Long = TIMEOUT_IN_MILLISECONDS): UiObject {
        val obj = device.findObject(selector)
        if (!obj.waitForExists(timeout)) {
            throw AssertionError("Element with selector $selector not found within $timeout ms")
        }
        return obj
    }

    /**
     * Waits for any one of the given selectors to match an object on screen.
     *
     * @param device The UiDevice instance
     * @param selectors A list of UiSelector objects
     * @param timeout Timeout in milliseconds (default is 5000)
     * @return The first matched and enabled UiObject
     * @throws AssertionError if no selectors match within the timeout
     */
    fun waitForAnyObject(
        device: UiDevice,
        selectors: List<UiSelector>,
        timeout: Long = 5000L
    ): UiObject {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeout) {
            for (selector in selectors) {
                val obj = device.findObject(selector)
                if (obj.exists() && obj.isEnabled) {
                    return obj
                }
            }
            Thread.sleep(250)
        }
        throw AssertionError("None of the selectors matched any object within $timeout ms")
    }
}

