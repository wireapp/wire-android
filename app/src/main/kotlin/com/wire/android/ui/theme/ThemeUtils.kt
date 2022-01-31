package com.wire.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

data class ThemeDependent<T>(
    val light: T,
    val dark: T
) {
    val current: T
        @Composable get() = if (isSystemInDarkTheme()) dark else light
}

// https://developer.android.com/guide/topics/large-screens/support-different-screen-sizes#TaskUseSWQuali
data class ScreenSizeDependent<T>(
    val compactPhone: T, //sw320dp
    val defaultPhone: T, //sw480dp
    val tablet7: T,      //sw600dp
    val tablet10: T      //sw840dp
) {
    val current: T
        @Composable get() = LocalConfiguration.current.smallestScreenWidthDp.let { swDp ->
            when {
                swDp >= 840 -> tablet10
                swDp >= 600 -> tablet7
                swDp >= 480 -> defaultPhone
                else -> compactPhone
            }
        }
}
