package com.wire.android.ui.home.newconversation.contacts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.kalium.logic.feature.publicuser.GetAllKnownUsersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val getAllKnownUsersUseCase: GetAllKnownUsersUseCase
) : ViewModel() {

    var contactState by mutableStateOf(ContactsState())

    init {
        viewModelScope.launch {
            getAllKnownUsersUseCase()
                .onStart {
                    contactState = contactState.copy(isLoading = true)
                }
                .collect {
                    contactState = contactState.copy(
                        isLoading = false,
                        contacts = it.map { publicUser -> publicUser.toContact() }
                    )
                }
        }
    }

    fun close() {
        viewModelScope.launch {
            navigationManager.navigateBack()
        }
    }

    fun addContactToGroup(contact: Contact) {
        contactState = contactState.copy(addToGroupContacts = contactState.addToGroupContacts + contact)
    }

    fun removeContactFromGroup(contact: Contact) {
        contactState = contactState.copy(addToGroupContacts = contactState.addToGroupContacts - contact)
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
