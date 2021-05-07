package com.wire.android.core.device

import android.content.Context
import android.provider.Settings

class DeviceNameUseCase(val context: Context) {
    fun run(): String? = Settings.Secure.getString(context.contentResolver, name)

    companion object {
        private const val name = "bluetooth_name"
    }
}
