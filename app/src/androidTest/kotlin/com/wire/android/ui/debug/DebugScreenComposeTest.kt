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
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.wire.android.extensions.waitUntilExists
import com.wire.android.ui.WireTestTheme
import kotlinx.coroutines.CompletableDeferred
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
                        onShowAiAssistantDebugOptions = {},
                        onFlushLogs = { CompletableDeferred(Unit) },
                        debugDataOptionsViewModel = object : DebugDataOptionsViewModel {},
                        exportObfuscatedCopyViewModel = object : ExportObfuscatedCopyViewModel {},
                    )
            }
        }

        composeTestRule.waitUntilExists("Debug Settings")
    }

    @Test
    fun givenPrivateBuild_DebugDataOptionsShouldShowAiAssistantDebugNavigation() = runTest {
        composeTestRule.setContent {
            WireTestTheme {
                DebugDataOptionsContent(
                    state = DebugDataOptionsState(),
                    appVersion = "1.0.0",
                    buildVariant = "devDebug",
                    onCopyText = {},
                    onDisableEventProcessingChange = {},
                    onEnableAsyncNotificationsChange = {},
                    onRestartSlowSyncForRecovery = {},
                    onForceUpdateApiVersions = {},
                    enrollE2EICertificate = {},
                    e2eiCertificateExpirationInputState = TextFieldState(),
                    handleE2EIEnrollmentResult = {},
                    dismissCertificateDialog = {},
                    checkCrlRevocationList = {},
                    forceCRLExpirationAfterOneMinute = false,
                    onForceCRLExpirationAfterOneMinuteChange = {},
                    onResendFCMToken = {},
                    onShowFeatureFlags = {},
                    onShowCryptoStats = {},
                    onRepairFaultyRemovalKeys = {},
                    onShowAiAssistantDebugOptions = {}
                )
            }
        }

        composeTestRule.waitUntilExists("AI assistant model")
        composeTestRule.onAllNodesWithText("Download").assertCountEquals(0)
    }

    @Test
    fun givenAiModelIsNotDownloaded_AiAssistantDebugScreenShouldShowDownloadButton() = runTest {
        composeTestRule.setContent {
            WireTestTheme {
                AiAssistantDebugScreenContent(
                    state = AiAssistantDebugState(
                        aiModelOptionState = AiModelOptionState(
                            status = AiModelUiStatus.NotDownloaded,
                            showDownloadButton = true,
                            isDownloading = false
                        ),
                        healthCheckState = AiModelHealthCheckState.Unavailable
                    ),
                    onNavigationPressed = {},
                    onDownloadAiModel = {},
                    onDownloadEmbeddingModel = {},
                    onModelSelected = {},
                    onDismissAuthorizationDialog = {},
                    onAuthorizeModelAccess = {}
                )
            }
        }

        composeTestRule.waitUntilExists("AI assistant model")
        composeTestRule.waitUntilExists("Embedding model")
        composeTestRule.waitUntilExists("Not downloaded")
        composeTestRule.onAllNodesWithText("Download").assertCountEquals(2)
        composeTestRule.waitUntilExists("Model health check")
        composeTestRule.waitUntilExists("Waiting for downloaded model")
    }

    @Test
    fun givenAiModelIsDownloading_AiAssistantDebugScreenShouldShowProgressAndDisabledButton() = runTest {
        composeTestRule.setContent {
            WireTestTheme {
                AiAssistantDebugScreenContent(
                    state = AiAssistantDebugState(
                        aiModelOptionState = AiModelOptionState(
                            status = AiModelUiStatus.Downloading(0.5F),
                            showDownloadButton = true,
                            isDownloading = true
                        ),
                        healthCheckState = AiModelHealthCheckState.Running
                    ),
                    onNavigationPressed = {},
                    onDownloadAiModel = {},
                    onDownloadEmbeddingModel = {},
                    onModelSelected = {},
                    onDismissAuthorizationDialog = {},
                    onAuthorizeModelAccess = {}
                )
            }
        }

        composeTestRule.waitUntilExists("Downloading 50%")
        composeTestRule.waitUntilExists("Checking")
        composeTestRule.onAllNodesWithText("Download")[0].assertIsNotEnabled()
    }

    @Test
    fun givenAiModelIsDownloaded_AiAssistantDebugScreenShouldHideDownloadButton() = runTest {
        composeTestRule.setContent {
            WireTestTheme {
                AiAssistantDebugScreenContent(
                    state = AiAssistantDebugState(
                        aiModelOptionState = AiModelOptionState(
                            status = AiModelUiStatus.Downloaded,
                            showDownloadButton = false,
                            isDownloading = false
                        ),
                        healthCheckState = AiModelHealthCheckState.Healthy
                    ),
                    onNavigationPressed = {},
                    onDownloadAiModel = {},
                    onDownloadEmbeddingModel = {},
                    onModelSelected = {},
                    onDismissAuthorizationDialog = {},
                    onAuthorizeModelAccess = {}
                )
            }
        }

        composeTestRule.waitUntilExists("Downloaded")
        composeTestRule.waitUntilExists("Healthy")
        composeTestRule.onAllNodesWithText("Download").assertCountEquals(1)
    }

    @Test
    fun givenEmbeddingModelIsNotDownloaded_AiAssistantDebugScreenShouldShowDownloadButton() = runTest {
        composeTestRule.setContent {
            WireTestTheme {
                AiAssistantDebugScreenContent(
                    state = AiAssistantDebugState(
                        aiModelOptionState = AiModelOptionState(
                            status = AiModelUiStatus.Downloaded,
                            showDownloadButton = false,
                            isDownloading = false
                        ),
                        embeddingModelOptionState = AiModelOptionState(
                            status = AiModelUiStatus.NotDownloaded,
                            showDownloadButton = true,
                            isDownloading = false
                        ),
                        healthCheckState = AiModelHealthCheckState.Healthy
                    ),
                    onNavigationPressed = {},
                    onDownloadAiModel = {},
                    onDownloadEmbeddingModel = {},
                    onModelSelected = {},
                    onDismissAuthorizationDialog = {},
                    onAuthorizeModelAccess = {}
                )
            }
        }

        composeTestRule.waitUntilExists("Embedding model")
        composeTestRule.waitUntilExists("Not downloaded")
        composeTestRule.onAllNodesWithText("Download").assertCountEquals(1)
    }

    @Test
    fun givenEmbeddingModelIsDownloading_AiAssistantDebugScreenShouldShowProgressAndDisabledButton() = runTest {
        composeTestRule.setContent {
            WireTestTheme {
                AiAssistantDebugScreenContent(
                    state = AiAssistantDebugState(
                        aiModelOptionState = AiModelOptionState(
                            status = AiModelUiStatus.Downloaded,
                            showDownloadButton = false,
                            isDownloading = false
                        ),
                        embeddingModelOptionState = AiModelOptionState(
                            status = AiModelUiStatus.Downloading(0.5F),
                            showDownloadButton = true,
                            isDownloading = true
                        ),
                        healthCheckState = AiModelHealthCheckState.Healthy
                    ),
                    onNavigationPressed = {},
                    onDownloadAiModel = {},
                    onDownloadEmbeddingModel = {},
                    onModelSelected = {},
                    onDismissAuthorizationDialog = {},
                    onAuthorizeModelAccess = {}
                )
            }
        }

        composeTestRule.waitUntilExists("Embedding model")
        composeTestRule.waitUntilExists("Downloading 50%")
        composeTestRule.onAllNodesWithText("Download")[0].assertIsNotEnabled()
    }

    @Test
    fun givenEmbeddingModelIsDownloaded_AiAssistantDebugScreenShouldHideDownloadButton() = runTest {
        composeTestRule.setContent {
            WireTestTheme {
                AiAssistantDebugScreenContent(
                    state = AiAssistantDebugState(
                        aiModelOptionState = AiModelOptionState(
                            status = AiModelUiStatus.Downloaded,
                            showDownloadButton = false,
                            isDownloading = false
                        ),
                        embeddingModelOptionState = AiModelOptionState(
                            status = AiModelUiStatus.Downloaded,
                            showDownloadButton = false,
                            isDownloading = false
                        ),
                        healthCheckState = AiModelHealthCheckState.Healthy
                    ),
                    onNavigationPressed = {},
                    onDownloadAiModel = {},
                    onDownloadEmbeddingModel = {},
                    onModelSelected = {},
                    onDismissAuthorizationDialog = {},
                    onAuthorizeModelAccess = {}
                )
            }
        }

        composeTestRule.waitUntilExists("Embedding model")
        composeTestRule.waitUntilExists("Downloaded")
        composeTestRule.onAllNodesWithText("Download").assertCountEquals(0)
    }

    @Test
    fun givenAiModelHealthCheckFails_AiAssistantDebugScreenShouldShowFailure() = runTest {
        composeTestRule.setContent {
            WireTestTheme {
                AiAssistantDebugScreenContent(
                    state = AiAssistantDebugState(
                        aiModelOptionState = AiModelOptionState(
                            status = AiModelUiStatus.Downloaded,
                            showDownloadButton = false,
                            isDownloading = false
                        ),
                        healthCheckState = AiModelHealthCheckState.Failed("Model returned an empty response")
                    ),
                    onNavigationPressed = {},
                    onDownloadAiModel = {},
                    onDownloadEmbeddingModel = {},
                    onModelSelected = {},
                    onDismissAuthorizationDialog = {},
                    onAuthorizeModelAccess = {}
                )
            }
        }

        composeTestRule.waitUntilExists("Model health check")
        composeTestRule.waitUntilExists("Failed: Model returned an empty response")
    }
}
