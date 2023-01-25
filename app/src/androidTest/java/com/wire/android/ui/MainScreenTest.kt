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
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onSiblings
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.wire.android.ui.authentication.welcome.WelcomeScreen
import com.wire.android.ui.authentication.welcome.WelcomeViewModel
import com.wire.android.ui.theme.WireTheme
import com.wire.android.utils.WorkManagerTestRule
import com.wire.android.utils.getViewModel
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class,
    ExperimentalComposeUiApi::class, ExperimentalMaterialApi::class
)
@HiltAndroidTest
class MainScreenTest {

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
                    WelcomeScreen(getViewModel(activity, WelcomeViewModel::class))
                }
            }
        }
    }

    private var logo = composeTestRule.onNodeWithContentDescription("Wire")
    private var images = logo.onSiblings()[0]
    private var loginButton = logo.onSiblings()[1]
    private var createTeamButton = composeTestRule.onNodeWithText("Create a Team")
    private var personalAccountText = logo.onSiblings()[3]
    private var personalAccountLink = composeTestRule.onNodeWithText("Create a Personal Account")

    @Test
    fun iTapLoginButton() {
        loginButton.assertIsDisplayed()
        createTeamButton.assertIsDisplayed()
        loginButton.performClick()
    }

    @Test
    fun iTapCreateEnterpriseButton() {
        createTeamButton.assertIsDisplayed()
        createTeamButton.performClick()
    }

    @Test
    fun check_UI() {
        logo.assertIsDisplayed()
        images.assertIsDisplayed()
        personalAccountLink.assertIsDisplayed()
        personalAccountText.assertTextEquals("Want to chat with friends and family?")
    }

    @Test
    fun scroll_Images() {
        images.performScrollToIndex(1).onChildren()[1]
            .assertTextEquals("Welcome to Wire, the most secure collaboration platform!")
        images.performScrollToIndex(2).onChildren()[1]
            .assertTextEquals("Absolute confidence your information is secure")
        images.performScrollToIndex(3).onChildren()[1].assertTextEquals("Encrypted audio & video conferencing with up to 50 participants")
        images.performScrollToIndex(4).onChildren()[1].assertTextEquals("Secure file sharing with teams and clients")
        images.performScrollToIndex(5).onChildren()[1].assertTextEquals("Wire is independently audited and ISO, CCPA, GDPR, SOX-compliant")
    }
}
