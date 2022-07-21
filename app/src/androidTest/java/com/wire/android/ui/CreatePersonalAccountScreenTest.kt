package com.wire.android.ui

import android.content.Intent
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onSiblings
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import com.wire.android.ui.authentication.create.personalaccount.CreatePersonalAccountScreen
import com.wire.android.ui.authentication.create.team.CreateTeamScreen
import com.wire.android.ui.theme.WireTheme
import com.wire.android.utils.EMAIL
import com.wire.android.utils.WorkManagerTestRule
import com.wire.android.utils.waitForExecution
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.core.AllOf.allOf
import org.junit.After
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runners.MethodSorters

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class,
    ExperimentalComposeUiApi::class, ExperimentalMaterialApi::class
)
@HiltAndroidTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class CreatePersonalAccountScreenTest {

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
                    CreatePersonalAccountScreen()
                }
            }
        }
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    val title = composeTestRule.onNodeWithText("Create a Personal Account")
    val continueButton = composeTestRule.onNodeWithText("Continue")
    val createTeamText = composeTestRule.onNode(hasTestTag("createTeamText"))
    val emailField = composeTestRule.onNode(hasTestTag("emailField"))
    val tcTitle = composeTestRule.onNodeWithText("Terms of Use")
    val cancelButton = composeTestRule.onNode(hasTestTag("cancelButton"))
    val tcButton = composeTestRule.onNode(hasTestTag("viewTC"))
    val firstName = composeTestRule.onNode(hasTestTag("firstName"))
    val lastName = composeTestRule.onNode(hasTestTag("lastName"))
    val password = composeTestRule.onNode(hasTestTag("password"))
    val confirmPassword = composeTestRule.onNode(hasTestTag("confirmPassword"))

    val invalidEmailError = "Please enter a valid format for your email."
    val createATeamText = "Enter your email to start using the most secure collaboration platform."
    val invalidPassword = "Use at least 8 characters, with one lowercase letter, one capital letter, a number, and a special character."
    val passwordsNotMatch = "Passwords do not match"
    val validEmail = "a@wire.com"

    @Ignore
    @Test
    fun create_account_success() {
        title.assertIsDisplayed()
        continueButton.performClick()
        createTeamText.assertTextEquals(createATeamText)
        emailField.onChildren()[1].performTextInput(EMAIL)
        continueButton.performClick()
        tcTitle.assertIsDisplayed()
        tcButton.onSiblings()[1].performClick()
        firstName.onChildren()[1].performTextInput("name")
    }

    @Test
    fun create_account_wrong_email() {
        title.assertIsDisplayed()
        continueButton.performClick()
        emailField.onChildren()[1].performTextInput("EMAIL")
        continueButton.performClick()
        composeTestRule.waitForExecution {
        composeTestRule.onNodeWithText(invalidEmailError).assertIsDisplayed()
        }
    }

    @Test
    fun create_account_tc_cancel() {
        title.assertIsDisplayed()
        continueButton.performClick()
        emailField.onChildren()[1].performTextInput(validEmail)
        continueButton.performClick()
        tcTitle.assertIsDisplayed()
        cancelButton.performClick()
        tcTitle.assertDoesNotExist()
        title.assertIsDisplayed()
    }

    @Test
    fun create_account_tc_view() {
        title.assertIsDisplayed()
        continueButton.performClick()
        emailField.onChildren()[1].performTextInput(EMAIL)
        continueButton.performClick()
        tcTitle.assertIsDisplayed()
        tcButton.performClick()
        Intents.intending(allOf(hasAction(Intent.ACTION_VIEW), hasData("https://wire.com/en/legal/")))
    }

    @Test
    fun create_account_invalid_password() {
        title.assertIsDisplayed()
        continueButton.performClick()
        createTeamText.assertTextEquals(createATeamText)
        emailField.onChildren()[1].performTextInput(validEmail)
        continueButton.performClick()
        tcTitle.assertIsDisplayed()
        tcButton.onSiblings()[3].performClick()
        composeTestRule.waitForExecution {
            firstName.onChildren()[2].performTextInput("name")
        }
        lastName.onChildren()[2].performTextInput("surName")
        password.onChildren()[2].performTextInput("password")
        confirmPassword.onChildren()[2].performTextInput("password")
        continueButton.performClick()
        composeTestRule.waitForExecution {
            composeTestRule.onNodeWithText(invalidPassword).assertIsDisplayed()
        }
    }

    @Test
    fun create_account_mismatch_password() {
        title.assertIsDisplayed()
        continueButton.performClick()
        createTeamText.assertTextEquals(createATeamText)
        emailField.onChildren()[1].performTextInput(validEmail)
        continueButton.performClick()
        tcTitle.assertIsDisplayed()
        tcButton.onSiblings()[3].performClick()
        composeTestRule.waitForExecution {
            firstName.onChildren()[2].performTextInput("name")
        }
        lastName.onChildren()[2].performTextInput("surName")
        continueButton.performScrollTo()
        password.onChildren()[2].performTextInput("Abcd1234!")
        confirmPassword.onChildren()[2].performTextInput("Abcd1234.")
        continueButton.performClick()
        composeTestRule.waitForExecution {
            composeTestRule.onNodeWithText(invalidPassword).assertIsDisplayed()
        }
    }

    @Test
    fun create_account_required_fields() {
        title.assertIsDisplayed()
        continueButton.performClick()
        createTeamText.assertTextEquals(createATeamText)
        emailField.onChildren()[1].performTextInput(validEmail)
        continueButton.performClick()
        tcTitle.assertIsDisplayed()
        tcButton.onSiblings()[3].performClick()
        composeTestRule.waitForExecution {
            firstName.onChildren()[2].performTextInput("name")
        }
        continueButton.assertIsNotEnabled()
        lastName.onChildren()[2].performTextInput("surName")
        continueButton.assertIsNotEnabled()
        continueButton.performScrollTo()
        password.onChildren()[2].performTextInput("Abcd1234!")
        continueButton.assertIsNotEnabled()
        confirmPassword.onChildren()[2].performTextInput("Abcd1234.")
        continueButton.assertIsEnabled()
    }
}
