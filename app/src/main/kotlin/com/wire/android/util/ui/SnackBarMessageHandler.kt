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
package com.wire.android.util.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.wire.android.model.SnackBarMessage
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import kotlinx.coroutines.flow.SharedFlow

@Composable
fun SnackBarMessageHandler(
    infoMessages: SharedFlow<SnackBarMessage>,
    onEmitted: () -> Unit = {},
    onActionClicked: (SnackBarMessage) -> Unit = {},
) {
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current

    LaunchedEffect(Unit) {
        infoMessages.collect {
            onEmitted()
            snackbarHostState.showSnackbar(
                message = it.uiText.asString(context.resources),
                actionLabel = it.actionLabel?.asString(context.resources),
                duration = if (it.actionLabel == null) SnackbarDuration.Short else SnackbarDuration.Long
            ).let { snackbarResult: SnackbarResult ->
                if (snackbarResult == SnackbarResult.ActionPerformed) {
                    onActionClicked(it)
                }
            }
        }
    }
}
