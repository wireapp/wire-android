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
import com.wire.android.feature.welcome.WelcomeFragment
import com.wire.android.framework.async.awaitResult
import com.wire.android.framework.retry.RetryTestRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch

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
        val positiveClickLatch = CountDownLatch(1)
        val negativeClickLatch = CountDownLatch(1)
        val neutralClickLatch = CountDownLatch(1)

        fun showDialogWithButtons() {
            fragmentScenario.onFragment {
                dialogBuilder.showDialog(it.requireContext()) {
                    setMessage(TEST_MESSAGE)
                    setPositiveButton(TEST_POSITIVE_BUTTON_TEXT) { _, _ -> positiveClickLatch.countDown() }
                    setNegativeButton(TEST_NEGATIVE_BUTTON_TEXT) { _, _ -> negativeClickLatch.countDown() }
                    setNeutralButton(TEST_NEUTRAL_BUTTON_TEXT) { _, _ -> neutralClickLatch.countDown() }
                }
            }
        }

        //positive button
        showDialogWithButtons()
        Thread.sleep(1000)
        onView(withText(TEST_MESSAGE)).check(matches(isDisplayed()))
        onView(withText(TEST_POSITIVE_BUTTON_TEXT)).perform(click())
        positiveClickLatch.awaitResult(BUTTON_CLICK_TIMEOUT)

        //negative button
        showDialogWithButtons()
        Thread.sleep(1000)
        onView(withText(TEST_NEGATIVE_BUTTON_TEXT)).perform(click())
        negativeClickLatch.awaitResult(BUTTON_CLICK_TIMEOUT)

        //neutral button
        showDialogWithButtons()
        Thread.sleep(1000)
        onView(withText(TEST_NEUTRAL_BUTTON_TEXT)).perform(click())
        neutralClickLatch.awaitResult(BUTTON_CLICK_TIMEOUT)
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
        private const val BUTTON_CLICK_TIMEOUT = 3L
    }
}
