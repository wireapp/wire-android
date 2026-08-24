/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.authentication.create.code

data class CreateAccountCodeViewState<FlowT, UserT, FailureT>(
    val type: FlowT,
    val codeLength: Int = DEFAULT_VERIFICATION_CODE_LENGTH,
    val email: String = "",
    val loading: Boolean = false,
    val result: CreateAccountCodeResult<UserT, FailureT> = CreateAccountCodeResult.None,
    val remainingTimerText: String? = null,
) {
    companion object {
        const val DEFAULT_VERIFICATION_CODE_LENGTH = 6
    }
}

sealed interface CreateAccountCodeResult<out UserT, out FailureT> {
    data object None : CreateAccountCodeResult<Nothing, Nothing>
    data class Success<UserT>(val userId: UserT) : CreateAccountCodeResult<UserT, Nothing>

    sealed interface Error<out UserT, out FailureT> : CreateAccountCodeResult<UserT, FailureT> {
        sealed interface TextFieldError : Error<Nothing, Nothing> {
            data object InvalidActivationCodeError : TextFieldError
        }

        sealed interface DialogError<out FailureT> : Error<Nothing, FailureT> {
            data object InvalidEmailError : DialogError<Nothing>
            data object AccountAlreadyExistsError : DialogError<Nothing>
            data object BlackListedError : DialogError<Nothing>
            data object EmailDomainBlockedError : DialogError<Nothing>
            data object TeamMembersLimitError : DialogError<Nothing>
            data object CreationRestrictedError : DialogError<Nothing>
            data object UserAlreadyExistsError : DialogError<Nothing>
            data class GenericError<FailureT>(val failure: FailureT) : DialogError<FailureT>
        }

        data class TooManyDevicesError<UserT>(val userId: UserT) : Error<UserT, Nothing>
    }
}
