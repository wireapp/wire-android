package com.wire.android.model

import com.wire.kalium.logic.CoreFailure

/**
 * Wrapper for use case responses with additional [Loading] state for UI purposes
 */
sealed class TriState<out T : Any> {
    data class Success<out T : Any>(val value: T) : TriState<T>()
    data class Error(val coreFailure: CoreFailure) : TriState<Nothing>()
    object Loading : TriState<Nothing>()
}
