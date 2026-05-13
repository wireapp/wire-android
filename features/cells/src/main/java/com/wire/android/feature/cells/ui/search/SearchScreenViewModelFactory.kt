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
package com.wire.android.feature.cells.ui.search

import com.wire.android.feature.cells.ui.CellFileLocalPathCache
import com.wire.kalium.cells.domain.usecase.GetAllTagsUseCase
import com.wire.kalium.cells.domain.usecase.GetOwnersUseCase
import com.wire.kalium.cells.domain.usecase.GetPaginatedCellConversationsFlowUseCase
import com.wire.kalium.cells.domain.usecase.GetPaginatedFilesFlowUseCase
import dev.zacsweers.metro.Inject

@Inject
class SearchScreenViewModelFactory(
    private val getAllTagsUseCase: GetAllTagsUseCase,
    private val getCellFilesPaged: GetPaginatedFilesFlowUseCase,
    private val getOwners: GetOwnersUseCase,
    private val getPaginatedConversations: GetPaginatedCellConversationsFlowUseCase,
    private val sharedPathCache: CellFileLocalPathCache,
) {
    fun create(navArgs: SearchNavArgs): SearchScreenViewModel =
        SearchScreenViewModel(
            navArgs = navArgs,
            getAllTagsUseCase = getAllTagsUseCase,
            getCellFilesPaged = getCellFilesPaged,
            getOwners = getOwners,
            getPaginatedConversations = getPaginatedConversations,
            sharedPathCache = sharedPathCache,
        )
}
