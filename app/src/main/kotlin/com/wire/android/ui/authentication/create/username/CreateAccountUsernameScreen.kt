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

package com.wire.android.ui.authentication.create.username

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.feature.authentication.R as AuthenticationR
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.error.CoreFailureErrorDialog
import com.wire.android.ui.newauthentication.login.NewAuthContainer
import com.wire.android.ui.newauthentication.login.NewAuthHeader
import com.wire.kalium.common.error.CoreFailure

@Composable
internal fun CreateAccountUsernameRouteScreen(
    viewModel: CreateAccountUsernameViewModel<CoreFailure>,
    onSuccess: () -> Unit,
) {
    CreateAccountUsernameContent(
        textState = viewModel.textState,
        state = viewModel.state,
        text = CreateAccountUsernameText(
            title = stringResource(AuthenticationR.string.create_account_set_username_title),
            description = stringResource(AuthenticationR.string.create_account_username_text),
            usernamePlaceholder = stringResource(AuthenticationR.string.create_account_username_placeholder),
            usernameLabel = stringResource(AuthenticationR.string.create_account_username_label),
            usernameDescription = stringResource(AuthenticationR.string.create_account_username_description),
            usernameTakenError = stringResource(AuthenticationR.string.create_account_username_taken_error),
            mentionContentDescription = stringResource(R.string.content_description_mention_icon),
            confirmLabel = stringResource(R.string.label_confirm),
        ),
        mentionIconResId = R.drawable.ic_mention,
        onContinuePressed = viewModel::onContinue,
        onErrorDismiss = viewModel::onErrorDismiss,
        layout = { title, content ->
            NewAuthContainer(
                header = {
                    NewAuthHeader(
                        title = { title() },
                        canNavigateBack = false,
                    )
                },
                contentPadding = dimensions().spacing16x,
                content = { content() },
            )
        },
        genericFailureContent = { failure, onDismiss -> CoreFailureErrorDialog(failure, onDismiss) },
    )

    LaunchedEffect(viewModel.state.success) {
        if (viewModel.state.success) onSuccess()
    }
}
