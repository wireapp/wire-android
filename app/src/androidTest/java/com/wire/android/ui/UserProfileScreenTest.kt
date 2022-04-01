package com.wire.android.ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onSibling
import androidx.compose.ui.test.performClick
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.userprofile.self.SelfUserProfileScreen
import com.wire.android.ui.userprofile.self.SelfUserProfileViewModel
import com.wire.android.utils.WorkManagerTestRule
import com.wire.android.utils.getViewModel
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
    val logoutButton = composeTestRule.onNodeWithText("Logout")
    val availableButton = composeTestRule.onNode(hasTestTag("Available"), useUnmergedTree = true)
    val busyButton = composeTestRule.onNode(hasTestTag("Busy"))
    val loginButton = composeTestRule.onNode(hasTestTag("loginButton"))
    val okButton = composeTestRule.onNodeWithText("OK")

    @Ignore
    @Test
    fun userProfile_change_status() {
        title.assertIsDisplayed()
        availableButton.onSibling().performClick()
        busyButton.performClick()
    }
}
