package com.wire.android.core.accessibility

import android.os.Build
import com.wire.android.UnitTest
import com.wire.android.core.compatibility.Compatibility
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`

class AccessibilityConfigTest : UnitTest() {

    private lateinit var accessibilityConfig: AccessibilityConfig

    @Mock
    private lateinit var compatibility: Compatibility

    @Before
    fun setup() {
        accessibilityConfig = AccessibilityConfig(compatibility)
    }

    @Test
    fun `given Android P is required for header compatibility, when build is P or above, then return true`() {
        `when`(compatibility.supportsAndroidVersion(Build.VERSION_CODES.P)).thenReturn(true)

        assertThat(accessibilityConfig.headingVersionCompatible()).isTrue()
    }

    @Test
    fun `given Android P is required for header compatibility, when build is below P, then return false`() {
        `when`(compatibility.supportsAndroidVersion(Build.VERSION_CODES.P)).thenReturn(false)
        assertThat(accessibilityConfig.headingVersionCompatible()).isFalse()

    }
}
