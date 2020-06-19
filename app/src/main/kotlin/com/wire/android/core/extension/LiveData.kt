package com.wire.android.core.extension

import androidx.lifecycle.MutableLiveData
import com.wire.android.core.functional.Either

fun <L, R> MutableLiveData<Either<L, R>>.setFailure(failure: L) {
    this.value = Either.Left(failure)
}

fun <L, R> MutableLiveData<Either<L, R>>.setSuccess(success: R) {
    this.value = Either.Right(success)
}

fun <L> MutableLiveData<Either<L, Unit>>.setSuccess() = setSuccess(Unit)

