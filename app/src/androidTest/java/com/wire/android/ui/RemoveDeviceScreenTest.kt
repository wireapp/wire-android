package com.wire.android.ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.wire.android.ui.authentication.devices.RemoveDeviceScreen
import com.wire.android.ui.authentication.devices.RemoveDeviceViewModel
import com.wire.android.ui.authentication.login.LoginScreen
import com.wire.android.ui.authentication.welcome.WelcomeViewModel
import com.wire.android.ui.theme.WireTheme
import com.wire.android.utils.PASSWORD
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


    val title = composeTestRule.onNodeWithText("Remove a Device")
    val backButton = composeTestRule.onNodeWithText("Back button")
    val removeDeviceButton = composeTestRule.onNode(hasTestTag("remove device button"))
    val remove = composeTestRule.onNodeWithContentDescription("Remove icon")
    val removeDeviceText = composeTestRule.onNodeWithText("Remove the following device?")
    val removeButton = composeTestRule.onNodeWithContentDescription("Remove")
    val cancelButton = composeTestRule.onNodeWithText("Cancel")
    val removeDevicePasswordField = composeTestRule.onNode(hasTestTag("remove device password field"))

    val invalidPasswordText = "Invalid password"

    @Test
    fun removeDeviceSucessfully() {
        title.assertIsDisplayed()
        removeDeviceButton.performClick()
        removeDeviceText.assertIsDisplayed()
        removeDevicePasswordField.performTextInput(PASSWORD)
        removeButton.performClick()

    }

    @Test
    fun removeDevice_error_wrongPassword() {
        title.assertIsDisplayed()
        removeDeviceButton.performClick()
        removeDeviceText.assertIsDisplayed()
        removeDevicePasswordField.performTextInput("123456")
        removeButton.performClick()
        composeTestRule.onNodeWithText("invalidPasswordText").assertIsDisplayed()
    }

    @Test
    fun removeDevice_cancel() {
        title.assertIsDisplayed()
        removeDeviceButton.performClick()
        removeDeviceText.assertIsDisplayed()
        removeDevicePasswordField.performTextInput(PASSWORD)
        cancelButton.performClick()
        title.assertIsDisplayed()
        removeDeviceButton.assertIsDisplayed()
    }
}
