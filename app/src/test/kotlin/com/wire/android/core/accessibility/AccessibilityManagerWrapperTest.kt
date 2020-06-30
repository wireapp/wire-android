package com.wire.android.core.accessibility

import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityManager
import com.wire.android.UnitTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`

class AccessibilityManagerWrapperTest : UnitTest() {

    private lateinit var accessibilityManagerWrapper: AccessibilityManagerWrapper

    @Mock
    private lateinit var accessibilityManager: AccessibilityManager

    @Mock
    private lateinit var accessibilityServiceInfo: AccessibilityServiceInfo

    @Before
    fun setup() {
        accessibilityManagerWrapper = AccessibilityManagerWrapper(accessibilityManager)
    }

    @Test
    fun `given an accessibility manager, when it is not enabled, then isTalkbackEnabled should be false`() {
        `when`(accessibilityManager.isEnabled).thenReturn(false)
        assertThat(accessibilityManagerWrapper.isTalkbackEnabled()).isFalse()
    }


    @Test
    fun `given an accessibility manager, when it is enabled and services list does not contains Talkback, then isTalkbackEnabled should be false`() {
        `when`(accessibilityManager.isEnabled).thenReturn(true)
        assertThat(accessibilityManagerWrapper.isTalkbackEnabled()).isFalse()
    }

    @Test
    fun `given an accessibility manager, when it is enabled and services list does contain Talkback, then isTalkbackEnabled should be false`() {
        val list = listOf(accessibilityServiceInfo)
        `when`(accessibilityManager.isEnabled).thenReturn(true)
        `when`(accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN)).thenReturn(list)
        assertThat(accessibilityManagerWrapper.isTalkbackEnabled()).isTrue()
    }
}