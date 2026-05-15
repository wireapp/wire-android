/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.ios.shared.auth.bridge

import com.wire.ios.shared.IosViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AuthBridgeViewModel<State : Any, Effect : Any, Intent : Any> {
    val state: StateFlow<State>
    val effects: Flow<Effect>

    fun sendIntent(intent: Intent)

    fun close() = Unit
}

fun <State : Any, Effect : Any, Intent : Any> AuthBridgeViewModel<State, Effect, Intent>.asIosViewModel():
        IosViewModel<State, Effect, Intent> =
    authIosViewModel(
        state = state,
        effects = effects,
        onIntent = ::sendIntent,
        onClose = ::close,
    )

fun <State : Any, Effect : Any, Intent : Any> authIosViewModel(
    state: StateFlow<State>,
    effects: Flow<Effect>,
    onIntent: (Intent) -> Unit,
    onClose: () -> Unit = {},
): IosViewModel<State, Effect, Intent> =
    IosViewModel(
        state = state,
        effects = effects,
        onIntent = onIntent,
        onClose = onClose,
    )
