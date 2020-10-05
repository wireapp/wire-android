package com.wire.android.core.ui.navigation

import android.content.Intent
import android.net.Uri
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragment
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import com.wire.android.InstrumentationTest
import com.wire.android.R
import com.wire.android.feature.welcome.WelcomeFragment
import org.hamcrest.CoreMatchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Test

class UriNavigationHandlerTest : InstrumentationTest() {

    private lateinit var fragmentScenario: FragmentScenario<*>

    private lateinit var uriNavigationHandler: UriNavigationHandler

    @Before
    fun setUp() {
        fragmentScenario = launchFragment<WelcomeFragment>(themeResId = R.style.AppTheme_Authentication)
        fragmentScenario.moveToState(Lifecycle.State.RESUMED)

        uriNavigationHandler = UriNavigationHandler()
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun openUri_withUri_startsNewActivityWithViewAction() {
        fragmentScenario.onFragment {
            uriNavigationHandler.openUri(it.requireContext(), TEST_URI)
        }
        intended(allOf(hasAction(Intent.ACTION_VIEW), hasData(TEST_URI)))
    }

    @Test
    fun openUri_withUriString_startsNewActivityWithViewAction() {
        fragmentScenario.onFragment {
            uriNavigationHandler.openUri(it.requireContext(), TEST_URI_STRING)
        }

        intended(allOf(hasAction(Intent.ACTION_VIEW), hasData(Uri.parse(TEST_URI_STRING))))
    }

    companion object {
        private const val TEST_URI_STRING = "https://wire.com/"
        private val TEST_URI = Uri.parse(TEST_URI_STRING)
    }
}
