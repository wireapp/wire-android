package com.wire.android.core.accessibility

import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Build
import android.view.accessibility.AccessibilityManager
import com.wire.android.core.util.CompatibilityManager

class AccessibilityConfig(private val compatibilityManager: CompatibilityManager) {
    fun headingVersionCompatible() = compatibilityManager.isCompatibleWith(Build.VERSION_CODES.P)
}

class AccessibilityManager(private val am: AccessibilityManager) {

    fun isTalkbackEnabled() = am.isEnabled &&
            am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN)
                .isNotEmpty()

}
