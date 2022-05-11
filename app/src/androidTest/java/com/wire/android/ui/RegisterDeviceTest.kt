package com.wire.android.ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
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
    val composeTestRule = createAndroidComposeRule<WireActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()

        // Start the app
        composeTestRule.setContent {
            WireTheme {
                RegisterDeviceScreen()
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
        registerButton.assertIsDisplayed()
        passwordField.onChildren()[1].performTextInput(PASSWORD)
        registerButton.assertIsEnabled()
    }
}
