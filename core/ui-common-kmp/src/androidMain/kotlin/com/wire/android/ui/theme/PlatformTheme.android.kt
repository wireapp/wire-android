package com.wire.android.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
actual fun platformOrientation(): Orientation = when (LocalConfiguration.current.orientation) {
    Configuration.ORIENTATION_LANDSCAPE -> Orientation.Landscape
    else -> Orientation.Portrait
}

@Composable
actual fun platformSmallestWidthDp(): Int = LocalConfiguration.current.smallestScreenWidthDp

@Composable
actual fun UpdateSystemBarIconsAppearance(useDarkIcons: Boolean) {
    val view = LocalView.current
    val activity = view.context.getActivity()
    if (!view.isInEditMode && activity != null) {
        SideEffect {
            WindowCompat.getInsetsController(activity.window, view).isAppearanceLightStatusBars = useDarkIcons
        }
    }
}

private fun Context.getActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}
