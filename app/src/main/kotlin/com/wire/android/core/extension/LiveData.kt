package com.wire.android.core.extension

import androidx.lifecycle.MutableLiveData
import com.wire.android.core.functional.Either

fun <L, R> MutableLiveData<Either<L, R>>.failure(failure: L) {
    this.value = Either.Left(failure)
}

fun <L, R> MutableLiveData<Either<L, R>>.success(success: R) {
    this.value = Either.Right(success)
}

fun <L> MutableLiveData<Either<L, Unit>>.success() = success(Unit)

