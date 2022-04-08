package com.wire.android.util.keyboard

import android.R
import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.view.Window
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt

class KeyboardInsetsProvider constructor(
    applicationContext: Context,
    activity: Activity
) : ViewTreeObserver.OnGlobalLayoutListener {

    private val rootWindow: Window = activity.window
    private val rootDecorView: View = rootWindow.decorView
    private val rootView: View = rootDecorView.findViewById(R.id.content)

    private val rectWindowVisibleDisplayFrame = Rect()

    private val deviceDensity = applicationContext.resources.displayMetrics.density

    private var screenMaxHeight = 0

    val keyBoardSize: KeyboardSize = KeyboardSize()

    init {
        rootView.viewTreeObserver.addOnGlobalLayoutListener(this)
    }

    override fun onGlobalLayout() {
        rootDecorView.getWindowVisibleDisplayFrame(rectWindowVisibleDisplayFrame)

        if (rectWindowVisibleDisplayFrame.bottom > screenMaxHeight) {
            screenMaxHeight = rectWindowVisibleDisplayFrame.bottom
        }

        keyBoardSize.height = ((screenMaxHeight - rectWindowVisibleDisplayFrame.bottom) / deviceDensity).roundToInt()

        Log.d("TEST","keyboard size ${keyBoardSize.height}")
    }

}

val LocalKeyboardSize = compositionLocalOf { KeyboardSize() }

class KeyboardSize {
    var height by mutableStateOf(0)
}
