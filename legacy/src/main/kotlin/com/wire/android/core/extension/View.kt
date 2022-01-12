package com.wire.android.core.extension

import android.annotation.SuppressLint
import android.view.View
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.wire.android.core.accessibility.AccessibilityConfig
import com.wire.android.core.compatibility.Compatibility

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

fun <T : View> T.afterMeasured(action: (T) -> Unit) =
    if (width == 0 || height == 0) {
        addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (width > 0 && height > 0) action(this)
        }
    } else {
        action(this)
    }
