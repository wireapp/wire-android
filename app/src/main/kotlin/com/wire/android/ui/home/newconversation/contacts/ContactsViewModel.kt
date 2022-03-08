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
                    "This is first contact"
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
