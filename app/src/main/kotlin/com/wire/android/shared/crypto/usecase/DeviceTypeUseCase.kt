package com.wire.android.shared.crypto.usecase

import android.content.Context
import android.content.res.Configuration

class DeviceTypeUseCase(val context: Context) {
    fun run(): String {
        if (((context.resources.configuration.screenLayout
                    and Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE))
            return TABLET
        return PHONE
    }

    companion object {
        private const val TABLET = "tablet"
        private const val PHONE = "phone"
    }
}
