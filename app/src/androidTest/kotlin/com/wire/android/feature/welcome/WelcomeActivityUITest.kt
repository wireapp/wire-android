package com.wire.android.feature.welcome

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.*
import com.wire.android.FunctionalActivityTest
import com.wire.android.R
import com.wire.android.feature.auth.login.LoginActivity
import com.wire.android.feature.auth.registration.CreateAccountActivity
import org.junit.After
import org.junit.Before
import org.junit.Test

class WelcomeActivityUITest : FunctionalActivityTest(WelcomeActivity::class.java) {

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
        onView(withText(R.string.welcome_text)).check(matches(isDisplayed()))
        onView(withText(R.string.welcome_create_account)).check(matches(isDisplayed()))
        onView(withText(R.string.welcome_login)).check(matches(isDisplayed()))
        onView(withText(R.string.welcome_enterprise_login)).check(matches(isDisplayed()))
    }

    @Test
    fun launch_uiElementsVisibleInLandscape() = rotateScreen {
        launch_uiElementsVisible()
    }

    @Test
    fun createAccountButtonClick() {
        onView(withId(R.id.welcomeCreateAccountButton)).check(matches(isDisplayed()))
        onView(withId(R.id.welcomeCreateAccountButton)).perform(click())
        intended(hasComponent(CreateAccountActivity::class.java.name))
    }

    @Test
    fun loginButtonClick() {
        onView(withId(R.id.welcomeLoginButton)).check(matches(isDisplayed()))
        onView(withId(R.id.welcomeLoginButton)).perform(click())
        intended(hasComponent(LoginActivity::class.java.name))
    }
}
