package com.wire.android

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.junit.Test

class MainActivityTest : FunctionalTest(MainActivity::class.java) {

    @Test
    fun checkHelloMessage() {
        onView(withId(R.id.hello)).check(matches(isDisplayed()))
    }
}