/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

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

    fun update(update: (Saveable) -> Saveable) {
        savedState?.let {
            savedState = update(it)
        }
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
