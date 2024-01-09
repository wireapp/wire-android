/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

package com.wire.android.ui.migration

import com.wire.android.migration.MigrationData
import com.wire.kalium.logic.data.user.UserId

sealed class MigrationState {
    data class InProgress(val type: MigrationData.Progress.Type) : MigrationState()
    sealed class Failed : MigrationState() {
        object NoNetwork : Failed()
        sealed class Account : Failed() {
            data class Specific(val userName: String, val userHandle: String) : Account()
            object Any : Account()
        }
        data class Messages(val errorCode: String) : Failed()
    }
    data class LoginRequired(val userHandle: String) : MigrationState()
    data class Success(val currentSessionAvailable: Boolean) : MigrationState()
}

sealed interface MigrationType {
    object Full : MigrationType
    data class SingleUser(val userId: UserId) : MigrationType
}
