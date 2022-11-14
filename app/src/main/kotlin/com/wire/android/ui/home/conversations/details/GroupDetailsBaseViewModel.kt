package com.wire.android.ui.home.conversations.details

import androidx.lifecycle.SavedStateHandle
import com.wire.android.navigation.SavedStateViewModel
import com.wire.android.util.ui.UIText
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

open class GroupDetailsBaseViewModel(savedStateHandle: SavedStateHandle) : SavedStateViewModel(savedStateHandle) {

    private val _snackBarMessenger = MutableSharedFlow<UIText>()
    val snackBarMessage = _snackBarMessenger.asSharedFlow()

    suspend fun showSnackBarMessage(message: UIText) {
        _snackBarMessenger.emit(message)
    }

}
