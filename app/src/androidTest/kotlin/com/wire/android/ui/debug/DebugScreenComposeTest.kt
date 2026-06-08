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

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.test.junit4.createComposeRule
import com.wire.android.extensions.waitUntilExists
import com.wire.android.ui.debug.automaticbackups.AutomaticBackupsDebugContent
import com.wire.android.ui.debug.automaticbackups.AutomaticBackupsDebugState
import com.wire.android.ui.WireTestTheme
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.feature.backup.BackupRootKeyInfo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.datetime.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class DebugScreenComposeTest {

    @get:Rule
    val composeTestRule by lazy { createComposeRule() }

    @Test
    fun givenAUserIsInDebugScreen_TitleShouldBeDisplayed() = runTest {
        composeTestRule.setContent {
                WireTestTheme {
                    UserDebugContent(
                        onNavigationPressed = { },
                        state = UserDebugState(logPath = "logPath"),
                        onLoggingEnabledChange = {},
                        onDeleteLogs = {},
                        onDatabaseLoggerEnabledChanged = {},
                        onShowFeatureFlags = {},
                        onShowCryptoStats = {},
                        onShowAutomaticBackups = {},
                        onFlushLogs = { CompletableDeferred(Unit) },
                        debugDataOptionsViewModel = object : DebugDataOptionsViewModel {},
                        exportObfuscatedCopyViewModel = object : ExportObfuscatedCopyViewModel {},
                    )
            }
        }

        composeTestRule.waitUntilExists("Debug Settings")
    }

    @Test
    fun givenAUserIsInDebugScreen_AutomaticBackupsShouldBeDisplayed() = runTest {
        composeTestRule.setContent {
            WireTestTheme {
                UserDebugContent(
                    onNavigationPressed = { },
                    state = UserDebugState(logPath = "logPath"),
                    onLoggingEnabledChange = {},
                    onDeleteLogs = {},
                    onDatabaseLoggerEnabledChanged = {},
                    onShowFeatureFlags = {},
                    onShowCryptoStats = {},
                    onShowAutomaticBackups = {},
                    onFlushLogs = { CompletableDeferred(Unit) },
                    debugDataOptionsViewModel = object : DebugDataOptionsViewModel {},
                    exportObfuscatedCopyViewModel = object : ExportObfuscatedCopyViewModel {},
                )
            }
        }

        composeTestRule.waitUntilExists("Automatic backups")
    }

    @Test
    fun givenExistingBackupRootKey_AutomaticBackupsScreenShouldDisplayKeyInfo() = runTest {
        composeTestRule.setContent {
            WireTestTheme {
                AutomaticBackupsDebugContent(
                    state = AutomaticBackupsDebugState(
                        backupRootKey = BackupRootKeyInfo(
                            id = "key-id",
                            fingerprint = "AA:BB:CC:DD",
                            createdAt = Instant.parse("2026-06-06T12:00:00Z"),
                            createdByClientId = ClientId("client-id"),
                            version = 1,
                        )
                    ),
                    exportBackupRootKeyPasswordTextState = TextFieldState(),
                    onNavigationPressed = {},
                    onFetchBackupRootKey = {},
                    onGenerateNewKey = {},
                    onShowExportBackupRootKeyPasswordDialog = {},
                    onDismissExportBackupRootKeyPasswordDialog = {},
                    onExportBackupRootKey = {},
                    onCreateBackup = {},
                    onRestoreBackup = {},
                )
            }
        }

        composeTestRule.waitUntilExists("Automatic backups")
        composeTestRule.waitUntilExists("AA:BB:CC:DD")
        composeTestRule.waitUntilExists("Generate new key")
        composeTestRule.waitUntilExists("Create backup")
    }

    @Test
    fun givenBackupCreationInProgress_AutomaticBackupsScreenShouldDisplayProgress() = runTest {
        composeTestRule.setContent {
            WireTestTheme {
                AutomaticBackupsDebugContent(
                    state = AutomaticBackupsDebugState(
                        isCreatingBackup = true,
                        backupCreationProgress = 0.42f,
                    ),
                    exportBackupRootKeyPasswordTextState = TextFieldState(),
                    onNavigationPressed = {},
                    onFetchBackupRootKey = {},
                    onGenerateNewKey = {},
                    onShowExportBackupRootKeyPasswordDialog = {},
                    onDismissExportBackupRootKeyPasswordDialog = {},
                    onExportBackupRootKey = {},
                    onCreateBackup = {},
                    onRestoreBackup = {},
                )
            }
        }

        composeTestRule.waitUntilExists("Create backup")
        composeTestRule.waitUntilExists("Progress: 42%")
    }
}
