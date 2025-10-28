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

package com.wire.android.ui.common

import android.app.Activity
import android.content.pm.ActivityInfo

/**
 * Sets up screen orientation based on device type.
 * - Tablets (smallestScreenWidthDp >= 600dp): Can rotate freely in all orientations
 * - Phones (smallestScreenWidthDp < 600dp): Locked to portrait orientation only
 *
 * This uses the same 600dp threshold that the app uses throughout its UI system
 * for determining tablet vs phone layouts.
 */
fun Activity.setupOrientationForDevice() {
    val isTablet = resources.configuration.smallestScreenWidthDp >= TABLET_MIN_SCREEN_WIDTH_DP
    requestedOrientation = if (isTablet) {
        ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
    } else {
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
}

private const val TABLET_MIN_SCREEN_WIDTH_DP = 600
