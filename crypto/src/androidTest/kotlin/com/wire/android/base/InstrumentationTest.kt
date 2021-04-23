package com.wire.android.base

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.runner.RunWith

@Suppress("UnnecessaryAbstractClass")
@RunWith(AndroidJUnit4::class)
abstract class InstrumentationTest {

    protected val appContext get() = ApplicationProvider.getApplicationContext<Context>()

    @Suppress("LeakingThis")
    @Rule
    @JvmField
    val injectMocksRule = InjectMockKsRule.create(this)

}
