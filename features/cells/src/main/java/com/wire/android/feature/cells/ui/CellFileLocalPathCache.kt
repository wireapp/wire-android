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

import com.wire.android.feature.cells.ui.model.CellNodeUi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A simple in-memory cache to store local file paths for cell nodes.
 * It also provides a channel to emit events when a file is ready to be opened.
 */
@Singleton
class CellFileLocalPathCache @Inject constructor() {

    private val _fileReadyChannel = Channel<CellNodeUi.File>(Channel.BUFFERED)
    val fileReadyEvents: Flow<CellNodeUi.File> = _fileReadyChannel.receiveAsFlow()

    private val _paths = MutableStateFlow<Map<String, String>>(emptyMap())
    val paths: StateFlow<Map<String, String>> = _paths.asStateFlow()

    fun put(uuid: String, localPath: String) {
        _paths.update { it + (uuid to localPath) }
    }

    fun emitFileReady(file: CellNodeUi.File) {
        _fileReadyChannel.trySend(file)
    }
}
