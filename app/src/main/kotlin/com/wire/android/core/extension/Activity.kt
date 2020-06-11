package com.wire.android.core.extension

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

fun AppCompatActivity.replaceFragment(frameId: Int, fragment: Fragment, addToBackStack: Boolean = true) =
    supportFragmentManager.doTransaction {
        replace(frameId, fragment).apply {
            if (addToBackStack) {
                addToBackStack(fragment.tag)
            }
        }
    }