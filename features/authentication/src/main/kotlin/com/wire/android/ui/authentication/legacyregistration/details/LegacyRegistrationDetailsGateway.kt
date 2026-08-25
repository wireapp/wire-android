/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication.legacyregistration.details

data class LegacyRegistrationDetailsInput<LinksT>(
    val customServerConfig: LinksT?,
    val email: String,
)

interface LegacyRegistrationDetailsGateway<LinksT, FailureT> {
    fun isPasswordValid(password: String): Boolean
    fun isEmailValid(email: String): Boolean
    suspend fun requestActivationCode(serverConfig: LinksT, email: String): LegacyActivationCodeResult<FailureT>
    suspend fun setAnonymousRegistrationEnabled(enabled: Boolean)
    suspend fun onAccountSetup(withPasswordTries: Boolean)
    suspend fun onTermsOfUseDialog()
}

sealed interface LegacyActivationCodeResult<out FailureT> {
    data object Sent : LegacyActivationCodeResult<Nothing>
    data object AlreadyInUse : LegacyActivationCodeResult<Nothing>
    data object Blacklisted : LegacyActivationCodeResult<Nothing>
    data object DomainBlocked : LegacyActivationCodeResult<Nothing>
    data object InvalidEmail : LegacyActivationCodeResult<Nothing>
    data class Generic<FailureT>(val failure: FailureT) : LegacyActivationCodeResult<FailureT>
    data object AuthScopeUnavailable : LegacyActivationCodeResult<Nothing>
}
