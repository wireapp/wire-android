package com.wire.android.core.extension

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment

fun Fragment.toast(@StringRes message: Int, length: Int = Toast.LENGTH_SHORT) =
    Toast.makeText(requireContext(), message, length).show()

fun Fragment.toast(message: String, length: Int = Toast.LENGTH_SHORT) =
    Toast.makeText(requireContext(), message, length).show()
