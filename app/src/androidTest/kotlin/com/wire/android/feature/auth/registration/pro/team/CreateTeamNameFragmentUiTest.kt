package com.wire.android.feature.auth.registration.pro.team

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.wire.android.FunctionalActivityTest
import com.wire.android.R
import com.wire.android.core.extension.EMPTY
import com.wire.android.feature.auth.registration.CreateAccountActivity
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not
import org.junit.Before
import org.junit.Test

class CreateTeamNameFragmentUiTest : FunctionalActivityTest(CreateAccountActivity::class.java) {

    @Before
    fun setup() {
        onView(withId(R.id.createProAccountTitleTextView)).perform(click())
    }

    @Test
    fun uiElementsVisible() {
        onView(withText(R.string.create_pro_account_set_team_name_title)).check(matches(isDisplayed()))
        onView(withText(R.string.create_pro_account_set_team_name_subtitle)).check(matches(isDisplayed()))
        onView(withText(R.string.create_pro_account_set_team_name_about)).check(matches(isDisplayed()))

        onView(withHint(R.string.create_pro_account_set_team_name_hint)).check(matches(isDisplayed()))

        onView(withId(R.id.createProAccountTeamNameInputConfirmationButton)).check(matches(isDisplayed()))
        onView(allOf(withId(R.id.createProAccountTeamNameInputConfirmationButton))).check(matches(allOf(not(isEnabled()))))

    }

    @Test
    fun inputTextIsNotEmpty_confirmationButtonShouldBeEnabled() {
        onView(withId(R.id.createProAccountTeamNameEditText)).perform(typeTextIntoFocusedView(TEST_TEAM_NAME))
        onView(withId(R.id.createProAccountTeamNameInputConfirmationButton)).check(matches(isEnabled()))
    }

    @Test
    fun inputTextIsEmpty_confirmationButtonShouldBeDisabled() {
        onView(withId(R.id.createProAccountTeamNameEditText)).perform(replaceText(String.EMPTY))
        onView(allOf(withId(R.id.createProAccountTeamNameInputConfirmationButton), not(isEnabled())))
    }

    @Test
    fun uiElementsVisibleInLandscape() = rotateScreen {
        uiElementsVisible()
    }

    companion object {
        private const val TEST_TEAM_NAME = "Team"
    }
}
