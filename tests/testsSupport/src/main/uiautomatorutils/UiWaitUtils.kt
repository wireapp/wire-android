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

import android.graphics.Rect
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import junit.framework.TestCase.assertTrue
import uiautomatorutils.UiWaitUtils.waitUntilGoneOrThrow
import uiautomatorutils.UiWaitUtils.waitUntilVisibleOrThrow
import java.io.IOException
import java.util.regex.Pattern

private const val TIMEOUT_IN_MILLISECONDS = 10000L
private const val DEFAULT_POLLING_INTERVAL_MS = 200L
private const val STABILIZE_TIMEOUT_MS = 3_000L
private const val STABILIZE_POLLING_INTERVAL_MS = 100L

data class UiSelectorParams(
    val text: String? = null,
    val textContains: String? = null,
    val textMatches: String? = null,
    val resourceId: String? = null,
    val className: String? = null,
    val description: String? = null,
    val instance: Int? = null,
    val fromParentText: String? = null,
    val timeout: Long = TIMEOUT_IN_MILLISECONDS
)

/**
 * Utility methods for robust UIAutomator synchronization in instrumentation tests.
 *
 * This object centralizes visibility/gone waits, polling retries, and click retries so page objects
 * in `:tests:testsCore` can avoid local wait/sleep loops and share consistent timeout semantics.
 */

@Suppress("TooManyFunctions")
object UiWaitUtils {

    private fun buildSelector(params: UiSelectorParams): BySelector {
        var selector: BySelector? = when {
            params.text != null -> By.text(params.text)
            params.textMatches != null -> By.text(Pattern.compile(params.textMatches))
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

    /**
     * Converts [UiSelectorParams] into a [BySelector] used by UIAutomator `By.*` based queries.
     */
    fun UiSelectorParams.toBySelector(): BySelector {
        return UiWaitUtils.buildSelector(this)
    }

    /**
     * Finds an element once and returns `null` when it is not currently available.
     *
     * This is intentionally non-throwing and is useful in polling loops or optional element checks.
     */
    fun findElementOrNull(selector: UiSelectorParams): UiObject2? {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        return try {
            device.findObject(selector.toBySelector())
        } catch (e: IOException) {
            null
        }
    }

    /**
     * Repeatedly evaluates [condition] until it returns `true` or [timeoutMs] expires.
     *
     * @return `true` if [condition] succeeded before timeout, otherwise `false`.
     */
    fun retryUntilTimeout(
        timeoutMs: Long,
        pollingIntervalMs: Long = DEFAULT_POLLING_INTERVAL_MS,
        condition: () -> Boolean
    ): Boolean {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) {
                return true
            }
            SystemClock.sleep(pollingIntervalMs)
        }
        return condition()
    }

    /**
     * Waits until an element matching [params] is visible, then returns.
     *
     * Throws [AssertionError] with [errorMessage] when the element does not appear in time.
     */
    fun waitUntilVisibleOrThrow(
        params: UiSelectorParams,
        timeoutMs: Long = TIMEOUT_IN_MILLISECONDS,
        errorMessage: String
    ) {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val isVisible = retryUntilTimeout(
            timeoutMs = timeoutMs,
            pollingIntervalMs = DEFAULT_POLLING_INTERVAL_MS
        ) {
            runCatching {
                device.wait(Until.hasObject(params.toBySelector()), DEFAULT_POLLING_INTERVAL_MS)
            }.getOrDefault(false)
        }

        if (!isVisible) {
            throw AssertionError(errorMessage)
        }
    }

    /**
     * Waits until any selector from [selectors] resolves to a visible element.
     *
     * @return the first visible [UiObject2], or `null` when no selector becomes visible in time.
     */
    fun waitAnyVisible(
        selectors: List<UiSelectorParams>,
        timeoutMs: Long = TIMEOUT_IN_MILLISECONDS,
        pollingIntervalMs: Long = DEFAULT_POLLING_INTERVAL_MS
    ): UiObject2? {
        var found: UiObject2? = null

        val isFound = retryUntilTimeout(
            timeoutMs = timeoutMs,
            pollingIntervalMs = pollingIntervalMs
        ) {
            found = selectors
                .asSequence()
                .mapNotNull(::findElementOrNull)
                .firstOrNull { runCatching { !it.visibleBounds.isEmpty }.getOrDefault(false) }
            found != null
        }

        return if (isFound) found else null
    }

    /**
     * Waits for an element to become visible and enabled, then clicks it.
     *
     * Handles transient `StaleObjectException` by retrying until timeout.
     *
     * @return `true` if the click succeeded within timeout, otherwise `false`.
     */
    fun clickWhenClickable(
        params: UiSelectorParams,
        timeoutMs: Long = TIMEOUT_IN_MILLISECONDS,
        pollingIntervalMs: Long = DEFAULT_POLLING_INTERVAL_MS
    ): Boolean {
        return retryUntilTimeout(
            timeoutMs = timeoutMs,
            pollingIntervalMs = pollingIntervalMs
        ) {
            val element = findElementOrNull(params) ?: return@retryUntilTimeout false
            try {
                if (!element.visibleBounds.isEmpty && element.isEnabled) {
                    element.click()
                    true
                } else {
                    false
                }
            } catch (_: StaleObjectException) {
                false
            }
        }
    }

    /**
     * Waits until an element matched by [selector] disappears from the UI hierarchy.
     *
     * Throws [AssertionError] with [errorMessage] when the element is still present after timeout.
     */
    fun waitUntilGoneOrThrow(
        selector: BySelector,
        timeoutMs: Long = 30_000,
        errorMessage: String
    ) {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val isGone = device.wait(Until.gone(selector), timeoutMs)
        if (!isGone) {
            throw AssertionError(errorMessage)
        }
    }

    /**
     * Waits until an element matched by [UiSelector] disappears.
     *
     * This variant is intended for call sites still using classic `UiSelector`.
     */
    fun waitUntilGoneOrThrow(
        device: UiDevice,
        selector: UiSelector,
        timeoutMillis: Long = 30_000,
        pollingInterval: Long = 500,
        errorMessage: String = "Element matching selector [$selector] did not disappear within timeout."
    ) {
        val isGone = retryUntilTimeout(
            timeoutMs = timeoutMillis,
            pollingIntervalMs = pollingInterval
        ) {
            !device.findObject(selector).exists()
        }
        if (!isGone) {
            throw AssertionError(errorMessage)
        }
    }

    /**
     * Waits for an element to appear and then stabilizes it before returning.
     *
     * Stabilization requires visible, on-screen, enabled bounds repeated across two probes, reducing
     * flaky interactions caused by transient or stale nodes.
     */
    @Suppress("MagicNumber", "NestedBlockDepth", "CyclomaticComplexMethod", "ComplexCondition")
    fun waitElement(
        params: UiSelectorParams,
        timeoutMillis: Long = 10_000
    ): UiObject2 {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val sel = buildSelector(params)

        // 1) Block until node exists
        if (!device.wait(Until.hasObject(sel), timeoutMillis)) {
            throw AssertionError("Element not found with selector: ${describe(params)}")
        }

        device.waitForIdle(500)

        // 2) Stabilize: refetch until bounds are stable & usable
        var lastBounds: Rect? = null
        var stableElement: UiObject2? = null

        retryUntilTimeout(
            timeoutMs = STABILIZE_TIMEOUT_MS,
            pollingIntervalMs = STABILIZE_POLLING_INTERVAL_MS
        ) {
            val obj = try {
                device.findObject(sel)
            } catch (_: StaleObjectException) {
                null
            }
            if (obj != null) {
                try {
                    val b = obj.visibleBounds
                    val onScreen = b.left >= 0 && b.top >= 0 &&
                            b.right <= device.displayWidth && b.bottom <= device.displayHeight
                    val nonZero = b.width() > 0 && b.height() > 0
                    val enabled = obj.isEnabled

                    // Same bounds twice in a row → considered stable
                    if (onScreen && nonZero && enabled && lastBounds != null && lastBounds == b) {
                        stableElement = obj
                        return@retryUntilTimeout true
                    }
                    lastBounds = b
                } catch (_: StaleObjectException) {
                    // re-loop
                }
            }
            false
        }

        if (stableElement != null) {
            return stableElement as UiObject2
        }

        throw AssertionError("Element found but not stable/visible with selector: ${describe(params)}")
    }

    private fun describe(params: UiSelectorParams) = listOfNotNull(
        params.text?.let { "text='$it'" },
        params.textContains?.let { "textContains='$it'" },
        params.textMatches?.let { "textMatches='$it'" },
        params.resourceId?.let { "resourceId='$it'" },
        params.className?.let { "className='$it'" },
        params.description?.let { "description='$it'" }
    ).joinToString(", ")

    /**
     * Compatibility wrapper for existing callers using the old `waitUntilElementGone` API.
     *
     * Internally delegates to [waitUntilGoneOrThrow].
     */
    fun waitUntilElementGone(
        device: UiDevice,
        selector: UiSelector,
        timeoutMillis: Long = 30_000,
        pollingInterval: Long = 500
    ) {
        waitUntilGoneOrThrow(
            device = device,
            selector = selector,
            timeoutMillis = timeoutMillis,
            pollingInterval = pollingInterval
        )
    }

    @Suppress("MagicNumber")
    object WaitUtils {
        /**
         * Legacy fixed sleep helper used by existing test flows.
         *
         * Kept for compatibility in PR-1; call sites migrate in later cleanups.
         */
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

    /**
     * Compatibility wrapper for older callers. Uses [waitUntilVisibleOrThrow] internally.
     */
    fun waitUntilVisible(
        params: UiSelectorParams,
        timeoutMs: Long = TIMEOUT_IN_MILLISECONDS,
        errorMessage: String
    ) {
        waitUntilVisibleOrThrow(
            params = params,
            timeoutMs = timeoutMs,
            errorMessage = errorMessage
        )
    }

    /**
     * Waits until a toast containing [message] is visible.
     */
    fun waitUntilToastIsDisplayed(
        message: String,
        timeoutMs: Long = 5_000
    ) {
        waitUntilVisible(
            params = UiSelectorParams(textContains = message),
            timeoutMs = timeoutMs,
            errorMessage = "Toast message containing '$message' was not displayed within ${timeoutMs}ms."
        )
    }

    /**
     * Waits until a system message containing [message] is visible.
     */
    fun iSeeSystemMessage(
        message: String,
        timeoutMs: Long = 5_000
    ) {
        waitUntilVisible(
            params = UiSelectorParams(textContains = message),
            timeoutMs = timeoutMs,
            errorMessage = "System message containing '$message' was not displayed within ${timeoutMs}ms."
        )
    }

    /**
     * Asserts a toast with [text] is emitted while executing [trigger].
     *
     * This uses accessibility events and is useful when UI tree based lookup is not reliable.
     */
    @Suppress("MagicNumber")
    fun assertToastDisplayed(text: String, trigger: () -> Unit, timeoutMs: Long = 5_000L) {
        var toastDisplayed = false
        val startTimeMs = System.currentTimeMillis()

        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation

        uiAutomation.setOnAccessibilityEventListener { event ->
            if (event.eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
                val className = event.className?.toString().orEmpty()
                val eventText = event.text?.joinToString(" ").orEmpty()

                if (className.contains("android.widget.Toast") && eventText.contains(text, ignoreCase = true)) {
                    toastDisplayed = true
                }
            }
        }

        try {
            // IMPORTANT: trigger AFTER listener is set
            trigger()

            while (!toastDisplayed && System.currentTimeMillis() - startTimeMs < timeoutMs) {
                Thread.sleep(50)
            }

            assertTrue("Toast with text '$text' not found within ${timeoutMs}ms", toastDisplayed)
        } finally {
            uiAutomation.setOnAccessibilityEventListener(null)
        }
    }
}
