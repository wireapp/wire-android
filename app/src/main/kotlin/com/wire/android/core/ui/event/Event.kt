package com.wire.android.core.ui.event

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.wire.android.core.functional.Either

typealias Event<T> = LiveData<OneTimeEvent<T>>

class MutableEvent<T> : MutableLiveData<OneTimeEvent<T>>() {
    fun event(value: T) = setValue(OneTimeEvent(value))
    fun postEvent(value: T) = postValue(OneTimeEvent(value))
}

fun <L, R> MutableEvent<Either<L, R>>.failureEvent(failure: L) = event(Either.Left(failure))

fun <L, R> MutableEvent<Either<L, R>>.successEvent(success: R) = event(Either.Right(success))

fun <L> MutableEvent<Either<L, Unit>>.successEvent() = successEvent(Unit)

