package com.wire.android.util.extension

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

suspend fun <A, B> flatMapFromIterable(
    collectionA: Collection<A>,
    block: suspend (A) -> Flow<B>
): Flow<Collection<B>> {
    return flow {
        val result = mutableListOf<B>()
        collectionA.forEach { a ->
            block(a).collect { b -> result.add(b) }
        }
        emit(result)
    }
}

suspend fun <A, B> Flow<Collection<A>>.flatMapIterable(block: suspend (A) -> Flow<B>): Flow<Collection<B>> =
    flatMapLatest { flatMapFromIterable(it, block) }
