package com.wire.android.ui.home.newconversation.contacts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.NavigationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(private val navigationManager: NavigationManager) : ViewModel() {

    val contactsState by mutableStateOf(
        ContactsState(
            contacts = listOf(
                Contact(
                    id = 1,
                    "This is first contact"
                ), Contact(
                    id = 2,
                    "test2"
                ), Contact(
                    id = 3,
                    "test3"
                ),
                Contact(
                    id = 4,
                    "test1"
                ), Contact(
                    id = 5,
                    "test2"
                ), Contact(
                    id = 6,
                    "test3"
                ),
                Contact(
                    id = 7,
                    "test1"
                ), Contact(
                    id = 8,
                    "test2"
                ), Contact(
                    id = 9,
                    "test3"
                ),
                Contact(
                    id = 10,
                    "test1"
                ), Contact(
                    id = 11,
                    "test2"
                ), Contact(
                    id = 12,
                    "test3"
                ),
                Contact(
                    id = 13,
                    "test1"
                ), Contact(
                    id = 14,
                    "test2"
                ), Contact(
                    id = 15,
                    "This is last contact"
                )
            )
        )
    )

    fun close() {
        viewModelScope.launch {
            navigationManager.navigateBack()
        }
    }

}
