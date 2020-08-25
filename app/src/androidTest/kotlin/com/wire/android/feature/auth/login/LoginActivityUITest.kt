package com.wire.android.feature.auth.login

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.wire.android.FunctionalActivityTest
import com.wire.android.R
import org.junit.Test

class LoginActivityUITest : FunctionalActivityTest(LoginActivity::class.java) {

    @Test
    fun launch_uiElementsVisible() {
        onView(withId(R.id.loginBackButton)).check(matches(isDisplayed()))
        onView(withId(R.id.loginTitleTextView)).check(matches(isDisplayed()))
    }

    @Test
    fun launch_uiElementsVisibleInLandscape() = rotateScreen {
        launch_uiElementsVisible()
    }
}
