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
import androidx.test.uiautomator.UiDevice

object KeyboardUtils {
    private const val keyboardSettleDelayMs = 300L

    fun closeKeyboardIfOpened() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        if (isKeyboardVisible(device)) {
            device.pressBack()
            device.waitForIdle()
            SystemClock.sleep(keyboardSettleDelayMs)
        }
    }

    private fun isKeyboardVisible(device: UiDevice): Boolean {
        val dump = runCatching {
            device.executeShellCommand("dumpsys input_method")
        }.getOrDefault("")

        return dump.contains("mInputShown=true") ||
            dump.contains("isInputViewShown=true") ||
            dump.contains("imeVisible=true")
    }
}
