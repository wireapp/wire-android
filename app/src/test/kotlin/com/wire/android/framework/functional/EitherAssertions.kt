package com.wire.android.framework.functional

import com.wire.android.core.functional.Either
import org.assertj.core.api.Assertions.fail

infix fun <L, R> Either<L, R>.shouldSucceed(successAssertion: (R) -> Unit) =
    this.fold({ fail<Unit>("Expected a Right value but got Left") }) { successAssertion(it) }!!

infix fun <L, R> Either<L, R>.shouldFail(failAssertion: (L) -> Unit) =
    this.fold({ failAssertion(it) }) { fail<Unit>("Expected a Left value but got Right") }!!

//TODO: Refactor this to accept infix
fun <L> Either<L, Unit>.shouldFail() = shouldFail { }

//TODO: remove this after migrating to Mockk and Kluent
fun <L, R> Either<L, R>.assertLeft(leftAssertion: (L) -> Unit) =
    this.fold({ leftAssertion(it) }) { fail<Unit>("Expected a Left value but got Right") }!!

fun <L, R> Either<L, R>.assertRight(rightAssertion: (R) -> Unit) =
    this.fold({ fail<Unit>("Expected a Right value but got Left") }) { rightAssertion(it) }!!

fun <L> Either<L, Unit>.assertRight() = assertRight { }
