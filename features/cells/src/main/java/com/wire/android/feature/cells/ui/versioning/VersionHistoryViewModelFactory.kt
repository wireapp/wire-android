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
package com.wire.android.feature.cells.ui.versioning

import com.wire.android.feature.cells.ui.edit.OnlineEditor
import com.wire.android.feature.cells.util.FileHelper
import com.wire.android.util.FileSizeFormatter
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.cells.domain.usecase.GetEditorUrlUseCase
import com.wire.kalium.cells.domain.usecase.download.DownloadCellVersionUseCase
import com.wire.kalium.cells.domain.usecase.versioning.GetNodeVersionsUseCase
import com.wire.kalium.cells.domain.usecase.versioning.RestoreNodeVersionUseCase
import dev.zacsweers.metro.Inject

@Inject
class VersionHistoryViewModelFactory(
    private val getNodeVersionsUseCase: GetNodeVersionsUseCase,
    private val fileSizeFormatter: FileSizeFormatter,
    private val restoreNodeVersionUseCase: RestoreNodeVersionUseCase,
    private val downloadCellVersionUseCase: DownloadCellVersionUseCase,
    private val fileHelper: FileHelper,
    private val onlineEditor: OnlineEditor,
    private val getEditorUrl: GetEditorUrlUseCase,
    private val dispatchers: DispatcherProvider,
) {
    fun create(navArgs: VersionHistoryNavArgs): VersionHistoryViewModel =
        VersionHistoryViewModel(
            navArgs = navArgs,
            getNodeVersionsUseCase = getNodeVersionsUseCase,
            fileSizeFormatter = fileSizeFormatter,
            restoreNodeVersionUseCase = restoreNodeVersionUseCase,
            downloadCellVersionUseCase = downloadCellVersionUseCase,
            fileHelper = fileHelper,
            onlineEditor = onlineEditor,
            getEditorUrl = getEditorUrl,
            dispatchers = dispatchers,
        )
}
