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

package com.wire.android.ui.authentication.login

import androidx.lifecycle.SavedStateHandle

private const val USER_IDENTIFIER_SAVED_STATE_KEY = "user_identifier"
private const val SSO_CODE_SAVED_STATE_KEY = "sso_code"

class SavedStateLoginSavedInputStore(
    private val savedStateHandle: SavedStateHandle,
) : LoginSavedInputStore {
    override var userIdentifier: String?
        get() = savedStateHandle[USER_IDENTIFIER_SAVED_STATE_KEY]
        set(value) {
            savedStateHandle[USER_IDENTIFIER_SAVED_STATE_KEY] = value
        }

    override var ssoCode: String?
        get() = savedStateHandle[SSO_CODE_SAVED_STATE_KEY]
        set(value) {
            savedStateHandle[SSO_CODE_SAVED_STATE_KEY] = value
        }
}
