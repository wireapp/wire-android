package com.wire.android.ui.common

import android.os.Build
import android.view.ViewTreeObserver
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.wire.android.appLogger

@OptIn(ExperimentalLayoutApi::class)
object KeyboardHelper {

    @Composable
    fun isKeyboardVisible(): Boolean {
        val elo =  ViewCompat.getRootWindowInsets(LocalView.current)?.isVisible(WindowInsetsCompat.Type.ime()) ?: false
        appLogger.d("KBX isKeyboardVisible $elo")
        return elo
    }

    @Composable
    fun getCalculatedKeyboardHeight(): Dp =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // ime covers also the navigation bar so we need to subtract navigation bars height
            val calculatedImeHeight = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
            val calculatedNavBarHeight = WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues().calculateBottomPadding()
            appLogger.d("KBX calculatedImeHeight $calculatedImeHeight calculatedNavBarHeight $calculatedNavBarHeight")

            calculatedImeHeight + calculatedNavBarHeight
        } else {
            val calculatedImeHeight = ViewCompat.getRootWindowInsets(LocalView.current)
                ?.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.ime())?.bottom ?: 0
            val calculatedNavBarHeight = ViewCompat.getRootWindowInsets(LocalView.current)
                ?.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
            with(LocalDensity.current) { (calculatedImeHeight - calculatedNavBarHeight).toDp() }
        }

    @Composable
    fun KeyboardHeight(): Dp {
        val rootView = LocalView.current
        val insets = ViewCompat.getRootWindowInsets(rootView)
        val keyboardHeightPx = insets?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
        appLogger.d("KBX keyboard ${keyboardHeightPx}")
        return with(LocalDensity.current) { keyboardHeightPx.toDp() }
    }

    @Composable
    fun IsKeyboardVisible(): Boolean {
        val rootView = LocalView.current
        val isKeyboardVisible = remember { mutableStateOf(false) }

        DisposableEffect(Unit) {
            val listener = ViewTreeObserver.OnGlobalLayoutListener {
                val rect = android.graphics.Rect()
                rootView.getWindowVisibleDisplayFrame(rect)
                val screenHeight = rootView.height
                val keypadHeightPx = screenHeight - rect.bottom
                appLogger.d("KBX screenHeight $screenHeight keypadHeightPx $keypadHeightPx")


                // Consider keyboard is visible if it takes up more than 20% of the screen height
                isKeyboardVisible.value = keypadHeightPx > screenHeight * 0.2
            }

            rootView.viewTreeObserver.addOnGlobalLayoutListener(listener)

            onDispose {
                rootView.viewTreeObserver.removeOnGlobalLayoutListener(listener)
            }
        }
        appLogger.d("KBX isKeyboardVisible ${isKeyboardVisible.value}")

        return isKeyboardVisible.value
    }

}
