package com.wire.android.core.config

import android.content.Context
import android.content.res.Configuration
import android.provider.Settings

class DeviceConfig(val context: Context) {

    fun deviceName(): String? = Settings.Secure.getString(context.contentResolver, bluetoothName)

    fun deviceClass(): DeviceClass {
        if ((context.resources.configuration.screenLayout
                    and Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE)
            return Tablet
        return Phone
    }

    companion object {
        private const val bluetoothName = "bluetooth_name"
    }
}
