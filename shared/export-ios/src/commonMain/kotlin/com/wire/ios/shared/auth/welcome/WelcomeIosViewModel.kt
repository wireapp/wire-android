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
package com.wire.ios.shared.auth.welcome

import com.wire.ios.shared.IosCloseable
import com.wire.ios.shared.IosObservableViewModel
import com.wire.ios.shared.IosViewModel
import com.wire.shared.auth.welcome.WelcomeEffect
import com.wire.shared.auth.welcome.WelcomeIntent
import com.wire.shared.auth.welcome.WelcomeState
import com.wire.shared.auth.welcome.WelcomeViewModelFactory
import dev.zacsweers.metro.Inject

class WelcomeIosViewModel(
    private val delegate: IosViewModel<WelcomeState, WelcomeEffect, WelcomeIntent>,
) : IosObservableViewModel<WelcomeState, WelcomeEffect, WelcomeIntent> {
    val state = delegate.state
    val effects = delegate.effects

    override val currentState: WelcomeState
        get() = delegate.currentState

    override fun observeState(observer: (WelcomeState) -> Unit): IosCloseable =
        delegate.observeState(observer)

    override fun observeEffect(observer: (WelcomeEffect) -> Unit): IosCloseable =
        delegate.observeEffect(observer)

    override fun sendIntent(intent: WelcomeIntent) {
        delegate.sendIntent(intent)
    }

    override fun close() {
        delegate.close()
    }
}

@Inject
class WelcomeIosViewModelFactory(
    private val sharedFactory: WelcomeViewModelFactory,
) {
    fun create(): WelcomeIosViewModel =
        WelcomeIosViewModel(createGeneric())

    fun createGeneric(): IosViewModel<WelcomeState, WelcomeEffect, WelcomeIntent> {
        val sharedViewModel = sharedFactory.create()
        return IosViewModel(
            state = sharedViewModel.state,
            effects = sharedViewModel.effects,
            onIntent = sharedViewModel::sendIntent,
            onClose = sharedViewModel::close,
        )
    }
}

fun createWelcomeIosViewModel(
    welcomeIosViewModelFactory: WelcomeIosViewModelFactory,
): WelcomeIosViewModel =
    welcomeIosViewModelFactory.create()

fun createGenericWelcomeIosViewModel(
    welcomeIosViewModelFactory: WelcomeIosViewModelFactory,
): IosViewModel<WelcomeState, WelcomeEffect, WelcomeIntent> =
    welcomeIosViewModelFactory.createGeneric()
