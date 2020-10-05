package com.wire.android.core.accessibility

import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Build
import android.view.accessibility.AccessibilityManager
import com.wire.android.core.compatibility.Compatibility

class AccessibilityConfig(private val compatibility: Compatibility) {
    fun headingVersionCompatible() = compatibility.supportsAndroidVersion(Build.VERSION_CODES.P)
}

class Accessibility(private val am: AccessibilityManager) {

    fun isTalkbackEnabled() = am.isEnabled &&
        am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN)
            .isNotEmpty()

}
