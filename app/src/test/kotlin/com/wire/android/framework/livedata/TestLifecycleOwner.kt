package com.wire.android.framework.livedata

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

class TestLifecycleOwner(activeByDefault: Boolean = true) : LifecycleOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)

    init {
        if (activeByDefault) start()
    }

    override fun getLifecycle(): Lifecycle = lifecycleRegistry

    fun start() {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    fun destroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }
}