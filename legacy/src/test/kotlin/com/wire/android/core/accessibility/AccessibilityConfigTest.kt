package com.wire.android.core.accessibility

import android.os.Build
import com.wire.android.UnitTest
import com.wire.android.core.compatibility.Compatibility
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.amshove.kluent.shouldBe
import org.junit.Before
import org.junit.Test

class AccessibilityConfigTest : UnitTest() {

    private lateinit var accessibilityConfig: AccessibilityConfig

    @MockK
    private lateinit var compatibility: Compatibility

    @Before
    fun setup() {
        accessibilityConfig = AccessibilityConfig(compatibility)
    }

    @Test
    fun `given Android P is required for header compatibility, when build is P or above, then return true`() {
        every { compatibility.supportsAndroidVersion(Build.VERSION_CODES.P) } returns true

        accessibilityConfig.headingVersionCompatible() shouldBe true
    }

    @Test
    fun `given Android P is required for header compatibility, when build is below P, then return false`() {
        every { compatibility.supportsAndroidVersion(Build.VERSION_CODES.P) } returns false

        accessibilityConfig.headingVersionCompatible() shouldBe false
    }
}
