package com.wire.android.util.keyboard

import android.R
import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.view.Window
import androidx.compose.runtime.Stable
import kotlin.math.roundToInt


class KeyboardInsetsProvider constructor(
    applicationContext: Context,
    activity: Activity,
    private val onSizeChanged: (Int, Int) -> Unit
) : ViewTreeObserver.OnGlobalLayoutListener {

    private val rootWindow: Window = activity.window
    private val rootView: View = rootWindow.decorView.findViewById(R.id.content)

    private val deviceDensity = applicationContext.resources.displayMetrics.density

    private var screenMaxHeight = 0
    private var screenMaxWidth = 0

    val keyBoardSize: KeyboardSize = KeyboardSize()

    init {
        rootView.viewTreeObserver.addOnGlobalLayoutListener(this)
    }

    override fun onGlobalLayout() {
        val rectWindowVisibleDisplayFrame = Rect()
        val rootDecorView: View = rootWindow.decorView

        rootDecorView.getWindowVisibleDisplayFrame(rectWindowVisibleDisplayFrame)

        if (rectWindowVisibleDisplayFrame.bottom > screenMaxHeight) {
            screenMaxHeight = rectWindowVisibleDisplayFrame.bottom
        }

        val keyboardHeight: Int = screenMaxHeight - rectWindowVisibleDisplayFrame.bottom

        onSizeChanged((keyboardHeight / deviceDensity).roundToInt(), (keyboardHeight / deviceDensity).roundToInt())

        Log.d("TEST", "keyboard height: : ${(keyboardHeight / deviceDensity).roundToInt()}")
    }

}


@Stable
data class KeyboardSize(
    var height: Int = 0,
    var width: Int = 0
)
