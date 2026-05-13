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
package com.wire.android.feature.cells.ui

import com.wire.android.feature.cells.ui.edit.OnlineEditor
import com.wire.android.feature.cells.ui.search.SearchNavArgs
import com.wire.kalium.cells.domain.usecase.DeleteCellAssetUseCase
import com.wire.kalium.cells.domain.usecase.GetEditorUrlUseCase
import com.wire.kalium.cells.domain.usecase.GetPaginatedFilesFlowUseCase
import com.wire.kalium.cells.domain.usecase.GetWireCellConfigurationUseCase
import com.wire.kalium.cells.domain.usecase.IsAtLeastOneCellAvailableUseCase
import com.wire.kalium.cells.domain.usecase.RestoreNodeFromRecycleBinUseCase
import dev.zacsweers.metro.Inject

@Inject
@Suppress("LongParameterList")
class CellViewModelFactory(
    private val getCellFilesPaged: GetPaginatedFilesFlowUseCase,
    private val deleteCellAsset: DeleteCellAssetUseCase,
    private val restoreNodeFromRecycleBinUseCase: RestoreNodeFromRecycleBinUseCase,
    private val isCellAvailable: IsAtLeastOneCellAvailableUseCase,
    private val fileExternalActions: CellFileExternalActions,
    private val getEditorUrl: GetEditorUrlUseCase,
    private val onlineEditor: OnlineEditor,
    private val cellFileActionsMenu: CellFileActionsMenu,
    private val getWireCellsConfig: GetWireCellConfigurationUseCase,
    private val sharedPathCache: CellFileLocalPathCache,
    private val openFileDownloadController: OpenFileDownloadController,
) {
    fun create(
        navArgs: CellFilesNavArgs,
        searchNavArgs: SearchNavArgs?,
    ): CellViewModel =
        CellViewModel(
            navArgs = navArgs,
            searchNavArgs = searchNavArgs,
            getCellFilesPaged = getCellFilesPaged,
            deleteCellAsset = deleteCellAsset,
            restoreNodeFromRecycleBinUseCase = restoreNodeFromRecycleBinUseCase,
            isCellAvailable = isCellAvailable,
            fileExternalActions = fileExternalActions,
            getEditorUrl = getEditorUrl,
            onlineEditor = onlineEditor,
            cellFileActionsMenu = cellFileActionsMenu,
            getWireCellsConfig = getWireCellsConfig,
            sharedPathCache = sharedPathCache,
            openFileDownloadController = openFileDownloadController,
        )
}
