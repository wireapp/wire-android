/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.home.cell

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import com.wire.android.feature.cells.ui.AllFilesScreen
import com.wire.android.feature.cells.ui.AllFilesNavigationActions
import com.wire.android.feature.cells.ui.CellViewModel

/**
 * Navigation-neutral Global Cells renderer. [viewModel] is owned by the Home Navigation 3 entry,
 * preserving the existing sharing contract without looking up a Nav2 parent back-stack entry.
 */
@SuppressLint("ComposeViewModelForwarding")
@Composable
internal fun GlobalCellsScreen(
    navigationActions: AllFilesNavigationActions,
    viewModel: CellViewModel,
) {
    AllFilesScreen(
        navigationActions = navigationActions,
        viewModel = viewModel,
    )
}
