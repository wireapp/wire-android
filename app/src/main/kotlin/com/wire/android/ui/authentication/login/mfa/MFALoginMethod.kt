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

package com.wire.android.ui.authentication.login.mfa

internal sealed interface MFALoginMethod {
    val id: String

    data class Authenticator(override val id: String, val name: String) : MFALoginMethod

    data object RecoveryCode : MFALoginMethod {
        override val id: String = "recovery-code"
    }
}

/** Temporary options for the Android MFA login spike. */
internal val mockMFALoginMethods: List<MFALoginMethod> = listOf(
    MFALoginMethod.Authenticator(id = "phone", name = "Phone"),
    MFALoginMethod.Authenticator(id = "backup-phone", name = "Backup phone"),
    MFALoginMethod.RecoveryCode,
)
