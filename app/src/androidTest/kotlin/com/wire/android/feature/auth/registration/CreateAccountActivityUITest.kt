package com.wire.android.feature.auth.registration

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
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
    fun launch_uiElementsVisibleInLandscape() = inRotatedMode {
        launch_uiElementsVisible()
    }
}

