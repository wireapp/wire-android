package com.wire.android.ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onSiblings
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
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
    val composeTestRule = createAndroidComposeRule<WireActivity>()

    @Before
    fun setUp(){
        hiltRule.inject()

        // Start the app
        composeTestRule.setContent {
            WireTheme {
                WelcomeScreen(composeTestRule.getViewModel(WelcomeViewModel::class))
            }
        }
    }

    private var logo = composeTestRule.onNodeWithContentDescription("Wire")
    private var images = logo.onSiblings()[0]
    private var loginButton = logo.onSiblings()[1]
    private var createEnterpriseAccountButton = composeTestRule.onNodeWithText("Create Enterprise Account")
    private var privateAccountText = logo.onSiblings()[3]
    private var privateAccountLink = composeTestRule.onNodeWithText("Create a private account for free")

    @Test
    fun iTapLoginButton() {
        loginButton.assertIsDisplayed()
        createEnterpriseAccountButton.assertIsDisplayed()
        loginButton.performClick()
    }

    @Test
    fun iTapCreateEnterpriseButton() {
        createEnterpriseAccountButton.assertIsDisplayed()
        createEnterpriseAccountButton.performClick()
    }

    @Test
    fun check_UI() {
        logo.assertIsDisplayed()
        images.assertIsDisplayed()
        privateAccountLink.assertIsDisplayed()
        privateAccountText.assertTextEquals("Want to chat with friends and family?")
    }

    @Test
    fun scroll_Images() {
        images.performScrollToIndex(1)
        composeTestRule.onNodeWithText("Login").onSiblings()[1].performScrollToIndex(1).onChildren()[1].assertTextEquals("Welcome to Wire, the most secure collaboration platform!")
        images.performScrollToIndex(2)
        composeTestRule.onNodeWithText("Login").onSiblings()[1].performScrollToIndex(2).onChildren()[1].assertTextEquals("Absolute confidence your information is secure")
        images.performScrollToIndex(3)
        composeTestRule.onNodeWithText("Login").onSiblings()[1].performScrollToIndex(3).onChildren()[1].assertTextEquals("Encrypted audio & video conferencing with up to 50 participants")
        images.performScrollToIndex(4)
        composeTestRule.onNodeWithText("Login").onSiblings()[1].performScrollToIndex(4).onChildren()[1].assertTextEquals("Secure file sharing with teams and clients")
        images.performScrollToIndex(5)
        composeTestRule.onNodeWithText("Login").onSiblings()[1].performScrollToIndex(5).onChildren()[1].assertTextEquals("Wire is independently audited and ISO, CCPA, GDPR, SOX-compliant")
    }
}
