package com.wire.android.feature.auth.login

import android.content.Intent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.matcher.ViewMatchers.*
import com.wire.android.FunctionalActivityTest
import com.wire.android.R
import org.junit.After
import org.junit.Before
import org.junit.Test

class LoginActivityUITest : FunctionalActivityTest(LoginActivity::class.java) {

    @Before
    fun init() {
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun launch_uiElementsVisible() {
        onView(withId(R.id.loginBackButton)).check(matches(isDisplayed()))
        onView(withId(R.id.loginTitleTextView)).check(matches(isDisplayed()))

        onView(withText(R.string.authentication_tab_layout_title_email)).check(matches(isDisplayed()))
        onView(withText(R.string.authentication_tab_layout_title_phone)).check(matches(isDisplayed()))
    }

    @Test
    fun launch_uiElementsVisibleInLandscape() = rotateScreen {
        launch_uiElementsVisible()
    }

    @Test
    fun forgotPassword_click_opensChangePasswordUrl() {
        onView(withId(R.id.loginForgotPasswordButton)).perform(click())
        Intents.intended(hasAction(Intent.ACTION_VIEW))
    }
}
