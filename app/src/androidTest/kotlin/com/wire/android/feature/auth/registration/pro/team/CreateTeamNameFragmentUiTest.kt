package com.wire.android.feature.auth.registration.pro.team

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.wire.android.FunctionalTest
import com.wire.android.R
import com.wire.android.core.extension.EMPTY
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class CreateTeamNameFragmentUiTest : FunctionalTest() {

    @Before
    fun setup() {
        launchFragmentInContainer<CreateProAccountTeamNameFragment>(factory = object : FragmentFactory() {
            override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
                return CreateProAccountTeamNameFragment.newInstance()
            }
        }, themeResId = R.style.AppTheme)
    }

    @Test
    fun uiElementsVisible() {
        onView(withText(R.string.create_pro_account_set_team_name_title)).check(matches(isDisplayed()))
        onView(withText(R.string.create_pro_account_set_team_name_subtitle)).check(matches(isDisplayed()))
        onView(withText(R.string.create_pro_account_set_team_name_about)).check(matches(isDisplayed()))

        onView(withHint(R.string.create_pro_account_set_team_name_hint)).check(matches(isDisplayed()))

        onView(withId(R.id.createProAccountTeamNameInputConfirmationButton)).check(matches(isDisplayed()))
        onView(allOf(withId(R.id.createProAccountTeamNameInputConfirmationButton), not(isEnabled())))

    }

    @Test
    fun inputText_confirmationButtonShouldBeEnabled() {
        onView(withId(R.id.createProAccountTeamNameEditText)).perform(typeTextIntoFocusedView(TEST_TEAM_NAME))
        onView(withId(R.id.createProAccountTeamNameInputConfirmationButton)).check(matches(isEnabled()))
    }

    @Test
    fun inputText_confirmationButtonShouldBeDisabled() {
        onView(withId(R.id.createProAccountTeamNameEditText)).perform(typeText(String.EMPTY))
        onView(allOf(withId(R.id.createProAccountTeamNameInputConfirmationButton), not(isEnabled())))
    }

    @Test
    fun uiElementsVisibleInLandscape() = rotateScreen {
        uiElementsVisible()
    }

    companion object {
        private const val TEST_TEAM_NAME = "Team"
    }
}
