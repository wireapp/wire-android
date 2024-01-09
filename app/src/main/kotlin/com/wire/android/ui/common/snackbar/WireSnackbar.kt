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
package com.wire.android.ui.common.snackbar

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.wire.android.util.ui.UIText
import kotlinx.coroutines.flow.SharedFlow

@Composable
fun SnackbarHostState.collectAndShowSnackbar(
    snackbarFlow: SharedFlow<UIText>
) {
    val localContext = LocalContext.current

    LaunchedEffect(snackbarFlow) {
        snackbarFlow.collect {
            showSnackbar(it.asString(localContext.resources))
        }
    }
}
