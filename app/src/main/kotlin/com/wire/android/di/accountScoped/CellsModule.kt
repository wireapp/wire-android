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

import com.wire.android.di.CurrentAccount
import com.wire.android.di.KaliumCoreLogic
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.cells.CellsScope
import com.wire.kalium.logic.feature.cells.usecase.CancelDraftUseCase
import com.wire.kalium.logic.feature.cells.usecase.DeleteCellFileUseCase
import com.wire.kalium.logic.feature.cells.usecase.GetCellFilesUseCase
import com.wire.kalium.logic.feature.cells.usecase.PublishDraftUseCase
import com.wire.kalium.logic.feature.cells.usecase.UploadToCellUseCase
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
    fun provideCellScopeProvider(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): CellsScope = coreLogic.getSessionScope(currentAccount).cells

    @ViewModelScoped
    @Provides
    fun provideListCellFilesUseCase(cellsScope: CellsScope): GetCellFilesUseCase = cellsScope.getCellFiles

    @ViewModelScoped
    @Provides
    fun provideUploadFileUseCase(cellsScope: CellsScope): UploadToCellUseCase = cellsScope.uploadToCell

    @ViewModelScoped
    @Provides
    fun provideDeleteCellFileUseCase(cellsScope: CellsScope): DeleteCellFileUseCase = cellsScope.deleteFromCell

    @ViewModelScoped
    @Provides
    fun providePublishDraftUseCase(cellsScope: CellsScope): PublishDraftUseCase = cellsScope.publishDraft

    @ViewModelScoped
    @Provides
    fun provideCancelDraftUseCase(cellsScope: CellsScope): CancelDraftUseCase = cellsScope.cancelDraft
}
