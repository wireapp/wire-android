/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.authentication.create.email

interface CreateAccountEmailGateway<LinksT, FailureT> {
    fun isEmailValid(email: String): Boolean
    suspend fun requestActivationCode(serverConfig: LinksT, email: String): ActivationCodeResult<FailureT>
}

sealed interface ActivationCodeResult<out FailureT> {
    data object Sent : ActivationCodeResult<Nothing>
    data object AlreadyInUse : ActivationCodeResult<Nothing>
    data object Blacklisted : ActivationCodeResult<Nothing>
    data object DomainBlocked : ActivationCodeResult<Nothing>
    data object InvalidEmail : ActivationCodeResult<Nothing>
    data class Generic<FailureT>(val failure: FailureT) : ActivationCodeResult<FailureT>
    data object AuthScopeUnavailable : ActivationCodeResult<Nothing>
}
