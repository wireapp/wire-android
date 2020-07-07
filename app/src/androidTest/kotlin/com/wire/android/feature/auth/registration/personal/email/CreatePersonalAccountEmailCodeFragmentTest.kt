package com.wire.android.feature.auth.registration.personal.email

import androidx.core.os.bundleOf
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.wire.android.FunctionalTest
import com.wire.android.R
import org.hamcrest.Matchers.containsString
import org.junit.Before
import org.junit.Test

class CreatePersonalAccountEmailCodeFragmentTest : FunctionalTest() {

    private lateinit var scenario: FragmentScenario<CreatePersonalAccountEmailCodeFragment>

    @Before
    fun setUp() {
        scenario = launchFragmentInContainer(
            bundleOf("email" to ARG_EMAIL),
            themeResId = R.style.AppTheme_Authentication
        )
    }

    @Test
    fun emailDescription_hasCorrectEmailFromBundle() {
        onView(withId(R.id.createPersonalAccountEmailCodeDescriptionTextView)).check(matches(withText(containsString(ARG_EMAIL))))
    }

    //TODO find a way to mock VM and add tests here

    companion object {
        private const val ARG_EMAIL = "test@wire.com"
    }
}
