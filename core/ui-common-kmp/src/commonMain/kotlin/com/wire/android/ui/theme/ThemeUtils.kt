package com.wire.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

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
        @Composable get() = when (platformOrientation()) {
            Orientation.Landscape -> landscape
            Orientation.Portrait -> portrait
        }
}

// https://developer.android.com/guide/topics/large-screens/support-different-screen-sizes#TaskUseSWQuali
// Uses platformSmallestWidthDp() so non-Android platforms can return a safe default.
data class ScreenSizeDependent<T>(
    val compactPhone: T, // sw320dp
    val defaultPhone: T, // sw480dp
    val tablet7: T, // sw600dp
    val tablet10: T // sw840dp
) {
    val currentScreenSize: T
        @Composable get() = platformSmallestWidthDp().let { swDp ->
            when {
                swDp >= 840 -> tablet10
                swDp >= 600 -> tablet7
                swDp >= 480 -> defaultPhone
                else -> compactPhone
            }
        }
}

val isTablet
    @Composable get() = platformSmallestWidthDp() >= 600
