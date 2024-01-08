/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package com.wire.android.navigation.style

interface ScreenModeStyle {
    fun screenMode(): ScreenMode
}

enum class ScreenMode {
    KEEP_ON, // keep screen on while that NavigationItem is visible (i.e CallScreen)
    WAKE_UP, // wake up the device on navigating to that NavigationItem (i.e IncomingCall)
    NONE // do not wake up and allow device to sleep
}
