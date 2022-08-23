package com.wire.android.ui.common.visbility

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Composable
fun rememberVisibilityState(): VisibilityState {
    val searchBarState = rememberSaveable(
        saver = VisibilityState.saver()
    ) {
        VisibilityState()
    }

    return searchBarState
}

class VisibilityState(isVisible: Boolean = false) {

    var isVisible by mutableStateOf(isVisible)
        private set

    fun dismiss() {
        isVisible = false
    }

    fun show() {
        isVisible = true
    }

    companion object {
        fun saver(): Saver<VisibilityState, *> = Saver(
            save = {
                listOf(it.isVisible)
            },
            restore = {
                VisibilityState(isVisible = it[0])
            }
        )
    }
}
