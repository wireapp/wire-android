package com.wire.android.feature.auth.registration

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.wire.android.FunctionalActivityTest
import com.wire.android.R
import org.junit.Test

class CreateAccountActivityUITest : FunctionalActivityTest(CreateAccountActivity::class.java) {

    @Test
    fun launch_uiElementsVisible() {
        onView(withId(R.id.createAccountBackButton)).check(matches(isDisplayed()))

        //Personal
        onView(withId(R.id.createPersonalAccountTitleTextView)).check(matches(isDisplayed()))
        onView(withId(R.id.createPersonalAccountDescriptionTextView)).check(matches(isDisplayed()))

        //Pro
        onView(withId(R.id.createProAccountTitleTextView)).check(matches(isDisplayed()))
        onView(withId(R.id.createProAccountDescriptionTextView)).check(matches(isDisplayed()))
    }

    @Test
    fun launch_uiElementsVisibleInLandscape() = rotateScreen {
        launch_uiElementsVisible()
    }

    @Test
    fun personal_uiElementsVisible() {
        onView(withId(R.id.createPersonalAccountTitleTextView)).perform(click())

        onView(withText(R.string.create_personal_account_title)).check(matches(isDisplayed()))
        onView(withHint(R.string.create_personal_account_email_input_hint)).check(matches(isDisplayed()))
    }

    @Test
    fun personal_uiElementsVisibleInLandscape() = rotateScreen {
        personal_uiElementsVisible()
    }
}
