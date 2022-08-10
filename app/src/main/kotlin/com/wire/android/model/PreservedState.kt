package com.wire.android.model

import com.wire.kalium.logic.CoreFailure

sealed class PreservedState<out T : Any> {
    abstract val state: T

    data class State<out T : Any>(override val state: T) : PreservedState<T>()
    data class Error<out T : Any>(override val state: T, val coreFailure: CoreFailure) : PreservedState<T>()
    data class Loading<out T : Any>(override val state: T) : PreservedState<T>()
}

fun <T : Any> PreservedState<T>.toLoading(): PreservedState<T> = PreservedState.Loading(state)
fun <T : Any> PreservedState<T>.toError(coreFailure: CoreFailure): PreservedState<T> =
    PreservedState.Error(state, coreFailure)

fun <T : Any> PreservedState<T>.toState(): PreservedState<T> = PreservedState.State(state)
