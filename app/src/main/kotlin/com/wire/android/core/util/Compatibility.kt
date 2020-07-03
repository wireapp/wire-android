package com.wire.android.core.util

import android.os.Build

class Compatibility {
    fun supportsAndroidVersion(versionCode: Int) = Build.VERSION.SDK_INT >= versionCode
}
