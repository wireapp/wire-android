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
import java.io.IOException
import java.util.regex.Pattern
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val TIMEOUT_DURATION = 10.seconds
private val STABILIZE_TIMEOUT = 3.seconds
private val STABILIZE_POLLING_INTERVAL = 100.milliseconds

data class UiSelectorParams(
    val text: String? = null,
    val textContains: String? = null,
    val textMatches: String? = null,
    val resourceId: String? = null,
    val className: String? = null,
    val description: String? = null,
    val instance: Int? = null,
    val fromParentText: String? = null,
    val timeout: Duration = TIMEOUT_DURATION
)

/**
 * Utility methods for robust UIAutomator synchronization in instrumentation tests.
 *
 * This object centralizes visibility/gone waits, polling retries, and click retries so page objects
 * in `:tests:testsCore` can avoid local wait/sleep loops and share consistent timeout semantics.
 */

@Suppress("TooManyFunctions")
object UiWaitUtils {
    val POLLING_FAST: Duration = 100.milliseconds
    val POLLING_DEFAULT: Duration = 200.milliseconds
    val POLLING_SLOW: Duration = 250.milliseconds
    val DEFAULT_TIMEOUT: Duration = TIMEOUT_DURATION
    val SHORT_TIMEOUT: Duration = 5.seconds
    val MEDIUM_TIMEOUT: Duration = 10.seconds
    val LONG_TIMEOUT: Duration = 15.seconds
    val VERY_LONG_TIMEOUT: Duration = 30.seconds

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
        timeout: Duration,
        pollingInterval: Duration = POLLING_DEFAULT,
        condition: () -> Boolean
    ): Boolean {
        val deadline = SystemClock.uptimeMillis() + timeout.inWholeMilliseconds
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) {
                return true
            }
            SystemClock.sleep(pollingInterval.inWholeMilliseconds)
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
        timeout: Duration = DEFAULT_TIMEOUT,
        errorMessage: String
    ) {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val isVisible = retryUntilTimeout(
            timeout = timeout,
            pollingInterval = POLLING_DEFAULT
        ) {
            runCatching {
                device.wait(Until.hasObject(params.toBySelector()), POLLING_DEFAULT.inWholeMilliseconds)
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
        timeout: Duration = DEFAULT_TIMEOUT,
        pollingInterval: Duration = POLLING_DEFAULT
    ): UiObject2? {
        var found: UiObject2? = null

        val isFound = retryUntilTimeout(
            timeout = timeout,
            pollingInterval = pollingInterval
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
        timeout: Duration = DEFAULT_TIMEOUT,
        pollingInterval: Duration = POLLING_DEFAULT
    ): Boolean {
        return retryUntilTimeout(
            timeout = timeout,
            pollingInterval = pollingInterval
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
        timeout: Duration = VERY_LONG_TIMEOUT,
        errorMessage: String
    ) {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val isGone = device.wait(Until.gone(selector), timeout.inWholeMilliseconds)
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
        timeout: Duration = VERY_LONG_TIMEOUT,
        pollingInterval: Duration = 500.milliseconds,
        errorMessage: String = "Element matching selector [$selector] did not disappear within timeout."
    ) {
        val isGone = retryUntilTimeout(
            timeout = timeout,
            pollingInterval = pollingInterval
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
        timeout: Duration = DEFAULT_TIMEOUT
    ): UiObject2 {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val sel = buildSelector(params)

        // 1) Block until node exists
        if (!device.wait(Until.hasObject(sel), timeout.inWholeMilliseconds)) {
            throw AssertionError("Element not found with selector: ${describe(params)}")
        }

        device.waitForIdle(500)

        // 2) Stabilize: refetch until bounds are stable & usable
        var lastBounds: Rect? = null
        var stableElement: UiObject2? = null

        retryUntilTimeout(
            timeout = STABILIZE_TIMEOUT,
            pollingInterval = STABILIZE_POLLING_INTERVAL
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
        timeout: Duration = VERY_LONG_TIMEOUT,
        pollingInterval: Duration = 500.milliseconds
    ) {
        waitUntilGoneOrThrow(
            device = device,
            selector = selector,
            timeout = timeout,
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
        fun waitFor(wait: Duration, startPinging: () -> Unit = {}, stopPinging: () -> Unit = {}) {
            if (wait > 20.seconds) {
                startPinging()
            }
            Thread.sleep(wait.inWholeMilliseconds)

            if (wait > 20.seconds) {
                stopPinging()
            }
        }
    }

    /**
     * Preferred entrypoint for fixed waits in UI tests.
     *
     * Internally delegates to [WaitUtils.waitFor] to keep backward compatibility with existing logic.
     */
    fun waitFor(wait: Duration, startPinging: () -> Unit = {}, stopPinging: () -> Unit = {}) {
        WaitUtils.waitFor(wait, startPinging, stopPinging)
    }

    /**
     * Fixed millisecond sleep used by callers that already compute millisecond values.
     */
    fun waitFor(wait: Duration) {
        Thread.sleep(wait.inWholeMilliseconds)
    }

    /**
     * Compatibility wrapper for older callers. Uses [waitUntilVisibleOrThrow] internally.
     */
    fun waitUntilVisible(
        params: UiSelectorParams,
        timeout: Duration = DEFAULT_TIMEOUT,
        errorMessage: String
    ) {
        waitUntilVisibleOrThrow(
            params = params,
            timeout = timeout,
            errorMessage = errorMessage
        )
    }

    /**
     * Waits until a toast containing [message] is visible.
     */
    fun waitUntilToastIsDisplayed(
        message: String,
        timeout: Duration = SHORT_TIMEOUT
    ) {
        waitUntilVisible(
            params = UiSelectorParams(textContains = message),
            timeout = timeout,
            errorMessage = "Toast message containing '$message' was not displayed within ${timeout.inWholeMilliseconds}ms."
        )
    }

    /**
     * Waits until a system message containing [message] is visible.
     */
    fun iSeeSystemMessage(
        message: String,
        timeout: Duration = SHORT_TIMEOUT
    ) {
        waitUntilVisible(
            params = UiSelectorParams(textContains = message),
            timeout = timeout,
            errorMessage = "System message containing '$message' was not displayed within ${timeout.inWholeMilliseconds}ms."
        )
    }

    /**
     * Asserts a toast with [text] is emitted while executing [trigger].
     *
     * This uses accessibility events and is useful when UI tree based lookup is not reliable.
     */
    @Suppress("MagicNumber")
    fun assertToastDisplayed(text: String, trigger: () -> Unit, timeout: Duration = SHORT_TIMEOUT) {
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

            while (!toastDisplayed && System.currentTimeMillis() - startTimeMs < timeout.inWholeMilliseconds) {
                Thread.sleep(50)
            }

            assertTrue("Toast with text '$text' not found within ${timeout.inWholeMilliseconds}ms", toastDisplayed)
        } finally {
            uiAutomation.setOnAccessibilityEventListener(null)
        }
    }
}
