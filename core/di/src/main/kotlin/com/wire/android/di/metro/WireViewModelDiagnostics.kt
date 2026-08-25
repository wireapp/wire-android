/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.di.metro

import androidx.lifecycle.ViewModelStoreOwner

/**
 * Optional diagnostics boundary for the Navigation 3 / Metro ViewModel runtime.
 *
 * The gateway remains independent from the application logger. Debug-capable hosts may install a
 * sink and associate opaque Android owners with their stable Wire owner keys.
 */
interface WireViewModelDiagnosticSink {
    fun ownerAvailable(owner: ViewModelStoreOwner, ownerKey: String)
    fun ownerReleased(owner: ViewModelStoreOwner, ownerKey: String)
    fun ownerCleared(ownerKey: String)
    fun viewModelRequested(owner: ViewModelStoreOwner, viewModelKey: String)
}

object WireViewModelDiagnostics {
    private val lock = Any()

    @Volatile
    private var sink: WireViewModelDiagnosticSink? = null

    /**
     * Installs [diagnosticSink] and returns a handle that only removes that exact installation.
     */
    fun install(diagnosticSink: WireViewModelDiagnosticSink): AutoCloseable {
        synchronized(lock) {
            sink = diagnosticSink
        }
        return AutoCloseable {
            synchronized(lock) {
                if (sink === diagnosticSink) sink = null
            }
        }
    }

    fun ownerAvailable(owner: ViewModelStoreOwner, ownerKey: String) {
        sink?.ownerAvailable(owner, ownerKey)
    }

    fun ownerReleased(owner: ViewModelStoreOwner, ownerKey: String) {
        sink?.ownerReleased(owner, ownerKey)
    }

    fun ownerCleared(ownerKey: String) {
        sink?.ownerCleared(ownerKey)
    }

    @PublishedApi
    internal fun viewModelRequested(owner: ViewModelStoreOwner, viewModelKey: String) {
        sink?.viewModelRequested(owner, viewModelKey)
    }
}
