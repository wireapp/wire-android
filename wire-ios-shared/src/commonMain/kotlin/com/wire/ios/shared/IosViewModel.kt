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
package com.wire.ios.shared

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Swift-facing cancellation token returned by observation APIs.
 *
 * The iOS adapter should keep the returned instance for as long as it wants to receive
 * callbacks and call [close] when the subscription is no longer needed. Calling [close]
 * multiple times is safe.
 */
interface IosCloseable {
    fun close()
}

/**
 * Swift-facing ViewModel bridge.
 *
 * The UI observes [state], handles one-shot [effects], sends user actions with [sendIntent],
 * and calls [close] when the Swift owner is deallocated.
 */
class IosViewModel<State : Any, Effect : Any, Intent : Any>(
    /**
     * Reactive UI state stream.
     *
     * Compose can collect this directly. Swift should usually prefer a typed screen wrapper
     * and its [IosObservableViewModel.currentState] / [IosObservableViewModel.observeState]
     * APIs instead of working with Kotlin Flow types directly.
     */
    val state: StateFlow<State>,
    /**
     * One-shot UI effects stream, such as navigation, opening URLs, or transient messages.
     *
     * Effects are not part of the durable screen state. Swift should usually subscribe through
     * [IosObservableViewModel.observeEffect].
     */
    val effects: Flow<Effect>,
    private val onIntent: (Intent) -> Unit,
    private val onClose: () -> Unit = {},
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var isClosed = false

    /**
     * Synchronous snapshot of the latest state.
     *
     * Swift adapters should use this to initialize their `@Published` state before subscribing
     * to [observeState].
     */
    val currentState: State
        get() = state.value

    /**
     * Observes state updates until the returned [IosCloseable] is closed or this ViewModel is closed.
     *
     * The observer receives the current [StateFlow] value first, then subsequent updates.
     */
    fun observeState(observer: (State) -> Unit): IosCloseable =
        observe(state, observer)

    /**
     * Observes one-shot effects until the returned [IosCloseable] is closed or this ViewModel is closed.
     *
     * Effects should be handled once by the platform UI layer and not stored as durable state.
     */
    fun observeEffect(observer: (Effect) -> Unit): IosCloseable =
        observe(effects, observer)

    /**
     * Sends a user or platform action to the shared ViewModel.
     *
     * Keep platform-specific payloads out of [Intent]; use platform gateways or effect callbacks
     * when the action requires iOS or Android APIs.
     */
    fun sendIntent(intent: Intent) {
        onIntent(intent)
    }

    /**
     * Releases resources owned by this bridge.
     *
     * Swift adapters should call this from `deinit` or their explicit close path. Calling it
     * multiple times is safe.
     */
    fun close() {
        if (isClosed) return
        isClosed = true
        scope.close()
        onClose()
    }

    private fun <T : Any> observe(
        flow: Flow<T>,
        observer: (T) -> Unit,
    ): IosCloseable {
        val job = scope.launch {
            flow.collect(observer)
        }
        return job.asIosCloseable()
    }
}

/**
 * Typed Swift-facing ViewModel contract implemented by per-screen wrappers.
 *
 * The generic [IosViewModel] remains useful internally and for Compose/KMP tests, while screen
 * wrappers expose concrete State/Effect/Intent types to SwiftUI without generic boilerplate.
 */
interface IosObservableViewModel<State : Any, Effect : Any, Intent : Any> {
    /**
     * Latest non-null screen state snapshot.
     */
    val currentState: State

    /**
     * Subscribes to state changes and returns a token that cancels only this subscription.
     */
    fun observeState(observer: (State) -> Unit): IosCloseable

    /**
     * Subscribes to one-shot effects and returns a token that cancels only this subscription.
     */
    fun observeEffect(observer: (Effect) -> Unit): IosCloseable

    /**
     * Sends a UI intent to the shared ViewModel.
     */
    fun sendIntent(intent: Intent)

    /**
     * Closes the ViewModel bridge and all subscriptions owned by it.
     */
    fun close()
}

private fun CoroutineScope.close() {
    coroutineContext[Job]?.cancel()
}

private fun Job.asIosCloseable(): IosCloseable =
    object : IosCloseable {
        override fun close() {
            cancel()
        }
    }
