package com.wire.android.feature.auth.registration.pro.team

import android.content.Intent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.matcher.ViewMatchers.*
import com.wire.android.FunctionalActivityTest
import com.wire.android.R
import com.wire.android.core.extension.EMPTY
import com.wire.android.feature.auth.registration.CreateAccountActivity
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Test

class CreateProAccountTeamNameFragmentUiTest : FunctionalActivityTest(CreateAccountActivity::class.java) {

    @Before
    fun setup() {
        onView(withId(R.id.createProAccountTitleTextView)).perform(click())
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
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
        onView(withId(R.id.createProAccountTeamNameEditText)).perform(replaceText(TEST_TEAM_NAME))
        onView(withId(R.id.createProAccountTeamNameInputConfirmationButton)).check(matches(isEnabled()))
    }

    @Test
    fun inputTextIsEmpty_confirmationButtonShouldBeDisabled() {
        onView(withId(R.id.createProAccountTeamNameEditText)).perform(replaceText(String.EMPTY), closeSoftKeyboard())
        onView(withId(R.id.createProAccountTeamNameInputConfirmationButton)).check(matches(allOf(not(isEnabled()))))
    }

    @Test
    fun uiElementsVisibleInLandscape() = rotateScreen {
        uiElementsVisible()
    }

    @Test
    fun aboutButton_click_opensAboutTeamUri() {
        onView(withId(R.id.createProAccountTeamNameAboutButton)).perform(click())
        intended(hasAction(Intent.ACTION_VIEW))
        intended(hasData("$CONFIG_URL$TEAM_ABOUT_URL_SUFFIX"))
    }

    companion object {
        private const val TEST_TEAM_NAME = "Team"
        private const val CONFIG_URL = "https://wire.com"
        private const val TEAM_ABOUT_URL_SUFFIX = "/products/pro-secure-team-collaboration/"
    }
}
