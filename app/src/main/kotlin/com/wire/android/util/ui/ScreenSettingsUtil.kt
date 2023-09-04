/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.util.ui

import android.app.Activity
import android.os.Build
import android.view.WindowManager
import androidx.navigation.NavDestination
import com.wire.android.navigation.style.ScreenMode
import com.wire.android.navigation.style.ScreenModeStyle
import com.wire.android.navigation.toDestination

fun Activity.updateScreenSettings(navDestination: NavDestination) {
    val screenMode = (navDestination.toDestination()?.style as? ScreenModeStyle)?.screenMode() ?: ScreenMode.NONE
    updateScreenSettings(screenMode)
}

private fun Activity.updateScreenSettings(screenMode: ScreenMode?) {
    when (screenMode) {
        ScreenMode.WAKE_UP -> wakeUpDevice()
        ScreenMode.KEEP_ON -> addScreenOnFlags()
        else -> removeScreenOnFlags()
    }
}

private fun Activity.wakeUpDevice() {

    addScreenOnFlags()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        setShowWhenLocked(true)
        setTurnScreenOn(true)
    } else {
        window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
    }
}

private fun Activity.addScreenOnFlags() {
    window.addFlags(
        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
    )
}

private fun Activity.removeScreenOnFlags() {
    window.clearFlags(
        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        setShowWhenLocked(false)
        setTurnScreenOn(false)
    } else {
        window.clearFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
    }
}
