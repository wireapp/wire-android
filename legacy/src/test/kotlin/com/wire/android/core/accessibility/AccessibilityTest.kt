package com.wire.android.core.accessibility

import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityManager
import com.wire.android.UnitTest
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.amshove.kluent.shouldBe
import org.junit.Before
import org.junit.Test

class AccessibilityTest : UnitTest() {

    private lateinit var accessibility: Accessibility

    @MockK
    private lateinit var accessibilityManager: AccessibilityManager

    @MockK
    private lateinit var accessibilityServiceInfo: AccessibilityServiceInfo

    @Before
    fun setup() {
        accessibility = Accessibility(accessibilityManager)
    }

    @Test
    fun `given an accessibility manager, when it is not enabled, then isTalkbackEnabled should be false`() {
        every { accessibilityManager.isEnabled } returns false

        accessibility.isTalkbackEnabled() shouldBe false
    }

    @Test
    fun `given an accessibility manager, when it is enabled and services list is empty, then isTalkbackEnabled is false`() {
        every { accessibilityManager.isEnabled } returns true
        every { accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN) } returns emptyList()

        accessibility.isTalkbackEnabled() shouldBe false
    }

    @Test
    fun `given an accessibility manager, when it is enabled and services list does contain Talkback, then isTalkbackEnabled is true`() {
        val list = listOf(accessibilityServiceInfo)

        every { accessibilityManager.isEnabled } returns true
        every { accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN) } returns list

        accessibility.isTalkbackEnabled() shouldBe true
    }
}
