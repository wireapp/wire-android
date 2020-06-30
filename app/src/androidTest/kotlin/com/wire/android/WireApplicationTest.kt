package com.wire.android

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test

class WireApplicationTest : InstrumentationTest() {
    @Test
    fun useAppContextWithRightPackage() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assert(appContext.packageName.startsWith("com.wire.android"))
    }
}
