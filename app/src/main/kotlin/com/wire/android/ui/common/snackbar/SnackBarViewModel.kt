package com.wire.android.ui.common.snackbar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.util.ui.UIText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SnackBarState(
    val showSnackBar: Boolean = false,
    val message: UIText? = null
)

@HiltViewModel
class SnackBarViewModel @Inject constructor(private val showSnackBar: ShowSnackBarUseCase): ViewModel() {

    private val _snackBarMessage = MutableSharedFlow<SnackBarState>()
    val snackBarMessage = _snackBarMessage.asSharedFlow()

    init {
        viewModelScope.launch {
            showSnackBar.observerSnackBarStatus.collect { result ->
                result.message?.let {
                    _snackBarMessage.emit(SnackBarState(message = it))
                }
            }
        }
    }
}
