package com.wire.android.ui.home.newconversation.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SearchPeopleViewModel @Inject constructor() : ViewModel() {

    val state: SearchPeopleState by mutableStateOf(
        SearchPeopleState()
    )

}



