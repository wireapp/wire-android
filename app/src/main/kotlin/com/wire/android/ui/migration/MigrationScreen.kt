/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui.migration

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.migration.MigrationData
import com.wire.android.ui.common.SettingUpWireScreenContent
import com.wire.android.ui.common.SettingUpWireScreenType

@Composable
fun MigrationScreen(viewModel: MigrationViewModel = hiltViewModel()) {
    SettingUpWireScreenContent(
        messageResId = when (val state = viewModel.state) {
            MigrationState.Failed -> R.string.error_no_network_message
            is MigrationState.InProgress -> when (state.type) {
                MigrationData.Progress.Type.SERVER_CONFIGS -> R.string.migration_server_configs_message
                MigrationData.Progress.Type.ACCOUNTS -> R.string.migration_accounts_message
                MigrationData.Progress.Type.CLIENTS -> R.string.migration_clients_message
                MigrationData.Progress.Type.USERS -> R.string.migration_users_message
                MigrationData.Progress.Type.CONVERSATIONS -> R.string.migration_conversations_message
                MigrationData.Progress.Type.MESSAGES -> R.string.migration_messages_message
                MigrationData.Progress.Type.UNKNOWN -> R.string.migration_message_unknown
            }
        },
        type = when (viewModel.state) {
            is MigrationState.InProgress -> SettingUpWireScreenType.Progress
            is MigrationState.Failed -> SettingUpWireScreenType.Failure(onRetryClick = viewModel::retry)
        }
    )
}
