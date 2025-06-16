/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

interface ActionsManager<T> {
    val actions: Flow<T> get() = emptyFlow()
    fun <VM : ViewModel> VM.sendAction(action: T) {}
}

class ActionsManagerImpl<T> : ActionsManager<T> {
    private val _actions: Channel<T> = Channel(
        capacity = Channel.BUFFERED,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val actions: Flow<T> = _actions
        .receiveAsFlow()
        .flowOn(Dispatchers.Main.immediate)

    override fun <VM : ViewModel> VM.sendAction(action: T) {
        viewModelScope.launch { _actions.send(action) }
    }
}

open class ActionsViewModel<T> : ViewModel(), ActionsManager<T> by ActionsManagerImpl()

@Composable
fun <T> HandleActions(actionsFlow: Flow<T>, onAction: (T) -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            actionsFlow.collect(onAction)
        }
    }
}
