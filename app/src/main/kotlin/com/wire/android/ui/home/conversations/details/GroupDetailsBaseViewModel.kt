package com.wire.android.ui.home.conversations.details

import androidx.lifecycle.ViewModel
import com.wire.android.util.ui.UIText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

@HiltViewModel
open class GroupDetailsBaseViewModel @Inject constructor() : ViewModel() {

    private val _snackBarMessenger = MutableSharedFlow<UIText>()
    val snackBarMessage = _snackBarMessenger.asSharedFlow()

    suspend fun showSnackBarMessage(message: UIText) {
        _snackBarMessenger.emit(message)
    }

}
