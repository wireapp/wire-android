package com.wire.android.ui.home.newconversation

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class NewConversationViewModel : ViewModel() {
    fun close() {
        //TODO:close the screen
    }

    val newConversationState = mutableStateOf(
        NewConversationState(
            listOf(
                Contact(
                    "test1"
                ), Contact(
                    "test2"
                ), Contact(
                    "test3"
                ),
                Contact(
                    "test1"
                ), Contact(
                    "test2"
                ), Contact(
                    "test3"
                ),
                Contact(
                    "test1"
                ), Contact(
                    "test2"
                ), Contact(
                    "test3"
                ),
                Contact(
                    "test1"
                ), Contact(
                    "test2"
                ), Contact(
                    "test3"
                ),
                Contact(
                    "test1"
                ), Contact(
                    "test2"
                ), Contact(
                    "test3"
                )
            )
        )
    )

}
