package com.wire.android.feature.welcome

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.wire.android.FunctionalTest
import com.wire.android.R
import org.junit.Test

class WelcomeActivityUITest : FunctionalTest(WelcomeActivity::class.java) {

    @Test
    fun launch_uiElementsVisible() {
        onView(withText(R.string.welcome_text)).check(matches(isDisplayed()))
        onView(withText(R.string.welcome_create_account)).check(matches(isDisplayed()))
        onView(withText(R.string.welcome_login)).check(matches(isDisplayed()))
        onView(withText(R.string.welcome_enterprise_login)).check(matches(isDisplayed()))
    }

    @Test
    fun launch_uiElementsVisibleInLandscape() = rotateScreen {
        launch_uiElementsVisible()
    }
}
