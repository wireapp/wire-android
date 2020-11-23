package com.wire.android

import android.app.Activity
import androidx.test.espresso.accessibility.AccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.UiDevice
import com.wire.android.framework.retry.RetryTestRule
import org.junit.Rule
import org.junit.runner.RunWith

@Suppress("UnnecessaryAbstractClass")
abstract class FunctionalActivityTest(clazz: Class<out Activity>) : FunctionalTest() {

    @get:Rule
    val activityRule = ActivityTestRule(clazz)

    @get:Rule
    val retryTestRule = RetryTestRule()
}

@Suppress("UnnecessaryAbstractClass")
@RunWith(AndroidJUnit4::class)
@LargeTest
abstract class FunctionalTest {

    private val uiDevice: UiDevice = UiDevice.getInstance(getInstrumentation())

    fun rotateScreen(block: () -> Unit) = with(uiDevice) {
        setOrientationLeft()
        block()
        setOrientationNatural()
    }

    companion object {
        init {
            AccessibilityChecks.enable()
                .setRunChecksFromRootView(true)
                .setThrowExceptionForErrors(false)
        }
    }
}
