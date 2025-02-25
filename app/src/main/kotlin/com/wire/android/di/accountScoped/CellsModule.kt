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
import com.wire.kalium.cells.CellsScope
import com.wire.kalium.cells.domain.CellUploadManager
import com.wire.kalium.cells.domain.usecase.AddAttachmentDraftUseCase
import com.wire.kalium.cells.domain.usecase.ObserveAttachmentDraftsUseCase
import com.wire.kalium.cells.domain.usecase.ObserveCellFilesUseCase
import com.wire.kalium.cells.domain.usecase.PublishAttachmentsUseCase
import com.wire.kalium.cells.domain.usecase.RemoveAttachmentDraftUseCase
import com.wire.kalium.cells.domain.usecase.SetWireCellForConversationUseCase
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
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
    fun provideCellsScope(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount accountId: UserId,
    ): CellsScope = coreLogic.getSessionScope(accountId).cells

    @ViewModelScoped
    @Provides
    fun provideAddAttachmentUseCase(cellsScope: CellsScope): AddAttachmentDraftUseCase = cellsScope.addAttachment

    @ViewModelScoped
    @Provides
    fun provideRemoveAttachmentUseCase(cellsScope: CellsScope): RemoveAttachmentDraftUseCase = cellsScope.removeAttachment

    @ViewModelScoped
    @Provides
    fun provideObserveAttachmentsUseCase(cellsScope: CellsScope): ObserveAttachmentDraftsUseCase = cellsScope.observeAttachments

    @ViewModelScoped
    @Provides
    fun providePublishAttachmentsUseCase(cellsScope: CellsScope): PublishAttachmentsUseCase = cellsScope.publishAttachments

    @ViewModelScoped
    @Provides
    fun provideCellUploadManager(cellsScope: CellsScope): CellUploadManager = cellsScope.uploadManager

    @ViewModelScoped
    @Provides
    fun provideObserveFilesUseCase(cellsScope: CellsScope): ObserveCellFilesUseCase = cellsScope.observeFiles

    @ViewModelScoped
    @Provides
    fun provideEnableCellUseCase(cellsScope: CellsScope): SetWireCellForConversationUseCase = cellsScope.enableWireCell
}
