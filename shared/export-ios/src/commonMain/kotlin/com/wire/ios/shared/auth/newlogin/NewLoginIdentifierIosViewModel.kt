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
package com.wire.ios.shared.auth.newlogin

import com.wire.ios.shared.IosCloseable
import com.wire.ios.shared.IosObservableViewModel
import com.wire.ios.shared.IosViewModel
import com.wire.shared.auth.newlogin.NewLoginIdentifierEffect
import com.wire.shared.auth.newlogin.NewLoginIdentifierIntent
import com.wire.shared.auth.newlogin.NewLoginIdentifierState
import com.wire.shared.auth.newlogin.NewLoginIdentifierViewModelFactory
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers

class NewLoginIdentifierIosViewModel(
    private val delegate: IosViewModel<NewLoginIdentifierState, NewLoginIdentifierEffect, NewLoginIdentifierIntent>,
) : IosObservableViewModel<NewLoginIdentifierState, NewLoginIdentifierEffect, NewLoginIdentifierIntent> {
    val state = delegate.state
    val effects = delegate.effects

    override val currentState: NewLoginIdentifierState
        get() = delegate.currentState

    override fun observeState(observer: (NewLoginIdentifierState) -> Unit): IosCloseable =
        delegate.observeState(observer)

    override fun observeEffect(observer: (NewLoginIdentifierEffect) -> Unit): IosCloseable =
        delegate.observeEffect(observer)

    override fun sendIntent(intent: NewLoginIdentifierIntent) {
        delegate.sendIntent(intent)
    }

    override fun close() {
        delegate.close()
    }
}

@Inject
class NewLoginIdentifierIosViewModelFactory(
    private val sharedFactory: NewLoginIdentifierViewModelFactory,
) {
    fun create(): NewLoginIdentifierIosViewModel =
        NewLoginIdentifierIosViewModel(createGeneric())

    fun createGeneric(): IosViewModel<NewLoginIdentifierState, NewLoginIdentifierEffect, NewLoginIdentifierIntent> {
        val sharedViewModel = sharedFactory.create(coroutineContext = Dispatchers.Main.immediate)
        return IosViewModel(
            state = sharedViewModel.state,
            effects = sharedViewModel.effects,
            onIntent = sharedViewModel::sendIntent,
            onClose = sharedViewModel::close,
        )
    }
}

fun createNewLoginIdentifierIosViewModel(
    newLoginIdentifierIosViewModelFactory: NewLoginIdentifierIosViewModelFactory,
): NewLoginIdentifierIosViewModel =
    newLoginIdentifierIosViewModelFactory.create()

fun createGenericNewLoginIdentifierIosViewModel(
    newLoginIdentifierIosViewModelFactory: NewLoginIdentifierIosViewModelFactory,
): IosViewModel<NewLoginIdentifierState, NewLoginIdentifierEffect, NewLoginIdentifierIntent> =
    newLoginIdentifierIosViewModelFactory.createGeneric()
