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

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import org.junit.Assert.assertNotSame
import org.junit.Rule
import org.junit.Test

class WireActivityNavigatorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenBlockingStateChanges_whenRememberingNavigator_thenNavigationStateIsDiscarded() {
        val isUserUiBlocked = mutableStateOf(false)
        lateinit var currentController: NavHostController

        composeTestRule.setContent {
            currentController = rememberWireActivityNavigator(
                isUserUiBlocked = isUserUiBlocked.value,
                finish = {},
                isAllowedToNavigate = { true },
            ).navController
        }

        lateinit var sessionController: NavHostController
        composeTestRule.runOnIdle {
            sessionController = currentController
            isUserUiBlocked.value = true
        }
        composeTestRule.waitForIdle()

        lateinit var blockedController: NavHostController
        composeTestRule.runOnIdle {
            blockedController = currentController
            isUserUiBlocked.value = false
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            assertNotSame(sessionController, blockedController)
            assertNotSame(blockedController, currentController)
            assertNotSame(sessionController, currentController)
        }
    }
}
