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

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wire.android.ui.WireTestActivity
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.theme.WireTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DebugScreenComposeTest {

    @get:Rule(order = 1)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<WireTestActivity>()

    @Before
    fun setup() {
        hiltRule.inject()

        composeTestRule.setContent {
            Surface {
                val snackbarHostState = remember { SnackbarHostState() }
                CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
                    WireTheme {
                        LocalSnackbarHostState.current
                        UserDebugContent(
                            onNavigationPressed = { },
                            onManualMigrationPressed = {},
                            state = UserDebugState(logPath = "logPath"),
                            onLoggingEnabledChange = {},
                            onDeleteLogs = {}
                        )
                    }
                }
            }
        }
    }

    @Test
    fun givenAUserIsInDebugScreen_TitleShouldBeDisplayed() = runTest {
        composeTestRule.onNodeWithText("Debug Settings").assertIsDisplayed()
    }
}
