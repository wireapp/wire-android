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
package com.wire.ios.shared.auth.sso

import com.wire.ios.shared.IosCloseable
import com.wire.ios.shared.IosObservableViewModel
import com.wire.ios.shared.IosViewModel
import com.wire.shared.auth.sso.LoginSsoEffect
import com.wire.shared.auth.sso.LoginSsoIntent
import com.wire.shared.auth.sso.LoginSsoState
import com.wire.shared.auth.sso.LoginSsoViewModelFactory
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers

class LoginSsoIosViewModel(
    private val delegate: IosViewModel<LoginSsoState, LoginSsoEffect, LoginSsoIntent>,
) : IosObservableViewModel<LoginSsoState, LoginSsoEffect, LoginSsoIntent> {
    val state = delegate.state
    val effects = delegate.effects

    override val currentState: LoginSsoState
        get() = delegate.currentState

    override fun observeState(observer: (LoginSsoState) -> Unit): IosCloseable =
        delegate.observeState(observer)

    override fun observeEffect(observer: (LoginSsoEffect) -> Unit): IosCloseable =
        delegate.observeEffect(observer)

    override fun sendIntent(intent: LoginSsoIntent) {
        delegate.sendIntent(intent)
    }

    override fun close() {
        delegate.close()
    }
}

@Inject
class LoginSsoIosViewModelFactory(
    private val sharedFactory: LoginSsoViewModelFactory,
) {
    fun create(): LoginSsoIosViewModel =
        LoginSsoIosViewModel(createGeneric())

    fun createGeneric(): IosViewModel<LoginSsoState, LoginSsoEffect, LoginSsoIntent> {
        val sharedViewModel = sharedFactory.create(coroutineContext = Dispatchers.Main.immediate)
        return IosViewModel(
            state = sharedViewModel.state,
            effects = sharedViewModel.effects,
            onIntent = sharedViewModel::sendIntent,
            onClose = sharedViewModel::close,
        )
    }
}

fun createLoginSsoIosViewModel(
    loginSsoIosViewModelFactory: LoginSsoIosViewModelFactory,
): LoginSsoIosViewModel =
    loginSsoIosViewModelFactory.create()

fun createGenericLoginSsoIosViewModel(
    loginSsoIosViewModelFactory: LoginSsoIosViewModelFactory,
): IosViewModel<LoginSsoState, LoginSsoEffect, LoginSsoIntent> =
    loginSsoIosViewModelFactory.createGeneric()
