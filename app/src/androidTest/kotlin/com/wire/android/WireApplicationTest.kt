package com.wire.android

import androidx.test.platform.app.InstrumentationRegistry
import com.wire.android.InstrumentationTest
import org.junit.Assert.assertEquals
import org.junit.Test

class WireApplicationTest : InstrumentationTest() {
    @Test
    fun useAppContextWithRightPackage() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.wire.android", appContext.packageName)
    }
}
