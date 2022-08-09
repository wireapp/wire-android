package com.wire.android.model

sealed class DialogState<out T : Any> {
    data class Visible<out T : Any>(val value: T) : DialogState<T>()
    object Hidden : DialogState<Nothing>()
}
