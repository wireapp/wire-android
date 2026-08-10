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
package com.wire.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.kalium.logic.UserSessionPreparationFailure
import com.wire.kalium.logic.UserSessionPreparationState

@Composable
internal fun UserSessionPreparationScreen(
    state: UserSessionPreparationUiState,
    onRetry: () -> Unit,
    onUpdate: () -> Unit,
    onContactSupport: () -> Unit,
) {
    val content = state.content()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (content.action == null) {
            CircularProgressIndicator()
            Spacer(Modifier.height(32.dp))
        }
        Text(
            text = stringResource(content.title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(content.message),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        content.action?.let { action ->
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = when (action) {
                    UserSessionPreparationAction.Retry -> onRetry
                    UserSessionPreparationAction.Update -> onUpdate
                    UserSessionPreparationAction.ContactSupport -> onContactSupport
                }
            ) {
                Text(stringResource(action.label))
            }
        }
    }
}

internal sealed interface UserSessionPreparationUiState {
    data object ResolvingSession : UserSessionPreparationUiState
    data object OpeningDatabase : UserSessionPreparationUiState
    data object MigratingDatabase : UserSessionPreparationUiState
    data object Ready : UserSessionPreparationUiState
    data class Failed(val failure: UserSessionPreparationUiFailure) : UserSessionPreparationUiState
}

internal enum class UserSessionPreparationUiFailure {
    InsufficientStorage,
    TemporarilyUnavailable,
    ApplicationUpdateRequired,
    SupportRequired,
}

internal fun UserSessionPreparationState.toUiState(): UserSessionPreparationUiState = when (this) {
    UserSessionPreparationState.NotStarted -> UserSessionPreparationUiState.ResolvingSession
    UserSessionPreparationState.OpeningDatabase -> UserSessionPreparationUiState.OpeningDatabase
    UserSessionPreparationState.MigratingDatabase -> UserSessionPreparationUiState.MigratingDatabase
    UserSessionPreparationState.Ready -> UserSessionPreparationUiState.Ready
    is UserSessionPreparationState.Failed -> UserSessionPreparationUiState.Failed(reason.toUiFailure())
}

internal fun UserSessionPreparationFailure.toUiFailure(): UserSessionPreparationUiFailure = when (this) {
    UserSessionPreparationFailure.InsufficientStorage -> UserSessionPreparationUiFailure.InsufficientStorage
    UserSessionPreparationFailure.TemporarilyUnavailable -> UserSessionPreparationUiFailure.TemporarilyUnavailable
    UserSessionPreparationFailure.ApplicationUpdateRequired -> UserSessionPreparationUiFailure.ApplicationUpdateRequired
    UserSessionPreparationFailure.SupportRequired -> UserSessionPreparationUiFailure.SupportRequired
}

private enum class UserSessionPreparationAction(val label: Int) {
    Retry(R.string.label_try_again),
    Update(R.string.label_update),
    ContactSupport(R.string.user_session_preparation_contact_support),
}

private data class UserSessionPreparationContent(
    val title: Int,
    val message: Int,
    val action: UserSessionPreparationAction? = null,
)

@Composable
private fun UserSessionPreparationUiState.content(): UserSessionPreparationContent = when (this) {
    UserSessionPreparationUiState.ResolvingSession,
    UserSessionPreparationUiState.OpeningDatabase,
    UserSessionPreparationUiState.Ready -> UserSessionPreparationContent(
        title = R.string.user_session_preparation_opening_title,
        message = R.string.user_session_preparation_opening_message,
    )

    UserSessionPreparationUiState.MigratingDatabase -> UserSessionPreparationContent(
        title = R.string.user_session_preparation_migrating_title,
        message = R.string.user_session_preparation_migrating_message,
    )

    is UserSessionPreparationUiState.Failed -> when (failure) {
        UserSessionPreparationUiFailure.InsufficientStorage -> UserSessionPreparationContent(
            title = R.string.user_session_preparation_storage_title,
            message = R.string.user_session_preparation_storage_message,
            action = UserSessionPreparationAction.Retry,
        )

        UserSessionPreparationUiFailure.TemporarilyUnavailable -> UserSessionPreparationContent(
            title = R.string.user_session_preparation_temporary_title,
            message = R.string.user_session_preparation_temporary_message,
            action = UserSessionPreparationAction.Retry,
        )

        UserSessionPreparationUiFailure.ApplicationUpdateRequired -> UserSessionPreparationContent(
            title = R.string.update_app_dialog_title,
            message = R.string.update_app_dialog_body,
            action = UserSessionPreparationAction.Update,
        )

        UserSessionPreparationUiFailure.SupportRequired -> UserSessionPreparationContent(
            title = R.string.user_session_preparation_support_title,
            message = R.string.user_session_preparation_support_message,
            action = UserSessionPreparationAction.ContactSupport,
        )
    }
}
