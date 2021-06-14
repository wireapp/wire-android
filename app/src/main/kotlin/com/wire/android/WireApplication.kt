package com.wire.android

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.wire.android.core.di.Injector
import com.wire.android.core.storage.cache.CacheGateway
import org.koin.android.ext.android.inject

class WireApplication : Application(), LifecycleObserver {
    private val cacheGateway by inject<CacheGateway>()

    override fun onCreate() {
        super.onCreate()
        Injector.start(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        cacheGateway.save(IS_IN_BACKGROUND, true)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        cacheGateway.save(IS_IN_BACKGROUND, false)
    }

    companion object {
        const val IS_IN_BACKGROUND = "app_is_in_background"
    }
}
