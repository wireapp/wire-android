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

package com.wire.android.ui

import androidx.compose.ui.test.junit4.createComposeRule
import com.wire.kalium.logic.startup.MigrationProgress
import com.wire.kalium.logic.startup.StartupState
import org.junit.Rule
import org.junit.Test

class SessionStartupScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenBlockingMigrationIsVisible_whenScreenIsComposed_thenRequiredCompositionLocalsAreProvided() {
        composeTestRule.setContent {
            SessionStartupScreenContent(
                state = SessionStartupUiState.Working(
                    technicalState = StartupState.Migrating(
                        progress = MigrationProgress(MigrationProgress.Stage.UpdatingSchema)
                    ),
                    showBlockingMigration = true,
                ),
                onRetry = {},
            )
        }

        composeTestRule.waitForIdle()
    }
}
