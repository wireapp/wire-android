package com.wire.android.ui

import android.content.Intent
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import com.wire.android.ui.authentication.login.email.LoginEmailScreen
import com.wire.android.ui.theme.WireTheme
import com.wire.android.utils.EMAIL
import com.wire.android.utils.PASSWORD
import com.wire.android.utils.USER_NAME
import com.wire.android.utils.WorkManagerTestRule
import com.wire.android.utils.waitForExecution
import com.wire.kalium.logic.configuration.ServerConfig
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.amshove.kluent.shouldNotBeEqualTo
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
class LoginEmailScreenTest {

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
        Intents.init()

        // Start the app
        scenario = ActivityScenario.launch(Intent(ApplicationProvider.getApplicationContext(), WireActivity::class.java))
        scenario.onActivity { activity ->
            activity.setContent {
                WireTheme {
                    LoginEmailScreen(serverConfig = ServerConfig.STAGING)
                }
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

    val loginErrorText = "This email or username is invalid. Please verify and try again."

    @Test
    fun login_email_success() {
        emailField.assertIsDisplayed()
        emailField.onChildren()[1].performTextClearance()
        emailField.onChildren()[1].performTextInput(EMAIL)

        passwordField.assertIsDisplayed()
        passwordField.onChildren()[1].performTextClearance()
        passwordField.onChildren()[1].performTextInput(PASSWORD)

        loginButton.assertHasClickAction()
        loginButton.performClick()

        composeTestRule.onNodeWithText("Logging in...").assertIsDisplayed()

        composeTestRule.waitForExecution {
            composeTestRule.onNodeWithText("Invalid information").assertDoesNotExist()
        }
    }

    @Test
    fun login_username_success() {
        emailField.assertIsDisplayed()
        emailField.onChildren()[1].performTextClearance()
        emailField.onChildren()[1].performTextInput(USER_NAME)

        passwordField.assertIsDisplayed()
        passwordField.onChildren()[1].performTextClearance()
        passwordField.onChildren()[1].performTextInput(PASSWORD)

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
        emailField.onChildren()[1].performTextInput(EMAIL)

        passwordField.assertIsDisplayed()
        passwordField.onChildren()[1].performTextClearance()
        passwordField.onChildren()[1].performTextInput("123456")
        // Click on show password icon and check password is visible
        hidePassword.performClick()
        passwordField.onChildren()[1].assertTextEquals("123456")
        hidePassword.performClick()
        passwordField.onChildren()[1].shouldNotBeEqualTo("123456")

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
        emailField.onChildren()[1].performTextInput(EMAIL)

        loginButton.assertIsNotEnabled()

        emailField.onChildren()[1].performTextClearance()

        passwordField.assertIsDisplayed()
        passwordField.onChildren()[1].performTextInput(EMAIL)

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
