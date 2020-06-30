package com.wire.android.core.accessibility

import android.view.View
import androidx.test.platform.app.InstrumentationRegistry
import com.wire.android.InstrumentationTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AccessibilityExtTest : InstrumentationTest() {

    private lateinit var aboveAndroidP: AboveAndroidP

    private lateinit var view: View

    @Before
    fun setup() {
        view = View(InstrumentationRegistry.getInstrumentation().context)
    }

    @Test
    fun givenAHeaderView_whenIsHeadingIsTrueAndAndroidSDKIs28OrAbove_thenSetIsAccessibilityHeaderToTrue() {
        aboveAndroidP = AboveAndroidP(true)

        view.headingForAccessibility(true, aboveAndroidP)

        assertTrue(view.isAccessibilityHeading)
    }

    @Test
    fun givenAHeaderView_whenIsHeadingIsFalseAndAndroidSDKIs28OrAbove_thenSetIsAccessibilityHeaderToTrue() {
        aboveAndroidP = AboveAndroidP(true)

        view.headingForAccessibility(false, aboveAndroidP)

        assertFalse(view.isAccessibilityHeading)
    }
}