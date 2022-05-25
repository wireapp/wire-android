package com.wire.android.ui

import android.content.Intent
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceScreen
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
class RemoveDeviceScreenTest {

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

        scenario = ActivityScenario.launch(Intent(ApplicationProvider.getApplicationContext(), WireActivity::class.java))
        scenario.onActivity { activity ->
            activity.setContent {
                WireTheme {
                    RemoveDeviceScreen()
                }
            }
        }
    }

    val title = composeTestRule.onNodeWithText("Remove a Device")
    val removeDeviceButton = composeTestRule.onAllNodes(hasTestTag("remove device button"))
    val remove = composeTestRule.onNodeWithContentDescription("Remove icon")
    val removeDeviceText = composeTestRule.onNodeWithText("Remove the following device?")
    val removeButton = composeTestRule.onNodeWithText("Remove")
    val cancelButton = composeTestRule.onNodeWithText("Cancel")
    val passwordField = composeTestRule.onNode(hasTestTag("remove device password field"))
    val hidePassword = composeTestRule.onNode(hasTestTag("hidePassword"), useUnmergedTree = true)

    val invalidPasswordText = "Invalid password"

    @Test
    fun removeDevice_Successfully() {
        title.assertIsDisplayed()
        composeTestRule.waitForExecution {
            removeDeviceButton[1].performClick()
            removeDeviceText.assertIsDisplayed()
        }
        passwordField.onChildren()[1].performTextClearance()
        passwordField.onChildren()[1].performTextInput(PASSWORD)
        hidePassword.performClick()
        passwordField.onChildren()[1].assertTextEquals(PASSWORD)
        hidePassword.performClick()
        passwordField.onChildren()[1].shouldNotBeEqualTo(PASSWORD)
        removeButton.performClick()
        composeTestRule.waitForExecution {
            composeTestRule.onNodeWithText(invalidPasswordText).assertDoesNotExist()
        }
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

    @Test
    fun deviceList_count() {
        title.assertIsDisplayed()
//        removeDeviceButton.assertCountEquals(6)
        removeDeviceButton.assertAll(hasContentDescription("Remove icon") and hasClickAction())
    }
}
