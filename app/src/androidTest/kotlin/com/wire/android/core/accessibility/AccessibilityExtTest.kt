package com.wire.android.core.accessibility

import android.view.View
import androidx.test.platform.app.InstrumentationRegistry
import com.wire.android.InstrumentationTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`

class AccessibilityExtTest : InstrumentationTest() {

    private lateinit var accessibilityConfig: AccessibilityConfig

    private lateinit var view: View

    @Before
    fun setup() {
        view = View(InstrumentationRegistry.getInstrumentation().context)
    }

    @Test
    fun givenAHeaderView_whenIsHeadingIsTrueAndAndroidSDKIs28OrAbove_thenSetIsAccessibilityHeaderToTrue() {
        `when`(accessibilityConfig.headingVersionCompatible()).thenReturn(true)

        view.headingForAccessibility(true, accessibilityConfig)

        assertTrue(view.isAccessibilityHeading)
    }

    @Test
    fun givenAHeaderView_whenIsHeadingIsFalseAndAndroidSDKIs28OrAbove_thenSetIsAccessibilityHeaderToTrue() {
        `when`(accessibilityConfig.headingVersionCompatible()).thenReturn(false)

        view.headingForAccessibility(false, accessibilityConfig)

        assertFalse(view.isAccessibilityHeading)
    }
}
