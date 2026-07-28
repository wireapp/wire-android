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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wire.android.R
import com.wire.android.ui.common.SettingUpWireScreenContent
import com.wire.android.ui.common.SettingUpWireScreenType
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.theme.WireTheme

@Composable
internal fun SessionStartupScreen(viewModel: SessionStartupViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SessionStartupScreenContent(state = state, onRetry = viewModel::retry)
}

@Composable
internal fun SessionStartupScreenContent(
    state: SessionStartupUiState,
    onRetry: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
        WireTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                when (val current = state) {
                    is SessionStartupUiState.Working -> {
                        if (current.showBlockingMigration) {
                            SettingUpWireScreenContent(
                                message = AnnotatedString(stringResource(R.string.migration_message_unknown)),
                            )
                        }
                    }

                    is SessionStartupUiState.Failed -> {
                        SettingUpWireScreenContent(
                            message = AnnotatedString(stringResource(R.string.migration_message_unknown)),
                            type = SettingUpWireScreenType.Failure(
                                buttonTextResId = R.string.label_retry.takeIf { current.failure.isRetryable },
                                onButtonClick = onRetry,
                            ),
                        )
                    }

                    SessionStartupUiState.ResolvingCurrentSession,
                    is SessionStartupUiState.Ready -> Unit
                }
            }
        }
    }
}
