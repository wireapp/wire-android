package com.wire.android

import android.app.Activity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.UiDevice
import org.junit.Rule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
open class FunctionalTest(clazz: Class<out Activity>) {

    @get:Rule
    val activityRule = ActivityTestRule(clazz)

    val uiDevice = UiDevice.getInstance(getInstrumentation())

    fun rotateScreen(block: () -> Unit) = with(uiDevice) {
        setOrientationLeft()
        block()
        setOrientationNatural()
    }
}
