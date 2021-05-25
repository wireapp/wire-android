package com.wire.android.core.events

import com.wire.android.core.functional.map
import com.wire.android.core.websocket.usecase.ListenToWebSocketUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch

class EventsHandler(private val listenToWebSocketUseCase: ListenToWebSocketUseCase) {

    @ExperimentalCoroutinesApi
    fun <T> subscribe(onEventReceived: (T : Event) -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                listenToWebSocketUseCase.run(Unit).map {
                    launch {
                        it.consumeEach { message ->
                            onEventReceived(Event.Message(0, message.byteString.toString())) //TODO pass valid value
                        }
                    }
                }
            } catch (ex: java.lang.Exception) {
                onSocketError(ex)
            }
        }

    }

    private fun onSocketError(ex: Throwable) {
        println("Error occurred : ${ex.message}")
    }
}
