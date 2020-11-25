package com.wire.android.feature.auth.registration.pro.email.verification

import androidx.core.os.bundleOf
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import com.wire.android.FunctionalTest
import com.wire.android.R
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test

class CreateProAccountEmailVerificationCodeFragmentTest : FunctionalTest() {

    private lateinit var scenario: FragmentScenario<CreateProAccountTeamEmailVerificationFragment>

    @Before
    fun setUp() {
        scenario = launchFragmentInContainer(
            bundleOf("email" to ARG_EMAIL),
            themeResId = R.style.AppTheme_Authentication
        )
    }

    @Test
    fun emailDescription_hasCorrectEmailFromBundle() {
        Espresso.onView(ViewMatchers.withId(R.id.createProAccountEmailVerificationCodeDescriptionTextView))
            .check(ViewAssertions.matches(ViewMatchers.withText(Matchers.containsString(ARG_EMAIL))))
    }

    //TODO find a way to mock VM and add tests here

    companion object {
        private const val ARG_EMAIL = "test@wire.com"
    }
}