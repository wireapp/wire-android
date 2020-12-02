package com.wire.android.feature.auth.registration.pro.email

import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.typeTextIntoFocusedView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.wire.android.FunctionalTest
import com.wire.android.R
import com.wire.android.core.extension.EMPTY
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not
import org.junit.Before
import org.junit.Test

class CreateProAccountTeamEmailFragmentTest : FunctionalTest() {

    private lateinit var scenario: FragmentScenario<CreateProAccountTeamEmailFragment>

    @Before
    fun setup() {
        scenario = launchFragmentInContainer(
            themeResId = R.style.AppTheme
        )
    }

    @Test
    fun uiElementsVisible() {
        onView(withText(R.string.create_pro_account_set_team_email_title))
            .check(matches(isDisplayed()))
        onView(withText(R.string.create_pro_account_set_team_email_subtitle))
            .check(matches(isDisplayed()))

        onView(withHint(R.string.create_pro_account_set_team_email_hint))
            .check(matches(isDisplayed()))

        onView(withId(R.id.createProAccountTeamEmailInputConfirmationButton))
            .check(matches(isDisplayed()))
        onView(allOf(withId(R.id.createProAccountTeamEmailInputConfirmationButton)))
            .check(matches(allOf(not(isEnabled()))))
    }

    @Test
    fun inputTextIsNotEmpty_confirmationButtonShouldBeEnabled() {
        onView(withId(R.id.createProAccountTeamEmailEditText))
            .perform(typeTextIntoFocusedView(TEST_TEAM_EMAIL))
        onView(withId(R.id.createProAccountTeamEmailInputConfirmationButton))
            .check(matches(isEnabled()))
    }

    @Test
    fun inputTextIsEmpty_confirmationButtonShouldBeDisabled() {
        onView(withId(R.id.createProAccountTeamEmailEditText)).perform(
            replaceText(String.EMPTY),
            closeSoftKeyboard()
        )
        onView(withId(R.id.createProAccountTeamEmailInputConfirmationButton))
            .check(matches(allOf(not(isEnabled()))))
    }

    @Test
    fun confirmationButtonClicked_navigateToVerificationScreen() {
        onView(withId(R.id.createProAccountTeamEmailEditText))
            .perform(typeTextIntoFocusedView(TEST_TEAM_EMAIL))
        onView(withId(R.id.createProAccountTeamEmailInputConfirmationButton))
            .perform(click())

        //Add navigated fragment logic in part 2
    }

    @Test
    fun uiElementsVisibleInLandscape() = rotateScreen {
        uiElementsVisible()
    }

    companion object {
        private const val TEST_TEAM_EMAIL = "test-team-account@wire.com"
    }
}
