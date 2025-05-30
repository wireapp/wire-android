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
package com.wire.android.ui.sharing

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.wire.android.ui.common.bottomsheet.WireModalSheetState
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.common.search.SearchBarState
import com.wire.android.ui.common.search.rememberSearchbarState
import kotlinx.coroutines.CoroutineScope

@Composable
fun rememberImportMediaScreenState(
    searchBarState: SearchBarState = rememberSearchbarState(),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    bottomSheetState: WireModalSheetState<Unit> = rememberWireModalSheetState()
): ImportMediaScreenState {
    val snackbarHostState = LocalSnackbarHostState.current

    return ImportMediaScreenState(
        searchBarState = searchBarState,
        coroutineScope = coroutineScope,
        bottomSheetState = bottomSheetState,
        snackbarHostState = snackbarHostState
    )
}

class ImportMediaScreenState(
    val snackbarHostState: SnackbarHostState,
    val searchBarState: SearchBarState,
    val coroutineScope: CoroutineScope,
    val bottomSheetState: WireModalSheetState<Unit>
)
