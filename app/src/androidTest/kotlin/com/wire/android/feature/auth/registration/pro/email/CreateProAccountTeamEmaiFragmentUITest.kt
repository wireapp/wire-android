package com.wire.android.feature.auth.registration.pro.email

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.wire.android.FunctionalActivityTest
import com.wire.android.R
import com.wire.android.core.extension.EMPTY
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not
import org.junit.Test

class CreateProAccountTeamEmailFragmentUITest : FunctionalActivityTest(
    CreateProAccountTeamEmailActivity::class.java
) {

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
    fun uiElementsVisibleInLandscape() = rotateScreen {
        uiElementsVisible()
    }

    companion object {
        private const val TEST_TEAM_EMAIL = "team@wire.com"
    }
}