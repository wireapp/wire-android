package com.wire.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wire.android.framework.retry.RetryTestRule
import org.junit.Rule
import org.junit.runner.RunWith

@Suppress("UnnecessaryAbstractClass")
@RunWith(AndroidJUnit4::class)
abstract class InstrumentationTest {

    protected val appContext get() = ApplicationProvider.getApplicationContext<Context>()

    @get:Rule
    val retryTestRule = RetryTestRule()
}
