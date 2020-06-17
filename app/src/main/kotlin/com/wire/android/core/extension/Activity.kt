package com.wire.android.core.extension

import android.app.Activity
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodManager.HIDE_IMPLICIT_ONLY
import android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

fun AppCompatActivity.replaceFragment(frameId: Int, fragment: Fragment, addToBackStack: Boolean = true) =
    supportFragmentManager.inTransaction {
        replace(frameId, fragment).also {
            if (addToBackStack) {
                it.addToBackStack(fragment.tag)
            }
        }
    }

fun Activity.showKeyboard() =
    (this.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager).toggleSoftInput(SHOW_IMPLICIT, HIDE_IMPLICIT_ONLY)