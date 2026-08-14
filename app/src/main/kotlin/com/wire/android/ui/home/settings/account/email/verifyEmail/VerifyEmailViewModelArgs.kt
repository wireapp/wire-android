/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.home.settings.account.email.verifyEmail

import com.wire.android.ui.home.settings.VerifyEmailRoute

data class VerifyEmailViewModelArgs(
    val newEmail: String,
) {
    init {
        require(newEmail.isNotBlank()) { "The email to verify cannot be blank" }
    }
}

internal fun VerifyEmailNavArgs.toViewModelArgs() = VerifyEmailViewModelArgs(newEmail)

internal fun VerifyEmailRoute.toViewModelArgs() = VerifyEmailViewModelArgs(newEmail)
