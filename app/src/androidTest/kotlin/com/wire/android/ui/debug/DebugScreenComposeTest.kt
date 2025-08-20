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

import androidx.compose.ui.test.junit4.createComposeRule
import com.wire.android.extensions.waitUntilExists
import com.wire.android.ui.WireTestTheme
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
                        onEnableWireCellsFeature = {},
                        onShowFeatureFlags = {},
                    )
            }
        }

        composeTestRule.waitUntilExists("Debug Settings")
    }
}
