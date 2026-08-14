/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.feature.cells.ui

import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.feature.cells.ui.publiclink.PublicLinkScreenData

/**
 * Semantic navigation boundary for the Global Cells root.
 *
 * It deliberately uses feature models instead of generated destinations, which lets the Home
 * owner keep [CellViewModel] alive while the destination implementation migrates independently.
 */
data class AllFilesNavigationActions(
    val openSearch: () -> Unit,
    val showPublicLink: (PublicLinkScreenData) -> Unit,
    val showAddRemoveTags: (CellNodeUi) -> Unit,
    val showImageViewer: (CellNodeUi.File) -> Unit,
    val showVideoPlayer: (CellNodeUi.File) -> Unit,
    val showAudioPlayer: (CellNodeUi.File) -> Unit,
)
