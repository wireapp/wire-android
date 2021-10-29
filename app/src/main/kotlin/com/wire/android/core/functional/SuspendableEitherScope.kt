package com.wire.android.core.functional

import com.wire.android.core.functional.Either.Left
import com.wire.android.core.functional.Either.Right

/**
 * Helper class that extends [Either] to be compatible with suspend functions. The extension methods created in this class
 * are only visible inside a scope that belongs to this class. The scope can be created by [suspending] method.
 *
 * ```
 * suspend fun save() {
 *    ...
 * }
 *
 * suspend fun saveResult() {
 *     either.flatMap {
 *        save()        // compile time error: cannot call suspend function here
 *     }
 * }
 *
 *
 * suspend fun saveResult() {
 *     suspending {             // this: SuspendableEitherScope
 *         either.flatMap {     // resolves "flatMap" in SuspendableEitherScope
 *             save()           // we can now call a suspend function here
 *         }
 *     }
 * }
 * ```
 *
 * @see Either
 */
class SuspendableEitherScope {

    /**
     * @see [Either.fold]
     */
    suspend fun <L, R, T> Either<L, R>.coFold(fnL: suspend (L) -> T?, fnR: suspend (R) -> T?): T? =
        when (this) {
            is Left -> fnL(a)
            is Right -> fnR(b)
        }

    suspend fun <L, R, T> Either<L, R>.flatMap(fn: suspend (R) -> Either<L, T>): Either<L, T> =
        when (this) {
            is Left -> Left(a)
            is Right -> fn(b)
        }

    suspend fun <L, R> Either<L, R>.onFailure(fn: suspend (failure: L) -> Unit): Either<L, R> =
        this.apply { if (this is Left) fn(a) }

    suspend fun <L, R> Either<L, R>.onSuccess(fn: suspend (success: R) -> Unit): Either<L, R> =
        this.apply { if (this is Right) fn(b) }

    suspend fun <L, R, T> Either<L, R>.map(fn: suspend (R) -> (T)): Either<L, T> =
        when (this) {
            is Left -> Left(a)
            is Right -> Right(fn(b))
        }

    /**
     * Folds a list into an Either while it doesn't go Left.
     * Allows for accumulation of value through iterations.
     * @return the final accumulated value if there are NO Left results, or the first Left result otherwise.
     */
    suspend fun <T, L, R> Iterable<T>.foldToEitherWhileRight(
        initialValue: R,
        fn: suspend (item: T, accumulated: R) -> Either<L, R>
    ): Either<L, R> {
        return this.fold<T, Either<L, R>>(Right(initialValue)) { acc, item ->
            acc.flatMap { accumulatedValue -> fn(item, accumulatedValue) }
        }
    }
}

suspend fun <T> suspending(block: suspend SuspendableEitherScope.() -> T): T = SuspendableEitherScope().block()
