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
package com.wire.android.ui.debug

import android.net.Uri
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.di.ScopedArgs
import com.wire.android.di.ViewModelScopedPreview
import com.wire.android.feature.analytics.AnonymousAnalyticsManagerImpl
import com.wire.android.feature.analytics.model.AnalyticsEvent
import com.wire.android.ui.home.settings.backup.BackupAndRestoreState
import com.wire.android.ui.home.settings.backup.BackupCreationProgress
import com.wire.android.util.FileManager
import com.wire.android.util.dispatchers.DefaultDispatcherProvider
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.feature.backup.CreateBackupResult
import com.wire.kalium.logic.feature.backup.CreateObfuscatedCopyUseCase
import com.wire.kalium.util.DelicateKaliumApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import javax.inject.Inject

@ViewModelScopedPreview
interface ExportObfuscatedCopyViewModel {

    val state: BackupAndRestoreState get() = BackupAndRestoreState.INITIAL_STATE

    val createBackupPasswordState: TextFieldState
        get() = TextFieldState()

    fun createObfuscatedCopy() {}
    fun shareCopy() {}
    fun saveCopy(uri: Uri) {}
    fun cancelBackupCreation() {}
}

@HiltViewModel
class ExportObfuscatedCopyViewModelImpl @OptIn(DelicateKaliumApi::class) @Inject constructor(
    private val createUnencryptedCopy: CreateObfuscatedCopyUseCase,
    private val dispatcher: DispatcherProvider = DefaultDispatcherProvider(),
    private val fileManager: FileManager,
) : ViewModel(), ExportObfuscatedCopyViewModel {

    override var state by mutableStateOf(BackupAndRestoreState.INITIAL_STATE)

    override val createBackupPasswordState: TextFieldState = TextFieldState()

    @VisibleForTesting
    internal var latestCreatedBackup: BackupAndRestoreState.CreatedBackup? = null

    @OptIn(DelicateKaliumApi::class)
    override fun createObfuscatedCopy() {
        viewModelScope.launch {

            when (val result = createUnencryptedCopy(null)) {
                is CreateBackupResult.Success -> {
                    state = state.copy(backupCreationProgress = BackupCreationProgress.Finished(result.backupFileName))
                    latestCreatedBackup = BackupAndRestoreState.CreatedBackup(
                        result.backupFilePath,
                        result.backupFileName,
                        false
                    )
                }

                is CreateBackupResult.Failure -> {
                    state = state.copy(backupCreationProgress = BackupCreationProgress.Failed)
                    appLogger.e("Failed to create backup: ${result.coreFailure}")
                    AnonymousAnalyticsManagerImpl.sendEvent(event = AnalyticsEvent.BackupExportFailed)
                }
            }
        }
    }

    override fun shareCopy() {
        viewModelScope.launch {
            latestCreatedBackup?.let { backupData ->
                withContext(dispatcher.io()) {
                    fileManager.shareWithExternalApp(backupData.path, backupData.assetName) {}
                }
            }
            state = state.copy(
                backupCreationProgress = BackupCreationProgress.InProgress(),
            )
        }
    }

    override fun saveCopy(uri: Uri) {
        viewModelScope.launch {
            latestCreatedBackup?.let { backupData ->
                fileManager.copyToUri(backupData.path, uri, dispatcher)
            }
            state = state.copy(
                backupCreationProgress = BackupCreationProgress.InProgress(),
            )
        }
    }

    override fun cancelBackupCreation() {
        viewModelScope.launch(dispatcher.main()) {
            createBackupPasswordState.clearText()
        }
    }
}

@Serializable
object ExportObfuscatedCopyArgs : ScopedArgs {
    override val key = "ExportObfuscatedCopyArgsKey"
}
