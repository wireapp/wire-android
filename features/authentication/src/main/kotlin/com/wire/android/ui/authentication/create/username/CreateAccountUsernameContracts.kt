/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.authentication.create.username

interface CreateAccountUsernameGateway<FailureT> {
    fun validateUsername(username: String): UsernameValidation
    suspend fun setUsername(username: String): SetUsernameResult<FailureT>
}

enum class UsernameValidation {
    Valid,
    Invalid,
}

sealed interface SetUsernameResult<out FailureT> {
    data object Success : SetUsernameResult<Nothing>
    data object UsernameTaken : SetUsernameResult<Nothing>
    data object UsernameInvalid : SetUsernameResult<Nothing>
    data class Failure<FailureT>(val failure: FailureT) : SetUsernameResult<FailureT>
}

interface CreateAccountUsernameAnalytics {
    suspend fun usernameScreenShown()
    suspend fun accountCreationCompleted()
}
