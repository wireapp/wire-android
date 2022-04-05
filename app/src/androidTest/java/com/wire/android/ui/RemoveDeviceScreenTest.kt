package com.wire.android.ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.ui.authentication.devices.RemoveDeviceScreen
import com.wire.android.ui.theme.WireTheme
import com.wire.android.utils.PASSWORD
import com.wire.android.utils.WorkManagerTestRule
import com.wire.android.utils.waitForExecution
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.ServerConfig
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
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

    @Inject
    @KaliumCoreLogic
    lateinit var coreLogic: CoreLogic

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() = runTest {
        hiltRule.inject()

        val authenticationScope = coreLogic.getAuthenticationScope()
        authenticationScope.login("mustafa+1@wire.com", PASSWORD, false, ServerConfig.STAGING)
        composeTestRule.setContent {
            WireTheme {
                RemoveDeviceScreen()
            }
        }
    }

    val title = composeTestRule.onNodeWithText("Remove a Device")
    val backButton = composeTestRule.onNodeWithText("Back button")
    val removeDeviceButton = composeTestRule.onAllNodes(hasTestTag("remove device button"))
    val remove = composeTestRule.onNodeWithContentDescription("Remove icon")
    val removeDeviceText = composeTestRule.onNodeWithText("Remove the following device?")
    val removeButton = composeTestRule.onNodeWithText("Remove")
    val cancelButton = composeTestRule.onNodeWithText("Cancel")
    val passwordField = composeTestRule.onNode(hasTestTag("remove device password field"))

    val invalidPasswordText = "Invalid password"

    @Test
    fun removeDevice_Successfully() {
        title.assertIsDisplayed()
        composeTestRule.waitForExecution {
            removeDeviceButton[1].performClick()
            removeDeviceText.assertIsDisplayed()
        }
        passwordField.onChildren()[1].performTextClearance()
        passwordField.onChildren()[1].performTextInput("Mustafastaging1!")
        removeButton.performClick()
    }

    @Test
    fun removeDevice_error_wrongPassword() {
        title.assertIsDisplayed()
        composeTestRule.waitForExecution {
            removeDeviceButton[1].performClick()
            removeDeviceText.assertIsDisplayed()
        }
        passwordField.onChildren()[1].performTextClearance()
        passwordField.onChildren()[1].performTextInput("BAD PASSWORD")
        removeButton.performClick()
        composeTestRule.waitForExecution {
            composeTestRule.onNodeWithText(invalidPasswordText).assertIsDisplayed()
        }
    }

    @Test
    fun removeDevice_cancel() {
        title.assertIsDisplayed()
        composeTestRule.waitForExecution {
            removeDeviceButton[1].performClick()
            removeDeviceText.assertIsDisplayed()
        }
        cancelButton.performClick()
        title.assertIsDisplayed()
        removeDeviceButton[1].assertIsDisplayed()
    }
}
