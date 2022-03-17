package com.wire.android.ui

import android.content.Intent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import com.wire.android.ui.authentication.login.LoginScreen
import com.wire.android.ui.theme.WireTheme
import com.wire.android.utils.WorkManagerTestRule
import com.wire.android.utils.waitForExecution
import com.wire.kalium.logic.configuration.ServerConfig
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.core.AllOf.allOf
import org.junit.After
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runners.MethodSorters

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class,
    ExperimentalComposeUiApi::class, ExperimentalMaterialApi::class
)
@HiltAndroidTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
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

    @Before
    fun setUp() {
        hiltRule.inject()
        Intents.init()

        // Start the app
        composeTestRule.setContent {
            WireTheme {
                LoginScreen(serverConfig = ServerConfig.DEFAULT)
            }
        }
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    val passwordField = composeTestRule.onNode(hasTestTag("passwordField"))
    val emailField = composeTestRule.onNode(hasTestTag("emailField"))
    val loginButton = composeTestRule.onNode(hasTestTag("loginButton"))
    val okButton = composeTestRule.onNodeWithText("OK")
    val forgotPassword = composeTestRule.onNode(hasTestTag("Forgot password?"))
    val hidePassword = composeTestRule.onNode(hasTestTag("hidePassword"), useUnmergedTree = true)

    val loginErrorText = "Please enter a valid format for your email or username"
    val email = "mustafa+1@wire.com"

    @Test
    fun login_success_case() {
        emailField.assertIsDisplayed()
        emailField.onChildren()[1].performTextClearance()
        emailField.onChildren()[1].performTextInput(email)

        passwordField.assertIsDisplayed()
        passwordField.onChildren()[1].performTextClearance()
        passwordField.onChildren()[1].performTextInput("Mustafastaging1!")

        loginButton.assertHasClickAction()
        loginButton.performClick()

        composeTestRule.onNodeWithText("Logging in...").assertIsDisplayed()

        composeTestRule.waitForExecution {
            composeTestRule.onNodeWithText("Invalid information").assertDoesNotExist()
        }
    }

    @Test
    fun login_error_wrongPassword() {
        emailField.assertIsDisplayed()
        emailField.onChildren()[1].performTextClearance()
        emailField.onChildren()[1].performTextInput(email)

        passwordField.assertIsDisplayed()
        passwordField.onChildren()[1].performTextClearance()
        passwordField.onChildren()[1].performTextInput("123456")
        // Click on show password icon and check password is visible
        hidePassword.performClick()
        passwordField.onChildren()[1].assertTextEquals("123456")

        loginButton.assertHasClickAction()
        loginButton.performClick()

        composeTestRule.onNodeWithText("Logging in...").assertIsDisplayed()
        composeTestRule.waitForExecution {
            composeTestRule.onNodeWithText("Invalid information").assertIsDisplayed()
        }

        okButton.assertIsDisplayed()
        okButton.performClick()
    }

    @Test
    fun login_error_wrongEmailFormat() {

        emailField.assertIsDisplayed()
        emailField.onChildren()[1].performTextClearance()
        emailField.onChildren()[1].performTextInput("m")

        passwordField.assertIsDisplayed()
        passwordField.onChildren()[1].performTextClearance()
        passwordField.onChildren()[1].performTextInput("123456")

        loginButton.assertHasClickAction()
        loginButton.performClick()

        composeTestRule.onNodeWithText(loginErrorText).assertIsDisplayed()
    }

    @Test
    fun login_state_loginButtonIsDisabled() {

        emailField.assertIsDisplayed()
        emailField.onChildren()[1].performTextInput(email)

        loginButton.assertIsNotEnabled()

        emailField.onChildren()[1].performTextClearance()

        passwordField.assertIsDisplayed()
        passwordField.onChildren()[1].performTextInput(email)

        loginButton.assertIsNotEnabled()

        passwordField.onChildren()[1].performTextClearance()
        loginButton.assertIsNotEnabled()
    }

    @Test
    fun login_navigation_forgotPasswordScreen() {
        forgotPassword.assertIsDisplayed()
        forgotPassword.performClick()

        // Change to espresso intents assertion, since we are displaying a chrome custom tab component (not a composable one)
        Intents.intending(allOf(hasAction(Intent.ACTION_VIEW), hasData("https://wire-account-staging.zinfra.io/forgot")))
    }
}
