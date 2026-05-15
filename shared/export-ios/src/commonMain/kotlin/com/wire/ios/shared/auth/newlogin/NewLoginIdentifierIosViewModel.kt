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
import com.wire.ios.shared.WireIosSharedConfig
import com.wire.ios.shared.auth.login.model.LoginServerLinks
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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
    private val config: WireIosSharedConfig,
) {
    fun create(): NewLoginIdentifierIosViewModel =
        NewLoginIdentifierIosViewModel(createGeneric())

    fun createGeneric(): IosViewModel<NewLoginIdentifierState, NewLoginIdentifierEffect, NewLoginIdentifierIntent> {
        val state = MutableStateFlow(
            NewLoginIdentifierState(
                isThereActiveSession = config.isThereActiveSession,
            )
        )
        val effects = MutableSharedFlow<NewLoginIdentifierEffect>(extraBufferCapacity = 1)

        return IosViewModel(
            state = state.asStateFlow(),
            effects = effects.asSharedFlow(),
            onIntent = { intent ->
                when (intent) {
                    is NewLoginIdentifierIntent.UserIdentifierChanged -> {
                        state.update { it.withUserIdentifier(intent.userIdentifier) }
                    }

                    NewLoginIdentifierIntent.Submit -> {
                        submit(
                            state = state,
                            effects = effects,
                            serverLinks = config.defaultServerLinks,
                        )
                    }

                    NewLoginIdentifierIntent.DismissDialog -> {
                        state.update { it.withFlowState(NewLoginIdentifierFlowState.Default) }
                    }

                    is NewLoginIdentifierIntent.ConfirmCustomServer -> {
                        effects.tryEmit(
                            NewLoginIdentifierEffect.OpenCustomConfig(
                                userIdentifier = state.value.userIdentifier,
                                serverLinks = intent.serverLinks,
                            )
                        )
                        state.update { it.withFlowState(NewLoginIdentifierFlowState.Default) }
                    }

                    is NewLoginIdentifierIntent.SSOResultReceived -> {
                        handleSsoResult(
                            result = intent.result,
                            state = state,
                            effects = effects,
                        )
                    }
                }
            }
        )
    }

    private fun submit(
        state: MutableStateFlow<NewLoginIdentifierState>,
        effects: MutableSharedFlow<NewLoginIdentifierEffect>,
        serverLinks: LoginServerLinks,
    ) {
        val userIdentifier = state.value.userIdentifier.trim()
        when {
            userIdentifier.isValidSsoCode() -> {
                effects.tryEmit(
                    NewLoginIdentifierEffect.OpenSSO(
                        url = serverLinks.accounts,
                        config = NewLoginSsoUrlConfig(userIdentifier = userIdentifier),
                    )
                )
                state.update { it.withFlowState(NewLoginIdentifierFlowState.Default) }
            }

            userIdentifier.isValidEmail() -> {
                effects.tryEmit(
                    NewLoginIdentifierEffect.OpenEmailPassword(
                        userIdentifier = userIdentifier,
                        path = NewLoginPasswordPath(),
                    )
                )
                state.update { it.withFlowState(NewLoginIdentifierFlowState.Default) }
            }

            else -> state.update {
                it.withFlowState(
                    NewLoginIdentifierFlowState.TextFieldError(NewLoginIdentifierTextFieldError.InvalidValue)
                )
            }
        }
    }

    private fun handleSsoResult(
        result: NewLoginSsoResult,
        state: MutableStateFlow<NewLoginIdentifierState>,
        effects: MutableSharedFlow<NewLoginIdentifierEffect>,
    ) {
        when (result) {
            is NewLoginSsoResult.Success -> {
                effects.tryEmit(NewLoginIdentifierEffect.LoginSucceeded(NewLoginSuccessNextStep.None))
                state.update { it.withFlowState(NewLoginIdentifierFlowState.Default) }
            }

            is NewLoginSsoResult.Failure -> {
                state.update {
                    it.withFlowState(
                        NewLoginIdentifierFlowState.DialogError(
                            NewLoginIdentifierDialogError.SSOResultFailure(result.code)
                        )
                    )
                }
            }
        }
    }
}

fun createNewLoginIdentifierIosViewModel(
    factory: NewLoginIdentifierIosViewModelFactory,
): NewLoginIdentifierIosViewModel =
    factory.create()

fun createGenericNewLoginIdentifierIosViewModel(
    factory: NewLoginIdentifierIosViewModelFactory,
): IosViewModel<NewLoginIdentifierState, NewLoginIdentifierEffect, NewLoginIdentifierIntent> =
    factory.createGeneric()

private fun String.isValidEmail(): Boolean {
    val atIndex = indexOf('@')
    val dotIndex = lastIndexOf('.')
    return atIndex > 0 && dotIndex > atIndex + 1 && dotIndex < lastIndex
}

private fun String.isValidSsoCode(): Boolean =
    startsWith(SSO_CODE_PREFIX) && removePrefix(SSO_CODE_PREFIX).matches(uuidRegex)

private const val SSO_CODE_PREFIX = "wire-"
private val uuidRegex = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
