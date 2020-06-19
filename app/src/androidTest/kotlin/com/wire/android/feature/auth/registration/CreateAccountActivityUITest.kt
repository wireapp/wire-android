package com.wire.android.feature.auth.registration

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.wire.android.FunctionalTest
import com.wire.android.R
import org.junit.Test

class CreateAccountActivityUITest : FunctionalTest(CreateAccountActivity::class.java) {

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
        onView(withText(R.string.authentication_tab_layout_title_email)).check(matches(isDisplayed()))
        onView(withText(R.string.authentication_tab_layout_title_phone)).check(matches(isDisplayed()))

        //TODO: check whether Email entry area is visible
    }

    @Test
    fun personal_uiElementsVisibleInLandscape() = rotateScreen {
        personal_uiElementsVisible()
    }
}
