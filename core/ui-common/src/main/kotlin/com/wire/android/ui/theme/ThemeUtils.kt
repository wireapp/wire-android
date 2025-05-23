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

package com.wire.android.ui.theme

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

data class ThemeDependent<T>(
    val light: T,
    val dark: T
) {
    val currentTheme: T
        @Composable get() = if (isSystemInDarkTheme()) dark else light
}

data class OrientationDependent<T>(
    val portrait: T,
    val landscape: T
) {
    val currentOrientation: T
        @Composable get() = when (LocalConfiguration.current.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> landscape
            else -> portrait
        }
}

// https://developer.android.com/guide/topics/large-screens/support-different-screen-sizes#TaskUseSWQuali
data class ScreenSizeDependent<T>(
    val compactPhone: T, //sw320dp
    val defaultPhone: T, //sw480dp
    val tablet7: T,      //sw600dp
    val tablet10: T      //sw840dp
) {
    val currentScreenSize: T
        @Composable get() = LocalConfiguration.current.smallestScreenWidthDp.let { swDp ->
            when {
                swDp >= 840 -> tablet10
                swDp >= 600 -> tablet7
                swDp >= 480 -> defaultPhone
                else -> compactPhone
            }
        }
}

val isTablet
    @Composable get() = LocalConfiguration.current.smallestScreenWidthDp >= 600
