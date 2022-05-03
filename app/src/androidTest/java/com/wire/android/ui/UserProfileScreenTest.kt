package com.wire.android.ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onSibling
import androidx.compose.ui.test.onSiblings
import androidx.compose.ui.test.performClick
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.userprofile.self.SelfUserProfileScreen
import com.wire.android.ui.userprofile.self.SelfUserProfileViewModel
import com.wire.android.utils.WorkManagerTestRule
import com.wire.android.utils.getViewModel
import com.wire.android.utils.waitForExecution
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class,
    ExperimentalComposeUiApi::class, ExperimentalMaterialApi::class
)
@HiltAndroidTest
class UserProfileScreenTest {

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
                SelfUserProfileScreen(composeTestRule.getViewModel(SelfUserProfileViewModel::class))
            }
        }
    }

    val title = composeTestRule.onNodeWithText("User Profile")
    val imageTitle = composeTestRule.onNodeWithText("Profile image")
    val logoutButton = composeTestRule.onNodeWithText("Logout")
    val availableButton = composeTestRule.onNode(hasTestTag("Available"), useUnmergedTree = true)
    val busyButton = composeTestRule.onNode(hasTestTag("Busy"), useUnmergedTree = true)
    val awayButton = composeTestRule.onNode(hasTestTag("Away"), useUnmergedTree = true)
    val noneButton = composeTestRule.onNode(hasTestTag("None"), useUnmergedTree = true)
    val okButton = composeTestRule.onNodeWithText("OK")
    val cancelButton = composeTestRule.onNodeWithText("Cancel")
    val avatar = composeTestRule.onNode(hasTestTag("User avatar"), useUnmergedTree = true)
    val changeImageButton = composeTestRule.onNodeWithText("Change Image")
    val chooseFromGallery = composeTestRule.onNodeWithText("Choose from gallery")
    val takeaPicture = composeTestRule.onNodeWithText("Take a picture")

    @Test
    fun userProfile_change_status_busy() {
        title.assertIsDisplayed()
        busyButton.onSibling().performClick()
        val busyText = composeTestRule.onNodeWithText("Set yourself to Busy")
        composeTestRule.waitForExecution {
            busyText.assertIsDisplayed()
        }
        busyText.onSiblings()[0].assertTextContains("You will appear as Busy to other people. You will only receive notifications for mentions, replies, and calls in conversations that are not muted.")
        busyText.onSiblings()[1].performClick().assertIsOn()
        busyText.onSiblings()[1].performClick().assertIsOff().assertTextContains("Do not display this information again")
        okButton.performClick()
        busyText.assertDoesNotExist()
    }

    @Test
    fun userProfile_change_status_available() {
        title.assertIsDisplayed()
        availableButton.onSibling().performClick()
       val availableText = composeTestRule.onNodeWithText("Set yourself to Available")
        /*        composeTestRule.waitForExecution {
                   availableText.assertIsDisplayed()
               }
        okButton.performClick()*/
        availableText.assertDoesNotExist()
    }

    @Test
    fun userProfile_change_status_away() {
        title.assertIsDisplayed()
        awayButton.onSibling().performClick()
        val awayText = composeTestRule.onNodeWithText("Set yourself to Away")
        composeTestRule.waitForExecution {
            awayText.assertIsDisplayed()
        }
        awayText.onSiblings()[1].performClick().assertIsOn().performClick().assertIsOff().assertTextContains("Do not display this information again")
        okButton.performClick()
        awayText.assertDoesNotExist()
    }

    @Ignore
    @Test
    fun userProfile_change_status_none() {
        title.assertIsDisplayed()
        noneButton.onSibling().performClick()
        val noneText = composeTestRule.onNodeWithText("No status Set")
        noneText.assertIsDisplayed()
        okButton.performClick()
        noneText.assertDoesNotExist()
        noneButton.onSibling().performClick()  // check status is set
        noneText.assertDoesNotExist()
    }

    @Test
    fun userProfile_check_donotdisplay() {
        title.assertIsDisplayed()
        awayButton.onSibling().performClick()
        val awayText = composeTestRule.onNodeWithText("Set yourself to Away")
        awayText.assertIsDisplayed().onSiblings()[1].performClick().assertIsOn().assertTextContains("Do not display this information again")
        okButton.performClick()
        awayText.assertDoesNotExist()
        awayButton.onSibling().performClick()
        awayText.assertDoesNotExist()
    }

    @Test
    fun userProfile_change_status_cancel() {
        title.assertIsDisplayed()
        awayButton.onSibling().performClick()
        val awayText = composeTestRule.onNodeWithText("Set yourself to Away")
        awayText.assertIsDisplayed().onSiblings()[1].performClick().assertIsOn().assertTextContains("Do not display this information again")
        cancelButton.performClick()
        awayText.assertDoesNotExist()
        awayButton.onSibling().performClick()
        awayText.assertIsDisplayed()
        cancelButton.performClick()
        awayText.assertDoesNotExist()
    }

    @Test
    fun userProfile_logout() {
        title.assertIsDisplayed()
        logoutButton.performClick()
    }

    @Ignore
    @Test
    fun userProfile_change_avatar() {
        title.assertIsDisplayed()
        avatar.performClick()
        imageTitle.assertIsDisplayed()
        changeImageButton.performClick()
        chooseFromGallery.performClick()
    }
}
