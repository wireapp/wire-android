package com.wire.android.ui.common.visbility

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Composable
fun <Saveable: Any> rememberVisibilityState(saveable: Saveable? = null): VisibilityState<Saveable> {
    val searchBarState = rememberSaveable(
        saver = VisibilityState.saver(saveable)
    ) {
        VisibilityState()
    }

    return searchBarState
}

class VisibilityState<Saveable: Any>(isVisible: Boolean = false, saveable: Saveable? = null) {

    var isVisible by mutableStateOf(isVisible)
        private set

    var savedState by mutableStateOf<Saveable?>(saveable)
        private set

    fun dismiss() {
        isVisible = false
    }

    fun show(saveable: Saveable) {
        savedState = saveable
        isVisible = true
    }

    companion object {
        fun <Saveable: Any> saver(saveable: Saveable?): Saver<VisibilityState<Saveable>, *> = Saver(
            save = {
                listOf(it.isVisible, saveable)
            },
            restore = {
                VisibilityState(isVisible = it[0] as Boolean, saveable= it[1] as Saveable?)
            }
        )
    }
}
