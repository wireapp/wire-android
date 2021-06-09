package com.wire.android.core.events

import com.wire.android.core.websocket.usecase.ListenToWebSocketUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class EventsHandler(private val listenToWebSocketUseCase: ListenToWebSocketUseCase) {

    fun <T> subscribe(onEventReceived: (T: Event) -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            listenToWebSocketUseCase.run(Unit).collect {
                onEventReceived(Event.Message(0, it.byteString.toString())) //TODO pass valid value
            }
        }
    }
}
