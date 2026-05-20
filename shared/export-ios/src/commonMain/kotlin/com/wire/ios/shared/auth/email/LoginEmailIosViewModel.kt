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
package com.wire.ios.shared.auth.email

import com.wire.ios.shared.IosCloseable
import com.wire.ios.shared.IosObservableViewModel
import com.wire.ios.shared.IosViewModel
import com.wire.shared.auth.email.LoginEmailEffect
import com.wire.shared.auth.email.LoginEmailIntent
import com.wire.shared.auth.email.LoginEmailState
import com.wire.shared.auth.email.LoginEmailViewModelFactory
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers

class LoginEmailIosViewModel(
    private val delegate: IosViewModel<LoginEmailState, LoginEmailEffect, LoginEmailIntent>,
) : IosObservableViewModel<LoginEmailState, LoginEmailEffect, LoginEmailIntent> {
    val state = delegate.state
    val effects = delegate.effects

    override val currentState: LoginEmailState
        get() = delegate.currentState

    override fun observeState(observer: (LoginEmailState) -> Unit): IosCloseable =
        delegate.observeState(observer)

    override fun observeEffect(observer: (LoginEmailEffect) -> Unit): IosCloseable =
        delegate.observeEffect(observer)

    override fun sendIntent(intent: LoginEmailIntent) {
        delegate.sendIntent(intent)
    }

    override fun close() {
        delegate.close()
    }
}

@Inject
class LoginEmailIosViewModelFactory(
    private val sharedFactory: LoginEmailViewModelFactory,
) {
    fun create(userIdentifier: String = ""): LoginEmailIosViewModel =
        LoginEmailIosViewModel(createGeneric(userIdentifier))

    fun createGeneric(userIdentifier: String = ""): IosViewModel<LoginEmailState, LoginEmailEffect, LoginEmailIntent> {
        val sharedViewModel = sharedFactory.create(
            userIdentifier = userIdentifier,
            coroutineContext = Dispatchers.Main.immediate,
        )
        return IosViewModel(
            state = sharedViewModel.state,
            effects = sharedViewModel.effects,
            onIntent = sharedViewModel::sendIntent,
            onClose = sharedViewModel::close,
        )
    }
}

fun createLoginEmailIosViewModel(
    loginEmailIosViewModelFactory: LoginEmailIosViewModelFactory,
    userIdentifier: String = "",
): LoginEmailIosViewModel =
    loginEmailIosViewModelFactory.create(userIdentifier)

fun createGenericLoginEmailIosViewModel(
    loginEmailIosViewModelFactory: LoginEmailIosViewModelFactory,
    userIdentifier: String = "",
): IosViewModel<LoginEmailState, LoginEmailEffect, LoginEmailIntent> =
    loginEmailIosViewModelFactory.createGeneric(userIdentifier)
