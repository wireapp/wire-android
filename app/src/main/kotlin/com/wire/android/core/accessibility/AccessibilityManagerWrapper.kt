package com.wire.android.core.accessibility

import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityManager

class AccessibilityManagerWrapper(private val am: AccessibilityManager) {

    fun isTalkbackEnabled() = if (am.isEnabled) {
        am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN).isNotEmpty()
    } else false

}
