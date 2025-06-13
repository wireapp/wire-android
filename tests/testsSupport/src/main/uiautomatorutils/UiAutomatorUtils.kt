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
package uiautomatorutils

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2

/**
 * ✔️ Waits until the element exists
 * ✔️ Confirms it's visibly rendered on screen
 * ✔️ Works for both interactive (buttons) and passive (labels) elements without extra parameters
 */

object UiAutomatorUtils {
    private const val TIMEOUT_IN_MILLISECONDS = 10000L

    fun waitElement(
        text: String? = null,
        textContains: String? = null,
        resourceId: String? = null,
        className: String? = null,
        description: String? = null,
        timeout: Long = TIMEOUT_IN_MILLISECONDS
    ): UiObject2 {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // Start with an empty selector
        var selector: BySelector? = null

        if (text != null) selector = (selector ?: By.text(text)) else selector = selector
        if (textContains != null) selector =
            (selector ?: By.textContains(textContains)) else selector = selector
        if (resourceId != null) selector = (selector?.res(resourceId) ?: By.res(resourceId))
        if (className != null) selector = (selector?.clazz(className) ?: By.clazz(className))
        if (description != null) selector = (selector?.desc(description) ?: By.desc(description))

        requireNotNull(selector) { "At least one selector must be provided" }

        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeout) {
            println("Waiting for element: $selector")
            val obj = device.findObject(selector)
            if (obj != null && !obj.visibleBounds.isEmpty) {
                return obj
            }
            Thread.sleep(250)
        }

        throw AssertionError("Element not found or not visible with selector: " +
                listOfNotNull(
                    text?.let { "text='$it'" },
                    textContains?.let { "textContains='$it'" },
                    resourceId?.let { "resourceId='$it'" },
                    className?.let { "className='$it'" },
                    description?.let { "description='$it'" }
                ).joinToString(", ")
        )
    }
}





