package com.wire.android.core.accessibility

import android.annotation.SuppressLint
import android.view.View
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.wire.android.core.util.AboveAndroidP

@SuppressLint("NewApi")
fun View.headingForAccessibility(
        isHeading: Boolean,
        aboveAndroidP: AboveAndroidP = AboveAndroidP()
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
