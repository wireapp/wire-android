package com.wire.android.framework.functional

import com.wire.android.core.functional.Either
import org.junit.Assert.fail

infix fun <L, R> Either<L, R>.shouldSucceed(successAssertion: (R) -> Unit) =
    this.fold({ fail("Expected a Right value but got Left") }) { successAssertion(it) }!!

infix fun <L, R> Either<L, R>.shouldFail(failAssertion: (L) -> Unit) =
    this.fold({ failAssertion(it) }) { fail("Expected a Left value but got Right") }!!

//TODO: Refactor this to accept infix
fun <L> Either<L, Unit>.shouldFail() = shouldFail { }
