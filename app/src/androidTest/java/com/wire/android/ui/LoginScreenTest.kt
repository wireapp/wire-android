package com.wire.android.ui

import android.util.Log
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onSiblings
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.printToLog
import androidx.compose.ui.test.printToString
import com.wire.android.R
import com.wire.android.ui.authentication.login.LoginScreen
import com.wire.android.ui.authentication.welcome.WelcomeScreen
import com.wire.android.ui.authentication.welcome.WelcomeViewModel
import com.wire.android.ui.theme.WireTheme
import com.wire.android.utils.WorkManagerTestRule
import com.wire.android.utils.getViewModel
import com.wire.kalium.logic.configuration.ServerConfig
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import junit.framework.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class,
    ExperimentalComposeUiApi::class, ExperimentalMaterialApi::class
)
@HiltAndroidTest
class LoginScreenTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    // Second, as we are using a WorkManager
    // In an instrumented test we need to ensure this gets initialized before launching any Compose/Activity Rule
    @get:Rule(order = 1)
    var workManagerTestRule = WorkManagerTestRule()

    // Third, we create the compose rule using an AndroidComposeRule, as we are depending on instrumented environment ie: Hilt, WorkManager
    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<WireActivity>()

    val passwordField = composeTestRule.onNode(hasTestTag("passwordField"))
    val emailField = composeTestRule.onNode(hasTestTag("emailField"))
    val loginButton = composeTestRule.onNode(hasTestTag("loginButton"))
    val okButton = composeTestRule.onNodeWithText("OK")

    @Before
    fun testPrep() {
        hiltRule.inject()

        // Start the app
        composeTestRule.setContent {
            WireTheme {
                LoginScreen(serverConfig = ServerConfig.DEFAULT)
            }
        }
    }

    @Test
    fun loginSucessfully() {

        emailField.assertIsDisplayed()
        emailField.onChildren()[1].performTextClearance()
        emailField.onChildren()[1].performTextInput("mustafa+1@wire.com")

        passwordField.assertIsDisplayed()
        passwordField.onChildren()[1].performTextClearance()
        passwordField.onChildren()[1].performTextInput("123456")

        loginButton.assertHasClickAction()
        loginButton.performClick()

        composeTestRule.onNodeWithText("Logging in...").assertIsDisplayed()
        composeTestRule.waitForIdle()
    }

    @Test
    fun TryToLoginWithWrongEmailPassword() {

        emailField.assertIsDisplayed()
        emailField.onChildren()[1].performTextClearance()
        emailField.onChildren()[1].performTextInput("mustafa+1@wire.com")

        passwordField.assertIsDisplayed()
        passwordField.onChildren()[1].performTextClearance()
        passwordField.onChildren()[1].performTextInput("123456")

        loginButton.assertHasClickAction()
        loginButton.performClick()

        composeTestRule.onNodeWithText("Logging in...").assertIsDisplayed()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(3000) { okButton.toString().contains("OK") }
        okButton.assertIsDisplayed()
        okButton.performClick()
    }

    @Test
    fun TryToLoginWithWrongEmailFormat() {

        emailField.assertIsDisplayed()
        emailField.onChildren()[1].performTextClearance()
        emailField.onChildren()[1].performTextInput("m")

        passwordField.assertIsDisplayed()
        passwordField.onChildren()[1].performTextClearance()
        passwordField.onChildren()[1].performTextInput("123456")

        loginButton.assertHasClickAction()
        loginButton.performClick()

        composeTestRule.onNodeWithText("Please enter a valid format for your email or username").assertIsDisplayed()
        composeTestRule.waitForIdle()
    }

    @Test
    fun checkLoginButtonIsDisabled() {

        emailField.assertIsDisplayed()
        emailField.onChildren()[1].performTextInput("mustafa+1@wire.com")

        loginButton.assertIsNotEnabled()

        emailField.onChildren()[1].performTextClearance()

        passwordField.assertIsDisplayed()
        passwordField.onChildren()[1].performTextInput("mustafa+1@wire.com")

        loginButton.assertIsNotEnabled()

        passwordField.onChildren()[1].performTextClearance()
        loginButton.assertIsNotEnabled()
    }

    @Test
    fun iSeeForgotPasswordScreen() {

        composeTestRule.onNodeWithText("Forgot password?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Forgot password?").performClick()
//        composeTestRule.onNodeWithText("Change Password", ignoreCase = true).assertIsDisplayed()
    }
}
