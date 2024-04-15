/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.di.CurrentAccount
import com.wire.android.migration.failure.UserMigrationStatus
import com.wire.android.util.getDeviceIdString
import com.wire.android.util.getGitBuildId
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.E2EIFailure
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.debug.DisableEventProcessingUseCase
import com.wire.kalium.logic.feature.e2ei.CheckCrlRevocationListUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.E2EIEnrollmentResult
import com.wire.kalium.logic.feature.keypackage.MLSKeyPackageCountResult
import com.wire.kalium.logic.feature.keypackage.MLSKeyPackageCountUseCase
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.functional.fold
import com.wire.kalium.logic.sync.periodic.UpdateApiVersionsScheduler
import com.wire.kalium.logic.sync.slow.RestartSlowSyncProcessForRecoveryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class DebugDataOptionsViewModel
@Inject constructor(
    @ApplicationContext private val context: Context,
    @CurrentAccount val currentAccount: UserId,
    private val globalDataStore: GlobalDataStore,
    private val updateApiVersions: UpdateApiVersionsScheduler,
    private val mlsKeyPackageCountUseCase: MLSKeyPackageCountUseCase,
    private val restartSlowSyncProcessForRecovery: RestartSlowSyncProcessForRecoveryUseCase,
    private val disableEventProcessingUseCase: DisableEventProcessingUseCase,
    private val checkCrlRevocationListUseCase: CheckCrlRevocationListUseCase
) : ViewModel() {

    var state by mutableStateOf(
        DebugDataOptionsState()
    )

    init {
        observeEncryptedProteusStorageState()
        observeMlsMetadata()
        checkIfCanTriggerManualMigration()
        setGitHashAndDeviceId()
    }

    private fun setGitHashAndDeviceId() {
        viewModelScope.launch {
            val deviceId = context.getDeviceIdString() ?: "null"
            val gitBuildId = context.getGitBuildId()
            state = state.copy(
                debugId = deviceId,
                commitish = gitBuildId
            )
        }
    }

    fun checkCrlRevocationList() {
        viewModelScope.launch {
            checkCrlRevocationListUseCase(
                forceUpdate = true
            )
        }
    }

    fun enableEncryptedProteusStorage(enabled: Boolean) {
        if (enabled) {
            viewModelScope.launch {
                globalDataStore.setEncryptedProteusStorageEnabled(true)
            }
        }
    }

    fun restartSlowSyncForRecovery() {
        viewModelScope.launch {
            restartSlowSyncProcessForRecovery()
        }
    }

    fun enrollE2EICertificate() {
        state = state.copy(startGettingE2EICertificate = true)
    }

    fun handleE2EIEnrollmentResult(result: Either<CoreFailure, E2EIEnrollmentResult>) {
        result.fold({
            state = state.copy(
                certificate = (it as E2EIFailure.OAuth).reason,
                showCertificate = true,
                startGettingE2EICertificate = false
            )
        }, {
            if (it is E2EIEnrollmentResult.Finalized) {
                state = state.copy(
                    certificate = it.certificate,
                    showCertificate = true,
                    startGettingE2EICertificate = false
                )
            } else {
                state.copy(
                    certificate = it.toString(),
                    showCertificate = true,
                    startGettingE2EICertificate = false
                )
            }
        })
    }

    fun dismissCertificateDialog() {
        state = state.copy(
            showCertificate = false,
        )
    }

    fun forceUpdateApiVersions() {
        updateApiVersions.scheduleImmediateApiVersionUpdate()
    }

    fun disableEventProcessing(disabled: Boolean) {
        viewModelScope.launch {
            disableEventProcessingUseCase(disabled)
            state = state.copy(isEventProcessingDisabled = disabled)
        }
    }

    //region Private
    private fun observeEncryptedProteusStorageState() {
        viewModelScope.launch {
            globalDataStore.isEncryptedProteusStorageEnabled().collect {
                state = state.copy(isEncryptedProteusStorageEnabled = it)
            }
        }
    }

    // If status is NoNeed, it means that the user has already been migrated in and older app version,
    // or it is a new install
    // this is why we check the existence of the database file
    private fun checkIfCanTriggerManualMigration() {
        viewModelScope.launch {
            globalDataStore.getUserMigrationStatus(currentAccount.value).first()
                .let { migrationStatus ->
                    if (migrationStatus != UserMigrationStatus.NoNeed) {
                        context.getDatabasePath(currentAccount.value).let {
                            state = state.copy(
                                isManualMigrationAllowed = (it.exists() && it.isFile)
                            )
                        }
                    }
                }
        }
    }

    private fun observeMlsMetadata() {
        viewModelScope.launch {
            mlsKeyPackageCountUseCase().let {
                when (it) {
                    is MLSKeyPackageCountResult.Success -> {
                        state = state.copy(
                            keyPackagesCount = it.count,
                            mslClientId = it.clientId.value
                        )
                    }

                    is MLSKeyPackageCountResult.Failure.NetworkCallFailure -> {
                        state = state.copy(mlsErrorMessage = "Network Error!")
                    }

                    is MLSKeyPackageCountResult.Failure.FetchClientIdFailure -> {
                        state = state.copy(mlsErrorMessage = "ClientId Fetch Error!")
                    }

                    is MLSKeyPackageCountResult.Failure.Generic -> {}
                }
            }
        }
    }
    //endregion
}
//endregion
