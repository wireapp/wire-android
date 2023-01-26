/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui

import android.content.Intent
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.wire.android.ui.authentication.devices.register.RegisterDeviceScreen
import com.wire.android.ui.theme.WireTheme
import com.wire.android.utils.PASSWORD
import com.wire.android.utils.WorkManagerTestRule
import com.wire.android.utils.waitForExecution
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class,
    ExperimentalComposeUiApi::class, ExperimentalMaterialApi::class
)
@HiltAndroidTest
class RegisterDeviceTest {

    // Order matters =(
    // First, we need hilt to be started
    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    // Second, as we are using a WorkManager
    // In an instrumented test we need to ensure this gets initialized before launching any Compose/Activity Rule
    @get:Rule(order = 1)
    var workManagerTestRule = WorkManagerTestRule()

    // Third, we create the compose rule using an AndroidComposeRule, as we are depending on instrumented environment ie: Hilt, WorkManager
    @get:Rule(order = 2)
    val composeTestRule = createEmptyComposeRule()

    private lateinit var scenario: ActivityScenario<WireActivity>

    @Before
    fun setUp() {
        hiltRule.inject()

        // Start the app
        scenario = ActivityScenario.launch(Intent(ApplicationProvider.getApplicationContext(), WireActivity::class.java))
        scenario.onActivity { activity ->
            activity.setContent {
                WireTheme {
                    RegisterDeviceScreen()
                }
            }
        }
    }

    val title = composeTestRule.onNodeWithText("Add this Device")
    val registerText = composeTestRule.onNode(hasTestTag("registerText"))
    val passwordField = composeTestRule.onNode(hasTestTag("password field"))
    val registerButton = composeTestRule.onNode(hasTestTag("registerButton"))
    val hidePassword = composeTestRule.onNode(hasTestTag("hidePassword"), useUnmergedTree = true)

    val text = "Enter your password to use Wire on this device."

    @Test
    fun register_device_success() {
        title.assertIsDisplayed()
        registerText.assertTextEquals(text)
        passwordField.onChildren()[1].performTextInput(PASSWORD)
        registerButton.performClick()
        composeTestRule.waitForExecution {
            composeTestRule.onNodeWithText("Invalid password").assertDoesNotExist()
        }
    }

    @Test
    fun register_device_wrongPassword() {
        title.assertIsDisplayed()
        registerText.assertTextEquals(text)
        passwordField.onChildren()[1].performTextInput("BADPASS")
        hidePassword.performClick()
        passwordField.onChildren()[1].assertTextEquals("BADPASS")
        hidePassword.performClick()
        passwordField.onChildren()[1].shouldNotBeEqualTo("BADPASS")
        registerButton.performClick()
        composeTestRule.waitForExecution {
            composeTestRule.onNodeWithText("Invalid password").assertIsDisplayed()
        }
    }

    @Test
    fun register_state_buttonDisabled() {
        title.assertIsDisplayed()
        registerText.assertTextEquals(text)
        registerButton.assertIsDisplayed().assertIsNotEnabled()
        passwordField.onChildren()[1].performTextInput(PASSWORD)
        registerButton.assertIsEnabled()
    }
}
