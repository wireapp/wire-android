package com.wire.android.core.events.adapter

import com.tinder.scarlet.Stream
import com.tinder.scarlet.StreamAdapter
import com.tinder.scarlet.utils.getRawType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.SharingStarted
import java.lang.reflect.Type

class FlowStreamAdapter<T>(private val externalScope: CoroutineScope) : StreamAdapter<T, SharedFlow<T>> {
    override fun adapt(stream: Stream<T>) = callbackFlow<T> {
        stream.start(object : Stream.Observer<T> {
            override fun onComplete() {
                close()
            }

            override fun onError(throwable: Throwable) {
                close(cause = throwable)
            }

            override fun onNext(data: T) {
                if (!isClosedForSend) trySend(data)
            }
        })
        awaitClose {}
    }.shareIn(externalScope, replay = 1, started = SharingStarted.WhileSubscribed())

    class Factory(private val externalScope: CoroutineScope) : StreamAdapter.Factory {
        override fun create(type: Type): StreamAdapter<Any, Any> {
            return when (type.getRawType()) {
                SharedFlow::class.java -> FlowStreamAdapter(externalScope)
                else -> throw IllegalArgumentException()
            }
        }
    }
}
