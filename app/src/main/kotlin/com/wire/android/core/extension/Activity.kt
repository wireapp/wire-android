package com.wire.android.core.extension

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT

/**
 * Shows the keyboard if there is a currently focused view, does nothing otherwise.
 */
fun Activity.showKeyboard(): Unit {
    currentFocus?.let {
        (it.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(it, SHOW_IMPLICIT)
    } ?: Log.i(this.javaClass.canonicalName, "keyboard was requested but there's no focused view")
}
