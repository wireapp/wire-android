package com.wire.android.util


import android.os.Build

object DeviceLabel {
    val label = "Wire AR: ${Build.MANUFACTURER} ${Build.MODEL} Android Build:${Build.VERSION.SDK_INT}"
}
