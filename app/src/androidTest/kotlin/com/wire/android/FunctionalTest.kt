package com.wire.android

import android.app.Activity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import org.junit.Rule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
abstract class FunctionalTest(clazz: Class<out Activity>) {

    @get:Rule
    val activityRule = ActivityTestRule(clazz)
}