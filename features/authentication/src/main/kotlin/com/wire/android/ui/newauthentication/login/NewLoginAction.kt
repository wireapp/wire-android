/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.newauthentication.login

sealed interface NewLoginAction<out LinksT, out UserT> {
    data class EnterpriseLoginNotSupported(val userIdentifier: String) : NewLoginAction<Nothing, Nothing>

    data class EmailPassword<LinksT>(
        val userIdentifier: String,
        val serverConfig: LinksT,
        val isCloudAccountCreationPossible: Boolean?,
        val claimedDomain: String? = null,
    ) : NewLoginAction<LinksT, Nothing>

    data class CustomConfig<LinksT>(
        val userIdentifier: String,
        val customServerConfig: LinksT,
    ) : NewLoginAction<LinksT, Nothing>

    data class SSO(val url: String, val userIdentifier: String) : NewLoginAction<Nothing, Nothing>

    data class Success<UserT>(val nextStep: NextStep<UserT>) : NewLoginAction<Nothing, UserT> {
        sealed interface NextStep<out UserT> {
            data class E2EIEnrollment<UserT>(val userId: UserT) : NextStep<UserT>
            data class TooManyDevices<UserT>(val userId: UserT) : NextStep<UserT>
            data class InitialSync<UserT>(val userId: UserT) : NextStep<UserT>
            data class None<UserT>(val userId: UserT) : NextStep<UserT>
        }
    }
}
