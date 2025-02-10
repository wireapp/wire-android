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
package com.wire.android.di.accountScoped

import com.wire.android.di.KaliumCellsScope
import com.wire.kalium.cells.CellsScope
import com.wire.kalium.cells.domain.CellUploadManager
import com.wire.kalium.cells.domain.usecase.CancelDraftUseCase
import com.wire.kalium.cells.domain.usecase.DeleteCellFileUseCase
import com.wire.kalium.cells.domain.usecase.GetCellFilesUseCase
import com.wire.kalium.cells.domain.usecase.PublishDraftUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
class CellsModule {

    @ViewModelScoped
    @Provides
    fun provideListCellFilesUseCase(@KaliumCellsScope cellsScope: CellsScope): GetCellFilesUseCase = cellsScope.getCellFiles

    @ViewModelScoped
    @Provides
    fun provideDeleteCellFileUseCase(@KaliumCellsScope cellsScope: CellsScope): DeleteCellFileUseCase = cellsScope.deleteFromCell

    @ViewModelScoped
    @Provides
    fun providePublishDraftUseCase(@KaliumCellsScope cellsScope: CellsScope): PublishDraftUseCase = cellsScope.publishDraft

    @ViewModelScoped
    @Provides
    fun provideCancelDraftUseCase(@KaliumCellsScope cellsScope: CellsScope): CancelDraftUseCase = cellsScope.cancelDraft

    @ViewModelScoped
    @Provides
    fun provideCellUploadManager(@KaliumCellsScope cellsScope: CellsScope): CellUploadManager = cellsScope.uploadManager
}
