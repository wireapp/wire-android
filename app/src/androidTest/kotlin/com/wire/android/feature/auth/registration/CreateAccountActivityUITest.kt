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
        //TODO check other elements
        onView(withId(R.id.createAccountBackButton)).check(matches(isDisplayed()))
    }
}

