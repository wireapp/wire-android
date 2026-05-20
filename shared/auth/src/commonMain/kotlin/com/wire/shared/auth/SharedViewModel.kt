/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.shared.auth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

interface SharedCloseable {
    fun close()
}

/**
 * Platform-neutral shared ViewModel contract.
 *
 * Android can bind [state] and [effects] directly from Compose. Other platform export modules
 * should wrap this type in platform-friendly concrete wrappers instead of duplicating auth logic.
 */
class SharedViewModel<State : Any, Effect : Any, Intent : Any>(
    val state: StateFlow<State>,
    val effects: Flow<Effect>,
    private val onIntent: (Intent) -> Unit,
    private val onClose: () -> Unit = {},
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var isClosed = false

    val currentState: State
        get() = state.value

    fun observeState(observer: (State) -> Unit): SharedCloseable =
        observe(state, observer)

    fun observeEffect(observer: (Effect) -> Unit): SharedCloseable =
        observe(effects, observer)

    fun sendIntent(intent: Intent) {
        onIntent(intent)
    }

    fun close() {
        if (isClosed) return
        isClosed = true
        scope.close()
        onClose()
    }

    private fun <T : Any> observe(
        flow: Flow<T>,
        observer: (T) -> Unit,
    ): SharedCloseable {
        val job = scope.launch {
            flow.collect(observer)
        }
        return job.asSharedCloseable()
    }
}

interface SharedObservableViewModel<State : Any, Effect : Any, Intent : Any> {
    val currentState: State

    fun observeState(observer: (State) -> Unit): SharedCloseable

    fun observeEffect(observer: (Effect) -> Unit): SharedCloseable

    fun sendIntent(intent: Intent)

    fun close()
}

private fun CoroutineScope.close() {
    coroutineContext[Job]?.cancel()
}

private fun Job.asSharedCloseable(): SharedCloseable =
    object : SharedCloseable {
        override fun close() {
            cancel()
        }
    }
