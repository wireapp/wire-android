package com.wire.android.core.extension

import android.app.Activity
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment

fun Activity.toast(@StringRes message: Int, length: Int = Toast.LENGTH_SHORT) =
    Toast.makeText(this, message, length).show()

fun Activity.toast(message: String, length: Int = Toast.LENGTH_SHORT) =
    Toast.makeText(this, message, length).show()

fun Fragment.toast(@StringRes message: Int, length: Int = Toast.LENGTH_SHORT) =
    requireActivity().toast(message, length)

fun Fragment.toast(message: String, length: Int = Toast.LENGTH_SHORT) =
    requireActivity().toast(message, length)
