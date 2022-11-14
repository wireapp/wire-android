package com.wire.android.ui.home.conversations.details

import androidx.lifecycle.ViewModel
import com.wire.android.util.ui.UIText
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

open class GroupDetailsBaseViewModel : ViewModel() {

    private val _snackBarMessenger = MutableSharedFlow<UIText>()
    val snackBarMessage = _snackBarMessenger.asSharedFlow()

    suspend fun showSnackBarMessage(message: UIText) {
        _snackBarMessenger.emit(message)
    }

}
