package com.android.wire

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test

class WireApplicationTest : InstrumentationTest() {
    @Test
    fun useAppContextWithRightPackage() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.wire.android", appContext.packageName)
    }
}