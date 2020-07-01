package com.wire.android.core.util

import android.os.Build

class CompatibilityManager {
    fun isCompatibleWith(versionCode: Int) = Build.VERSION.SDK_INT >= versionCode
}
