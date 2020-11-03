package com.wire.android.core.ui.dialog

import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragment
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.wire.android.InstrumentationTest
import com.wire.android.R
import com.wire.android.feature.welcome.ui.WelcomeFragment
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

@Ignore("The tests are very flaky therefore need to be fine-tuned")
class DialogBuilderTest : InstrumentationTest() {

    private lateinit var fragmentScenario: FragmentScenario<WelcomeFragment>

    private lateinit var dialogBuilder: DialogBuilder

    @Before
    fun setUp() {
        dialogBuilder = DialogBuilder()
        fragmentScenario = launchFragment(themeResId = R.style.AppTheme_Authentication)
    }

    @Test
    fun showDialog_withTitleAndMessage_displaysTitleAndMessage() {
        fragmentScenario.onFragment {
            dialogBuilder.showDialog(it.requireContext()) {
                setTitle(TEST_TITLE)
                setMessage(TEST_MESSAGE)
                setPositiveButton(TEST_POSITIVE_BUTTON_TEXT, DialogBuilder.NO_OP_LISTENER)
            }
        }

        onView(withText(TEST_TITLE)).check(matches(isDisplayed()))
        onView(withText(TEST_MESSAGE)).check(matches(isDisplayed()))
        onView(withText(TEST_POSITIVE_BUTTON_TEXT)).perform(click()) //dismiss
    }

    @Test
    fun showDialog_withButtons_performsButtonClicks() {
        val positiveClickHelper = mockk<DialogClickAssertionHelper>(name = "positive", relaxUnitFun = true)
        val negativeClickHelper = mockk<DialogClickAssertionHelper>(name = "negative", relaxUnitFun = true)
        val neutralClickHelper = mockk<DialogClickAssertionHelper>(name = "neutral", relaxUnitFun = true)

        fun showDialogWithButtons() {
            fragmentScenario.onFragment {
                dialogBuilder.showDialog(it.requireContext()) {
                    setMessage(TEST_MESSAGE)
                    setPositiveButton(TEST_POSITIVE_BUTTON_TEXT) { _, _ -> positiveClickHelper.clicked() }
                    setNegativeButton(TEST_NEGATIVE_BUTTON_TEXT) { _, _ -> negativeClickHelper.clicked() }
                    setNeutralButton(TEST_NEUTRAL_BUTTON_TEXT) { _, _ -> neutralClickHelper.clicked() }
                }
            }
        }

        //positive button
        showDialogWithButtons()
        onView(withText(TEST_MESSAGE)).check(matches(isDisplayed()))
        onView(withText(TEST_POSITIVE_BUTTON_TEXT)).perform(click())
        verify(timeout = BUTTON_CLICK_TIMEOUT) { positiveClickHelper.clicked() }

        //negative button
        showDialogWithButtons()
        onView(withText(TEST_NEGATIVE_BUTTON_TEXT)).perform(click())
        verify(timeout = BUTTON_CLICK_TIMEOUT) { negativeClickHelper.clicked() }

        //neutral button
        showDialogWithButtons()
        onView(withText(TEST_NEUTRAL_BUTTON_TEXT)).perform(click())
        verify(timeout = BUTTON_CLICK_TIMEOUT) { neutralClickHelper.clicked() }
    }

    @Test
    fun showErrorDialog_displaysMessageTitleAndOKButton() {
        val errorMessage = ErrorMessage(R.string.app_name, R.string.authentication_create_account_subtitle)

        fragmentScenario.onFragment {
            dialogBuilder.showErrorDialog(it.requireContext(), errorMessage)
        }

        onView(withText(errorMessage.title!!)).check(matches(isDisplayed()))
        onView(withText(errorMessage.message)).check(matches(isDisplayed()))
        onView(withText(R.string.ok)).perform(click())
    }

    companion object {
        private const val TEST_TITLE = "title"
        private const val TEST_MESSAGE =
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."
        private const val TEST_POSITIVE_BUTTON_TEXT = "Positive"
        private const val TEST_NEGATIVE_BUTTON_TEXT = "Negative"
        private const val TEST_NEUTRAL_BUTTON_TEXT = "Neutral"
        private const val BUTTON_CLICK_TIMEOUT = 3000L
    }

    private class DialogClickAssertionHelper {
        @Suppress("EmptyFunctionBlock")
        fun clicked() {}
    }
}
