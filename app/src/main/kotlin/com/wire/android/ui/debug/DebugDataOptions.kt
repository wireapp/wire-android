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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.di.CurrentAccount
import com.wire.android.migration.failure.UserMigrationStatus
import com.wire.android.model.Clickable
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.WireSwitch
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.e2eiEnrollment.GetE2EICertificateUI
import com.wire.android.ui.home.conversationslist.common.FolderHeader
import com.wire.android.ui.home.settings.SettingsItem
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.getDeviceIdString
import com.wire.android.util.getGitBuildId
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.E2EIFailure
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.debug.DisableEventProcessingUseCase
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

//region DebugDataOptionsViewModel
data class DebugDataOptionsState(
    val isEncryptedProteusStorageEnabled: Boolean = false,
    val isEventProcessingDisabled: Boolean = false,
    val keyPackagesCount: Int = 0,
    val mslClientId: String = "null",
    val mlsErrorMessage: String = "null",
    val isManualMigrationAllowed: Boolean = false,
    val debugId: String = "null",
    val commitish: String = "null",
    val certificate: String = "null",
    val showCertificate: Boolean = false,
    val startGettingE2EICertificate: Boolean = false
)

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
) : ViewModel() {

    var state by mutableStateOf(
        DebugDataOptionsState()
    )

    init {
        observeEncryptedProteusStorageState()
        observeMlsMetadata()
        checkIfCanTriggerManualMigration()
        state = state.copy(
            debugId = context.getDeviceIdString() ?: "null",
            commitish = context.getGitBuildId()
        )
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

@Composable
fun DebugDataOptions(
    viewModel: DebugDataOptionsViewModel = hiltViewModel(),
    appVersion: String,
    buildVariant: String,
    onCopyText: (String) -> Unit,
    onManualMigrationPressed: (currentAccount: UserId) -> Unit
) {
    DebugDataOptionsContent(
        state = viewModel.state,
        appVersion = appVersion,
        buildVariant = buildVariant,
        onCopyText = onCopyText,
        onEnableEncryptedProteusStorageChange = viewModel::enableEncryptedProteusStorage,
        onRestartSlowSyncForRecovery = viewModel::restartSlowSyncForRecovery,
        onForceUpdateApiVersions = viewModel::forceUpdateApiVersions,
        onManualMigrationPressed = { onManualMigrationPressed(viewModel.currentAccount) },
        onDisableEventProcessingChange = viewModel::disableEventProcessing,
        enrollE2EICertificate = viewModel::enrollE2EICertificate,
        handleE2EIEnrollmentResult = viewModel::handleE2EIEnrollmentResult,
        dismissCertificateDialog = viewModel::dismissCertificateDialog
    )
}

@Suppress("LongParameterList")
@Composable
fun DebugDataOptionsContent(
    state: DebugDataOptionsState,
    appVersion: String,
    buildVariant: String,
    onCopyText: (String) -> Unit,
    onEnableEncryptedProteusStorageChange: (Boolean) -> Unit,
    onDisableEventProcessingChange: (Boolean) -> Unit,
    onRestartSlowSyncForRecovery: () -> Unit,
    onForceUpdateApiVersions: () -> Unit,
    onManualMigrationPressed: () -> Unit,
    enrollE2EICertificate: () -> Unit,
    handleE2EIEnrollmentResult: (Either<CoreFailure, E2EIEnrollmentResult>) -> Unit,
    dismissCertificateDialog: () -> Unit
) {
    Column {

        FolderHeader(stringResource(R.string.label_debug_data))

        SettingsItem(
            title = stringResource(R.string.app_version),
            text = appVersion,
            trailingIcon = R.drawable.ic_copy,
            onIconPressed = Clickable(
                enabled = true,
                onClick = { onCopyText(appVersion) }
            )
        )

        SettingsItem(
            title = stringResource(R.string.build_variant_name),
            text = buildVariant,
            trailingIcon = R.drawable.ic_copy,
            onIconPressed = Clickable(
                enabled = true,
                onClick = { onCopyText(buildVariant) }
            )
        )

        SettingsItem(
            title = stringResource(R.string.label_code_commit_id),
            text = state.commitish,
            trailingIcon = R.drawable.ic_copy,
            onIconPressed = Clickable(
                enabled = true,
                onClick = { onCopyText(state.commitish) }
            )
        )
        if (BuildConfig.PRIVATE_BUILD) {

            SettingsItem(
                title = stringResource(R.string.debug_id),
                text = state.debugId,
                trailingIcon = R.drawable.ic_copy,
                onIconPressed = Clickable(
                    enabled = true,
                    onClick = { onCopyText(state.debugId) }
                )
            )
            if (BuildConfig.DEBUG) {
                GetE2EICertificateSwitch(
                    enrollE2EI = enrollE2EICertificate
                )

                if (state.showCertificate) {
                    WireDialog(
                        title = stringResource(R.string.end_to_end_identity_ceritifcate),
                        text = state.certificate,
                        onDismiss = {
                            dismissCertificateDialog()
                        },
                        optionButton1Properties = WireDialogButtonProperties(
                            onClick = {
                                dismissCertificateDialog()
                            },
                            text = stringResource(R.string.label_ok),
                            type = WireDialogButtonType.Primary,
                        )
                    )
                }
            }
            ProteusOptions(
                isEncryptedStorageEnabled = state.isEncryptedProteusStorageEnabled,
                onEncryptedStorageEnabledChange = onEnableEncryptedProteusStorageChange
            )
            if (BuildConfig.DEBUG) {
                MLSOptions(
                    keyPackagesCount = state.keyPackagesCount,
                    mlsClientId = state.mslClientId,
                    mlsErrorMessage = state.mlsErrorMessage,
                    restartSlowSyncForRecovery = onRestartSlowSyncForRecovery,
                    onCopyText = onCopyText
                )
            }

            DebugToolsOptions(
                isEventProcessingEnabled = state.isEventProcessingDisabled,
                onDisableEventProcessingChange = onDisableEventProcessingChange,
                onRestartSlowSyncForRecovery = onRestartSlowSyncForRecovery,
                onForceUpdateApiVersions = onForceUpdateApiVersions
            )
        }

        if (state.isManualMigrationAllowed) {
            FolderHeader("Other Debug Options")
            ManualMigrationOptions(
                onManualMigrationClicked = onManualMigrationPressed
            )
        }

        if (state.startGettingE2EICertificate) {
            GetE2EICertificateUI(
                enrollmentResultHandler = { handleE2EIEnrollmentResult(it) },
                isNewClient = false
            )
        }
    }
}

@Composable
private fun GetE2EICertificateSwitch(
    enrollE2EI: () -> Unit
) {
    Column {
        FolderHeader(stringResource(R.string.debug_settings_e2ei_enrollment_title))
        RowItemTemplate(modifier = Modifier.wrapContentWidth(),
            title = {
                Text(
                    style = MaterialTheme.wireTypography.body01,
                    color = MaterialTheme.wireColorScheme.onBackground,
                    text = stringResource(R.string.label_get_e2ei_cetificate),
                    modifier = Modifier.padding(start = dimensions().spacing8x)
                )
            },
            actions = {
                WirePrimaryButton(
                    onClick = {
                        enrollE2EI()
                    },
                    text = stringResource(R.string.label_get_e2ei_cetificate),
                    fillMaxWidth = false
                )
            }
        )
    }
}

//region Scala Migration Options
@Composable
private fun ManualMigrationOptions(
    onManualMigrationClicked: () -> Unit,
) {
    RowItemTemplate(
        modifier = Modifier.wrapContentWidth(),
        title = {
            Text(
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.onBackground,
                text = stringResource(R.string.label_manual_migration_title),
                modifier = Modifier.padding(start = dimensions().spacing8x)
            )
        },
        actions = {
            WirePrimaryButton(
                minSize = MaterialTheme.wireDimensions.buttonMediumMinSize,
                minClickableSize = MaterialTheme.wireDimensions.buttonMinClickableSize,
                onClick = onManualMigrationClicked,
                text = stringResource(R.string.start_manual_migration),
                fillMaxWidth = false
            )
        }
    )
}
//endregion

//region MLS Options
@Composable
private fun MLSOptions(
    keyPackagesCount: Int,
    mlsClientId: String,
    mlsErrorMessage: String,
    onCopyText: (String) -> Unit,
    restartSlowSyncForRecovery: () -> Unit
) {
    FolderHeader(stringResource(R.string.label_mls_option_title))
    Column {
        SettingsItem(
            title = "Error Message",
            text = mlsErrorMessage,
            trailingIcon = R.drawable.ic_copy,
            onIconPressed = Clickable(
                enabled = true,
                onClick = restartSlowSyncForRecovery
            )
        )
        SettingsItem(
            title = stringResource(R.string.label_key_packages_count),
            text = keyPackagesCount.toString(),
            trailingIcon = R.drawable.ic_copy,
            onIconPressed = Clickable(
                enabled = true,
                onClick = { onCopyText(keyPackagesCount.toString()) }
            )
        )
        SettingsItem(
            title = stringResource(R.string.label_mls_client_id),
            text = mlsClientId,
            trailingIcon = R.drawable.ic_copy,
            onIconPressed = Clickable(
                enabled = true,
                onClick = { onCopyText(mlsClientId) }
            )
        )
    }
}
//endregion

//region Proteus Options
@Composable
private fun ProteusOptions(
    isEncryptedStorageEnabled: Boolean,
    onEncryptedStorageEnabledChange: (Boolean) -> Unit,
) {
    FolderHeader(stringResource(R.string.label_proteus_option_title))
    EnableEncryptedProteusStorageSwitch(
        isEnabled = isEncryptedStorageEnabled,
        onCheckedChange = onEncryptedStorageEnabledChange
    )
}

@Composable
private fun EnableEncryptedProteusStorageSwitch(
    isEnabled: Boolean = false,
    onCheckedChange: ((Boolean) -> Unit)?,
) {
    RowItemTemplate(
        title = {
            Text(
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.onBackground,
                text = stringResource(R.string.label_enable_encrypted_proteus_storage),
                modifier = Modifier.padding(start = dimensions().spacing8x)
            )
        },
        actions = {
            WireSwitch(
                checked = isEnabled,
                onCheckedChange = onCheckedChange,
                enabled = !isEnabled,
                modifier = Modifier
                    .padding(end = dimensions().spacing8x)
                    .size(
                        width = dimensions().buttonSmallMinSize.width,
                        height = dimensions().buttonSmallMinSize.height
                    )
            )
        }
    )
}
//endregion

//region Debug Tools
@Composable
private fun DebugToolsOptions(
    isEventProcessingEnabled: Boolean,
    onDisableEventProcessingChange: (Boolean) -> Unit,
    onRestartSlowSyncForRecovery: () -> Unit,
    onForceUpdateApiVersions: () -> Unit
) {
    FolderHeader(stringResource(R.string.label_debug_tools_title))
    Column {
        DisableEventProcessingSwitch(
            isEnabled = isEventProcessingEnabled,
            onCheckedChange = onDisableEventProcessingChange
        )
        RowItemTemplate(
            modifier = Modifier.wrapContentWidth(),
            title = {
                Text(
                    style = MaterialTheme.wireTypography.body01,
                    color = MaterialTheme.wireColorScheme.onBackground,
                    text = stringResource(R.string.label_restart_slowsync_for_recovery),
                    modifier = Modifier.padding(start = dimensions().spacing8x)
                )
            },
            actions = {
                WirePrimaryButton(
                    minSize = MaterialTheme.wireDimensions.buttonMediumMinSize,
                    minClickableSize = MaterialTheme.wireDimensions.buttonMinClickableSize,
                    onClick = onRestartSlowSyncForRecovery,
                    text = stringResource(R.string.restart_slowsync_for_recovery_button),
                    fillMaxWidth = false
                )
            }
        )
        RowItemTemplate(
            modifier = Modifier.wrapContentWidth(),
            title = {
                Text(
                    style = MaterialTheme.wireTypography.body01,
                    color = MaterialTheme.wireColorScheme.onBackground,
                    text = stringResource(R.string.debug_settings_force_api_versioning_update),
                    modifier = Modifier.padding(start = dimensions().spacing8x)
                )
            },
            actions = {
                WirePrimaryButton(
                    minSize = MaterialTheme.wireDimensions.buttonMediumMinSize,
                    minClickableSize = MaterialTheme.wireDimensions.buttonMinClickableSize,
                    onClick = onForceUpdateApiVersions,
                    text = stringResource(R.string.debug_settings_force_api_versioning_update_button_text),
                    fillMaxWidth = false
                )
            }
        )
    }
}

@Composable
private fun DisableEventProcessingSwitch(
    isEnabled: Boolean = false,
    onCheckedChange: ((Boolean) -> Unit)?,
) {
    RowItemTemplate(
        title = {
            Text(
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.onBackground,
                text = stringResource(R.string.label_disable_event_processing),
                modifier = Modifier.padding(start = dimensions().spacing8x)
            )
        },
        actions = {
            WireSwitch(
                checked = isEnabled,
                onCheckedChange = onCheckedChange,
                modifier = Modifier
                    .padding(end = dimensions().spacing8x)
                    .size(
                        width = dimensions().buttonSmallMinSize.width,
                        height = dimensions().buttonSmallMinSize.height
                    )
            )
        }
    )
}
//endregion

@PreviewMultipleThemes
@Composable
fun PreviewOtherDebugOptions() {
    DebugDataOptionsContent(
        appVersion = "1.0.0",
        buildVariant = "debug",
        onCopyText = {},
        state = DebugDataOptionsState(
            isEncryptedProteusStorageEnabled = true,
            keyPackagesCount = 10,
            mslClientId = "clientId",
            mlsErrorMessage = "error",
            isManualMigrationAllowed = true,
            debugId = "debugId",
            commitish = "commitish"
        ),
        onEnableEncryptedProteusStorageChange = {},
        onForceUpdateApiVersions = {},
        onDisableEventProcessingChange = {},
        onRestartSlowSyncForRecovery = {},
        onManualMigrationPressed = {},
        enrollE2EICertificate = {},
        handleE2EIEnrollmentResult = {},
        dismissCertificateDialog = {},
    )
}
