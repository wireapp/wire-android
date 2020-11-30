package com.wire.android.core.extension

import android.widget.TextView
import androidx.fragment.app.testing.launchFragmentInContainer
import com.wire.android.FunctionalTest
import com.wire.android.R
import com.wire.android.feature.welcome.ui.WelcomeFragment
import kotlinx.android.synthetic.main.fragment_welcome.*
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.Test

class ViewExtensionsTest : FunctionalTest() {

    @Test
    fun afterMeasured_widthOrHeightNotZero_executesActionDirectly() {
        launchFragmentInContainer<WelcomeFragment>(themeResId = R.style.AppTheme_Authentication).onFragment {
            val view = it.welcomeTitleTextView
            view.width shouldNotBeEqualTo 0
            view.height shouldNotBeEqualTo 0

            var actionExecuted = false

            view.afterMeasured { actionExecuted = true }

            actionExecuted shouldBeEqualTo true
        }
    }

    @Test
    fun afterMeasured_widthOrHeightZero_executesActionAfterLayoutChange() {
        launchFragmentInContainer<WelcomeFragment>(themeResId = R.style.AppTheme_Authentication).onFragment {
            val view = TextView(it.context)
            var actionExecuted = false

            view.afterMeasured { actionExecuted = true }

            actionExecuted shouldBeEqualTo false

            //trigger layout change:
            view.layout(0, 0, 100, 100)

            actionExecuted shouldBeEqualTo true
        }
    }
}
