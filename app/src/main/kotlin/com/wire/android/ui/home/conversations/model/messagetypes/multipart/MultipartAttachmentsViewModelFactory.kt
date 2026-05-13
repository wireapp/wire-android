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
package com.wire.android.ui.home.conversations.model.messagetypes.multipart

import com.wire.android.feature.cells.ui.edit.OnlineEditor
import com.wire.android.util.FileManager
import com.wire.kalium.cells.domain.usecase.GetEditorUrlUseCase
import com.wire.kalium.cells.domain.usecase.GetWireCellConfigurationUseCase
import com.wire.kalium.cells.domain.usecase.download.DownloadCellFileUseCase
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.featureFlags.KaliumConfigs
import dev.zacsweers.metro.Inject

@Inject
@Suppress("LongParameterList")
class MultipartAttachmentsViewModelFactory(
    private val refreshHelper: CellAssetRefreshHelper,
    private val download: DownloadCellFileUseCase,
    private val getEditorUrl: GetEditorUrlUseCase,
    private val onlineEditor: OnlineEditor,
    private val fileManager: FileManager,
    private val kaliumFileSystem: KaliumFileSystem,
    private val featureFlags: KaliumConfigs,
    private val getWireCellsConfig: GetWireCellConfigurationUseCase,
) {
    fun create(): MultipartAttachmentsViewModelImpl = MultipartAttachmentsViewModelImpl(
        refreshHelper = refreshHelper,
        download = download,
        getEditorUrl = getEditorUrl,
        onlineEditor = onlineEditor,
        fileManager = fileManager,
        kaliumFileSystem = kaliumFileSystem,
        featureFlags = featureFlags,
        getWireCellsConfig = getWireCellsConfig,
    )
}
