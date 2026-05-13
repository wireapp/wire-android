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
package com.wire.android.feature.cells.ui.movetofolder

import com.wire.kalium.cells.domain.usecase.GetFoldersUseCase
import com.wire.kalium.cells.domain.usecase.MoveNodeUseCase
import dev.zacsweers.metro.Inject

@Inject
class MoveToFolderViewModelFactory(
    private val getFoldersUseCase: GetFoldersUseCase,
    private val moveNodeUseCase: MoveNodeUseCase,
) {
    fun create(args: MoveToFolderNavArgs): MoveToFolderViewModel =
        MoveToFolderViewModel(
            navArgs = args,
            getFoldersUseCase = getFoldersUseCase,
            moveNodeUseCase = moveNodeUseCase,
        )
}
