package com.wire.android.feature.auth.registration

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.wire.android.FunctionalActivityTest
import com.wire.android.R
import com.wire.android.core.exception.Conflict
import com.wire.android.core.exception.Forbidden
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.activation.ActivationRepository
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.koin.test.mock.declareMock
import org.mockito.Mockito.`when`

class CreateAccountActivityUITest : FunctionalActivityTest(CreateAccountActivity::class.java) {

    @Before
    fun setup() {
        declareMock<ActivationRepository>().let {
            runBlocking {
                `when`(it.sendEmailActivationCode(VALID_EMAIL)).thenReturn(Either.Right(Unit))
                `when`(it.sendEmailActivationCode(FORBIDDEN_EMAIL)).thenReturn(Either.Left(Forbidden))
                `when`(it.sendEmailActivationCode(CONFLICT_EMAIL)).thenReturn(Either.Left(Conflict))
            }
        }
    }

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
    fun launch_uiElementsVisibleInLandscape() = rotateScreen {
        launch_uiElementsVisible()
    }

    @Test
    fun personal_uiElementsVisible() {
        onView(withId(R.id.createPersonalAccountLayoutContainer)).perform(click())

        onView(withText(R.string.create_personal_account_title)).check(matches(isDisplayed()))
        onView(withText(R.string.authentication_tab_layout_title_email)).check(matches(isDisplayed()))
        onView(withText(R.string.authentication_tab_layout_title_phone)).check(matches(isDisplayed()))

        onView(withId(R.id.createPersonalAccountEmailTextInputLayout)).check(matches(isDisplayed()))
    }

    @Test
    fun personal_uiElementsVisibleInLandscape() = rotateScreen {
        personal_uiElementsVisible()
    }

    @Test
    fun personalEmailScreen_validEmailFilled_opensCodeVerificationScreen() {
        onView(withId(R.id.createPersonalAccountLayoutContainer)).perform(click())
        onView(withId(R.id.createPersonalAccountEmailEditText)).perform(replaceText(VALID_EMAIL))

        onView(withId(R.id.createPersonalAccountEmailConfirmationButton)).let {
            it.check(matches(isEnabled()))
            it.perform(click())
        }

        onView(withText(R.string.create_personal_account_email_code_title)).check(matches(isDisplayed()))
    }

    @Test
    fun personalEmailScreen_forbiddenEmailFilled_showsError() {
        onView(withId(R.id.createPersonalAccountLayoutContainer)).perform(click())
        onView(withId(R.id.createPersonalAccountEmailEditText)).perform(replaceText(FORBIDDEN_EMAIL))

        onView(withId(R.id.createPersonalAccountEmailConfirmationButton)).perform(click())

        //TODO: validate after proper dialog is added
    }

    @Test
    fun personalEmailScreen_conflictEmailFilled_showsError() {
        onView(withId(R.id.createPersonalAccountLayoutContainer)).perform(click())
        onView(withId(R.id.createPersonalAccountEmailEditText)).perform(replaceText(CONFLICT_EMAIL))

        onView(withId(R.id.createPersonalAccountEmailConfirmationButton)).perform(click())

        //TODO: validate after proper dialog is added
    }

    companion object {
        private const val VALID_EMAIL = "test@wire.com"
        private const val FORBIDDEN_EMAIL = "forbidden@wire.com"
        private const val CONFLICT_EMAIL = "conflict@wire.com"
    }
}
