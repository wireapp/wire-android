package com.wire.android.ui.home.newconversation

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel

class NewConversationViewModel : ViewModel() {

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
