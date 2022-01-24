package com.wire.android

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WireApplication : Application() {

    override fun onCreate() {
        super.onCreate()
    }

}
