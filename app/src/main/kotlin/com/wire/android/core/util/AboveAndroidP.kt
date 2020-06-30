package com.wire.android.core.util

import android.os.Build

data class AboveAndroidP(val isAboveAndroidP: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
