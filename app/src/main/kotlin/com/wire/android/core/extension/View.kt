package com.wire.android.core.extension

import android.annotation.SuppressLint
import android.view.View
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.wire.android.core.accessibility.AccessibilityConfig
import com.wire.android.core.util.Compatibility

@SuppressLint("NewApi")
fun View.headingForAccessibility(
    accessibilityConfig: AccessibilityConfig = AccessibilityConfig(Compatibility())
) =
    if (accessibilityConfig.headingVersionCompatible()) {
        isAccessibilityHeading = true
    } else {
        ViewCompat.setAccessibilityDelegate(this, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.isHeading = true
            }
        })
    }
