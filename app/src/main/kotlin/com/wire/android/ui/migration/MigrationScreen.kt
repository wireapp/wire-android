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

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.migration.MigrationData
import com.wire.android.ui.common.SettingUpWireScreenContent
import com.wire.android.ui.common.SettingUpWireScreenType
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.stringWithStyledArgs

@Composable
fun MigrationScreen(viewModel: MigrationViewModel = hiltViewModel()) {
    MigrationScreenContent(viewModel.state, viewModel::retry, viewModel::finish, viewModel::accountLogin)
}

@Composable
private fun MigrationScreenContent(
    state: MigrationState,
    retry: () -> Unit = {},
    finish: () -> Unit = {},
    accountLogin: (String?) -> Unit = {}
) {
    SettingUpWireScreenContent(
        message = when (state) {
            is MigrationState.InProgress -> when (state.type) {
                MigrationData.Progress.Type.ACCOUNTS -> AnnotatedString(stringResource(R.string.migration_accounts_message))
                MigrationData.Progress.Type.MESSAGES -> AnnotatedString(stringResource(R.string.migration_messages_message))
                MigrationData.Progress.Type.UNKNOWN -> AnnotatedString(stringResource(R.string.migration_message_unknown))
            }
            is MigrationState.Failed.NoNetwork -> buildAnnotatedString {
                withStyle(SpanStyle(color = MaterialTheme.wireColorScheme.error)) {
                    append(stringResource(R.string.error_no_network_message))
                }
            }
            is MigrationState.Failed.Messages -> buildAnnotatedString {
                withStyle(SpanStyle(color = MaterialTheme.wireColorScheme.error)) {
                    append(stringResource(R.string.migration_messages_failure, state.errorCode))
                }
            }
            is MigrationState.Failed.Account.Any -> AnnotatedString(stringResource(R.string.migration_login_required))
            is MigrationState.Failed.Account.Specific -> LocalContext.current.resources.stringWithStyledArgs(
                stringResId = R.string.migration_login_required_specific_account,
                normalStyle = MaterialTheme.wireTypography.body01,
                argsStyle = MaterialTheme.wireTypography.body02,
                normalColor = MaterialTheme.wireColorScheme.secondaryText,
                argsColor = MaterialTheme.wireColorScheme.secondaryText,
                stringResource(R.string.migration_login_required_specific_account_name, state.userName, state.userHandle)
            )
        },
        title = when (state) {
            is MigrationState.Failed -> null
            is MigrationState.InProgress -> {
                MigrationData.Progress.steps.indexOf(state.type).let { stepNumber ->
                    if (stepNumber >= 0) stringResource(R.string.migration_title_step, stepNumber + 1, MigrationData.Progress.steps.size)
                    else null
                }
            }
        },
        type = when (state) {
            is MigrationState.InProgress -> SettingUpWireScreenType.Progress
            is MigrationState.Failed.NoNetwork -> SettingUpWireScreenType.Failure(
                buttonTextResId = R.string.label_retry,
                onButtonClick = retry
            )
            is MigrationState.Failed.Account.Any -> SettingUpWireScreenType.Failure(
                buttonTextResId = R.string.label_continue,
                onButtonClick = { accountLogin(null) }
            )
            is MigrationState.Failed.Account.Specific -> SettingUpWireScreenType.Failure(
                buttonTextResId = R.string.label_continue,
                onButtonClick = { accountLogin(state.userHandle) }
            )
            is MigrationState.Failed.Messages -> SettingUpWireScreenType.Failure(
                buttonTextResId = R.string.label_continue,
                onButtonClick = finish
            )
        }
    )
}

@Preview
@Composable
fun PreviewMigrationScreenInProgress() {
    MigrationScreenContent(state = MigrationState.InProgress(MigrationData.Progress.Type.ACCOUNTS))
}
@Preview
@Composable
fun PreviewMigrationScreenFailedNoNetwork() {
    MigrationScreenContent(state = MigrationState.Failed.NoNetwork)
}
@Preview
@Composable
fun PreviewMigrationScreenFailedAccountAny() {
    MigrationScreenContent(state = MigrationState.Failed.Account.Any)
}
@Preview
@Composable
fun PreviewMigrationScreenFailedAccountSpecific() {
    MigrationScreenContent(state = MigrationState.Failed.Account.Specific(userName = "name", userHandle = "@handle"))
}
@Preview
@Composable
fun PreviewMigrationScreenFailedMessages() {
    MigrationScreenContent(state = MigrationState.Failed.Messages(errorCode = "123"))
}
