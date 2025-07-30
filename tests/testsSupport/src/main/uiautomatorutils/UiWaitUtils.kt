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

import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.UiSelector

private const val TIMEOUT_IN_MILLISECONDS = 10000L

data class UiSelectorParams(
    val text: String? = null,
    val textContains: String? = null,
    val resourceId: String? = null,
    val className: String? = null,
    val description: String? = null,
    val timeout: Long = TIMEOUT_IN_MILLISECONDS
)

/**
 * ✔️ Waits until the element exists
 * ✔️ Confirms it's visibly rendered on screen
 * ✔️ Works for both interactive (buttons) and passive (labels) elements without extra parameters
 */

object UiWaitUtils {

    private fun buildSelector(params: UiSelectorParams): BySelector {
        var selector: BySelector? = when {
            params.text != null -> By.text(params.text)
            params.textContains != null -> By.textContains(params.textContains)
            else -> null
        }

        params.resourceId?.let {
            selector = selector?.res(it) ?: By.res(it)
        }
        params.className?.let {
            selector = selector?.clazz(it) ?: By.clazz(it)
        }
        params.description?.let {
            selector = selector?.desc(it) ?: By.desc(it)
        }
        return requireNotNull(selector) { "At least one selector must be provided" }
    }

    fun UiSelectorParams.toBySelector(): BySelector {
        return UiWaitUtils.buildSelector(this)
    }

    fun findElementOrNull(selector: UiSelectorParams): UiObject2? {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        return try {
            device.findObject(selector.toBySelector())
        } catch (e: Exception) {
            null
        }
    }

    @Suppress("MagicNumber")
    fun waitElement(
        params: UiSelectorParams,
        timeoutMillis: Long = 10_000,
        pollingInterval: Long = 250
    ): UiObject2 {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val selector = buildSelector(params)
        val deadline = SystemClock.uptimeMillis() + timeoutMillis

        while (SystemClock.uptimeMillis() < deadline) {
            try {
                val obj = device.findObject(selector)
                if (obj != null) {
                    try {
                        if (!obj.visibleBounds.isEmpty) return obj
                    } catch (e: StaleObjectException) {
                        // Ignore and retry
                    }
                }
            } catch (e: StaleObjectException) {
                // Ignore and retry
            }

            SystemClock.sleep(pollingInterval)
        }

        throw AssertionError(
            "Element not found or not visible with selector: " +
                    listOfNotNull(
                        params.text?.let { "text='$it'" },
                        params.textContains?.let { "textContains='$it'" },
                        params.resourceId?.let { "resourceId='$it'" },
                        params.className?.let { "className='$it'" },
                        params.description?.let { "description='$it'" }
                    ).joinToString(", ")
        )
    }


    fun waitUntilElementGone(
        device: UiDevice,
        selector: UiSelector,
        timeoutMillis: Long = 30_000,
        pollingInterval: Long = 500
    ) {
        val deadline = SystemClock.uptimeMillis() + timeoutMillis

        while (SystemClock.uptimeMillis() < deadline) {
            val element = device.findObject(selector)
            if (!element.exists()) {
                return
            }

            SystemClock.sleep(pollingInterval)
        }

        throw AssertionError("Element matching selector [$selector] did not disappear within timeout.")
    }

    object WaitUtils {
        fun waitFor(seconds: Int, startPinging: () -> Unit = {}, stopPinging: () -> Unit = {}) {
            if (seconds > 20) {
                startPinging()
            }
            Thread.sleep(seconds * 1000L)

            if (seconds > 20) {
                stopPinging()
            }
        }
    }
}
