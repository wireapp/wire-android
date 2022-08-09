package com.wire.android.model

import com.wire.kalium.logic.CoreFailure

sealed class TriState<out T : Any> {
    data class Success<out T : Any>(val value: T) : TriState<T>()
    data class Error(val coreFailure: CoreFailure) : TriState<Nothing>()
    object Loading : TriState<Nothing>()
}
