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
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import org.junit.Assert.assertTrue
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils

@Suppress("MagicNumber")
internal fun selectShareTarget(device: UiDevice, receiverLabel: String) {
    // Wait for Android to load the available receiving apps.
    UiWaitUtils.waitAnyVisible(
        selectors = listOf(
            UiSelectorParams(resourceId = "android:id/chooser_header"),
            UiSelectorParams(resourceId = "miui:id/alertTitle", text = "Share")
        ),
        timeout = UiWaitUtils.LONG_TIMEOUT
    ) ?: throw AssertionError("Android's share sheet is not visible for $receiverLabel")
    val receiver = By.text(receiverLabel)
    val visible = UiWaitUtils.retryUntilTimeout(UiWaitUtils.SHORT_TIMEOUT) {
        if (!device.hasObject(receiver)) {
            // Samsung's app row scrolls horizontally; More opens the remaining share targets.
            val samsungApps = device.findObject(By.res("com.android.intentresolver:id/sem_chooser_recycler_ranked_app"))
            if (samsungApps != null) {
                val more = samsungApps.findObject(By.text("More"))
                if (more != null) more.click() else samsungApps.scroll(Direction.RIGHT, 1f)
            } else {
                val miuiApps = device.findObject(By.res("android.miui:id/screen_view"))
                if (miuiApps != null) {
                    // MIUI puts additional share targets on the next horizontal page.
                    miuiApps.swipe(Direction.LEFT, 1f)
                } else {
                    // Android 12's outer contentPanel includes the navigation edge; scroll only its app list.
                    val androidApps = device.findObject(By.res("android:id/resolver_list"))
                    if (androidApps != null) {
                        androidApps.setGestureMargin(androidApps.visibleBounds.height() / 4)
                        androidApps.swipe(Direction.UP, 1f, device.displayWidth)
                    } else {
                        device.findObject(By.scrollable(true))?.scroll(Direction.DOWN, 1f)
                    }
                }
            }
        }
        device.hasObject(receiver)
    }
    assertTrue("$receiverLabel is missing from Android's share sheet", visible)
    device.findObject(receiver).click()
}
