package com.wire.android

import android.app.Activity
import androidx.test.espresso.accessibility.AccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.UiDevice
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesCheckNames
import org.hamcrest.core.Is.`is`
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.runner.RunWith

open class FunctionalActivityTest(clazz: Class<out Activity>) : FunctionalTest() {

    @get:Rule
    val activityRule = ActivityTestRule(clazz)
}

@RunWith(AndroidJUnit4::class)
@LargeTest
open class FunctionalTest {

    val uiDevice = UiDevice.getInstance(getInstrumentation())

    fun rotateScreen(block: () -> Unit) = with(uiDevice) {
        setOrientationLeft()
        block()
        setOrientationNatural()
    }

    companion object {
        @BeforeClass
        @JvmStatic
        fun enableAllChecks() {
            AccessibilityChecks.enable()
                .setRunChecksFromRootView(true)
                .setSuppressingResultMatcher(matchesCheckNames(`is`("TouchTargetSizeViewCheck")))
        }
    }
}
