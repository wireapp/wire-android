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
package com.wire.android.feature.cells.ui.publiclink

import com.wire.android.feature.cells.util.FileHelper
import com.wire.kalium.cells.domain.usecase.publiclink.CreatePublicLinkUseCase
import com.wire.kalium.cells.domain.usecase.publiclink.DeletePublicLinkUseCase
import com.wire.kalium.cells.domain.usecase.publiclink.GetPublicLinkUseCase
import dev.zacsweers.metro.Inject

@Inject
class PublicLinkViewModelFactory(
    private val createPublicLink: CreatePublicLinkUseCase,
    private val getPublicLinkUseCase: GetPublicLinkUseCase,
    private val deletePublicLinkUseCase: DeletePublicLinkUseCase,
    private val fileHelper: FileHelper,
) {
    fun create(navArgs: PublicLinkNavArgs): PublicLinkViewModel =
        PublicLinkViewModel(
            navArgs = navArgs,
            createPublicLink = createPublicLink,
            getPublicLinkUseCase = getPublicLinkUseCase,
            deletePublicLinkUseCase = deletePublicLinkUseCase,
            fileHelper = fileHelper,
        )
}
