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
package com.wire.ios.shared.auth.flow

import com.wire.ios.shared.IosCloseable
import com.wire.ios.shared.IosObservableViewModel
import com.wire.ios.shared.IosViewModel
import com.wire.shared.auth.flow.AuthLoginFlowEffect
import com.wire.shared.auth.flow.AuthLoginFlowIntent
import com.wire.shared.auth.flow.AuthLoginFlowState
import com.wire.shared.auth.flow.AuthLoginFlowViewModelFactory
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers

class AuthLoginFlowIosViewModel(
    private val delegate: IosViewModel<AuthLoginFlowState, AuthLoginFlowEffect, AuthLoginFlowIntent>,
) : IosObservableViewModel<AuthLoginFlowState, AuthLoginFlowEffect, AuthLoginFlowIntent> {
    val state = delegate.state
    val effects = delegate.effects

    override val currentState: AuthLoginFlowState
        get() = delegate.currentState

    override fun observeState(observer: (AuthLoginFlowState) -> Unit): IosCloseable =
        delegate.observeState(observer)

    override fun observeEffect(observer: (AuthLoginFlowEffect) -> Unit): IosCloseable =
        delegate.observeEffect(observer)

    override fun sendIntent(intent: AuthLoginFlowIntent) {
        delegate.sendIntent(intent)
    }

    override fun close() {
        delegate.close()
    }
}

@Inject
class AuthLoginFlowIosViewModelFactory(
    private val sharedFactory: AuthLoginFlowViewModelFactory,
) {
    fun create(): AuthLoginFlowIosViewModel =
        AuthLoginFlowIosViewModel(createGeneric())

    fun createGeneric(): IosViewModel<AuthLoginFlowState, AuthLoginFlowEffect, AuthLoginFlowIntent> {
        val sharedViewModel = sharedFactory.create(coroutineContext = Dispatchers.Main.immediate)
        return IosViewModel(
            state = sharedViewModel.state,
            effects = sharedViewModel.effects,
            onIntent = sharedViewModel::sendIntent,
            onClose = sharedViewModel::close,
        )
    }
}

fun createAuthLoginFlowIosViewModel(
    authLoginFlowIosViewModelFactory: AuthLoginFlowIosViewModelFactory,
): AuthLoginFlowIosViewModel =
    authLoginFlowIosViewModelFactory.create()

fun createGenericAuthLoginFlowIosViewModel(
    authLoginFlowIosViewModelFactory: AuthLoginFlowIosViewModelFactory,
): IosViewModel<AuthLoginFlowState, AuthLoginFlowEffect, AuthLoginFlowIntent> =
    authLoginFlowIosViewModelFactory.createGeneric()
