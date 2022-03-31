package com.wire.android.ui.home.newconversation.contacts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val navigationManager: NavigationManager
) : ViewModel() {

    @Suppress("MagicNumber")
    var contactsState by mutableStateOf(
        ContactsState(
            contacts = buildList {
                add(
                    Contact(
                        id = 1.toString(),
                        "This is first contact"
                    )
                )
                for (i in 2..100) {
                    add(
                        Contact(
                            id = i.toString(),
                            "This is $i contact"
                        )
                    )
                }
                add(
                    Contact(
                        id = 12.toString(),
                        "This is last contact"
                    )
                )
            }
        )
    )

    fun close() {
        viewModelScope.launch {
            navigationManager.navigateBack()
        }
    }

    fun addContactToGroup(contact: Contact) {
        contactsState = contactsState.copy(addToGroupContacts = contactsState.addToGroupContacts + contact)
    }

    fun removeContactFromGroup(contact: Contact) {
        contactsState = contactsState.copy(addToGroupContacts = contactsState.addToGroupContacts - contact)
    }

    fun openUserProfile(contact: Contact) {
        viewModelScope.launch {
            navigationManager.navigate(
                command = NavigationCommand(
                    destination = NavigationItem.OtherUserProfile.getRouteWithArgs(listOf(contact.id))
                )
            )
        }
    }

}
