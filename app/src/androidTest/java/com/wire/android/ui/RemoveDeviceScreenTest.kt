package com.wire.android.ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.wire.android.ui.authentication.devices.RemoveDeviceScreen
import com.wire.android.ui.authentication.devices.RemoveDeviceViewModel
import com.wire.android.ui.authentication.login.LoginScreen
import com.wire.android.ui.authentication.welcome.WelcomeViewModel
import com.wire.android.ui.theme.WireTheme
import com.wire.android.utils.WorkManagerTestRule
import com.wire.android.utils.getViewModel
import com.wire.kalium.logic.configuration.ServerConfig
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
class RemoveDeviceScreenTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    // Second, as we are using a WorkManager
    // In an instrumented test we need to ensure this gets initialized before launching any Compose/Activity Rule
    @get:Rule(order = 1)
    var workManagerTestRule = WorkManagerTestRule()

    // Third, we create the compose rule using an AndroidComposeRule, as we are depending on instrumented environment ie: Hilt, WorkManager
    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<WireActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()

        // Start the app
        composeTestRule.setContent {
            WireTheme {
                RemoveDeviceScreen()
            }
        }
    }

    @Test
    fun removeDeviceSucessfully() {
        composeTestRule.onNodeWithText("Remove a Device").assertIsDisplayed()
    }
}
