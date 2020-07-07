package com.wire.android.core.accessibility

import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityManager
import com.wire.android.UnitTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`

class AccessibilityTest : UnitTest() {

    private lateinit var accessibility: Accessibility

    @Mock
    private lateinit var accessibilityManager: AccessibilityManager

    @Mock
    private lateinit var accessibilityServiceInfo: AccessibilityServiceInfo

    @Before
    fun setup() {
        accessibility = Accessibility(accessibilityManager)
    }

    @Test
    fun `given an accessibility manager, when it is not enabled, then isTalkbackEnabled should be false`() {
        `when`(accessibilityManager.isEnabled).thenReturn(false)
        assertThat(accessibility.isTalkbackEnabled()).isFalse()
    }


    @Test
    fun `given an accessibility manager, when it is enabled and services list is empty, then isTalkbackEnabled is false`() {
        `when`(accessibilityManager.isEnabled).thenReturn(true)
        assertThat(accessibility.isTalkbackEnabled()).isFalse()
    }

    @Test
    fun `given an accessibility manager, when it is enabled and services list does contain Talkback, then isTalkbackEnabled is true`() {
        val list = listOf(accessibilityServiceInfo)
        `when`(accessibilityManager.isEnabled).thenReturn(true)
        `when`(accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN)).thenReturn(
            list
        )
        assertThat(accessibility.isTalkbackEnabled()).isTrue()
    }
}
