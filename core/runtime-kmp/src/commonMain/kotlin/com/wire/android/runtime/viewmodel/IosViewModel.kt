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
package com.wire.android.runtime.viewmodel

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class IosViewModel<State : Any, Effect : Any, Intent : Any>(
    val state: StateFlow<State>,
    val effects: Flow<Effect>,
    private val onIntent: (Intent) -> Unit,
    private val onClose: () -> Unit = {},
) {
    fun sendIntent(intent: Intent) = onIntent(intent)

    fun close() = onClose()
}
