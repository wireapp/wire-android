package com.wire.android

import android.app.Application
import com.wire.android.core.di.Injector

class WireApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Injector.start(this)
    }
}