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

    @Suppress("MagicNumber")
    val contactsState by mutableStateOf(
        ContactsState(
            contacts = buildList {
                add(
                    Contact(
                        id = 1,
                        "This is first contact"
                    )
                )
                for (i in 2..11) {
                    add(
                        Contact(
                            id = 1,
                            "This is $i contact"
                        )
                    )
                }
                add(
                    Contact(
                        id = 12,
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

}
