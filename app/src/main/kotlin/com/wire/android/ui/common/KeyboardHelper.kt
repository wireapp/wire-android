package com.wire.android.ui.common

import android.os.Build
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

@OptIn(ExperimentalLayoutApi::class)
object KeyboardHelper {

    @Composable
    fun isKeyboardVisible(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowInsets.isImeVisible
        } else {
            ViewCompat.getRootWindowInsets(LocalView.current)?.isVisible(WindowInsetsCompat.Type.ime()) ?: false
        }

    @Composable
    fun getCalculatedKeyboardHeight(): Dp =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // ime covers also the navigation bar so we need to subtract navigation bars height
            val calculatedImeHeight = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
            val calculatedNavBarHeight = WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues().calculateBottomPadding()
            calculatedImeHeight - calculatedNavBarHeight
        } else {
            val calculatedImeHeight = ViewCompat.getRootWindowInsets(LocalView.current)
                ?.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.ime())?.bottom ?: 0
            val calculatedNavBarHeight = ViewCompat.getRootWindowInsets(LocalView.current)
                ?.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
            with(LocalDensity.current) { (calculatedImeHeight - calculatedNavBarHeight).toDp() }
        }
}
