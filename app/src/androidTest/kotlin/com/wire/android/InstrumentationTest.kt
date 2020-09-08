package com.wire.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith

@Suppress("UnnecessaryAbstractClass")
@RunWith(AndroidJUnit4::class)
abstract class InstrumentationTest {

    protected val appContext get() = ApplicationProvider.getApplicationContext<Context>()
}
