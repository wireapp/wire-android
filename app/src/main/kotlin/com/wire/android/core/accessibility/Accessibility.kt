package com.wire.android.core.accessibility

import android.os.Build
import android.view.View
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat

data class AboveAndroidP(val isAboveAndroidP: Boolean)

fun View.headingForAccessibility(
        isHeading: Boolean,
        aboveAndroidP: AboveAndroidP = AboveAndroidP(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
) =
    if (aboveAndroidP.isAboveAndroidP) {
        isAccessibilityHeading = isHeading
    } else {
        ViewCompat.setAccessibilityDelegate(this, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.isHeading = isHeading
            }
        })
    }
