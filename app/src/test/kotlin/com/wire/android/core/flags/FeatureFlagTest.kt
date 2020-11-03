package com.wire.android.core.flags

import com.wire.android.UnitTest
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class FeatureFlagTest : UnitTest() {

    @Test
    fun `given an enabled state for a feature flag, then logic should be executed`() {
        val activeFlag = ActiveFeatureFlag()
        val fakeNavigator = mockk<Navigator>(relaxed = true)

        activeFlag whenActivated {
            fakeNavigator.doSomething()
            fakeNavigator.navigateToActiveFeature()
            fakeNavigator.doSomething()
        }

        verify(exactly = 1) { fakeNavigator.navigateToActiveFeature() }
        verify(exactly = 2) { fakeNavigator.doSomething() }
    }

    @Test
    fun `given a disable state for a feature flag, then logic should not be executed`() {
        val inactiveFlag = InactiveFeatureFlag()
        val fakeNavigator = mockk<Navigator>(relaxed = true)

        inactiveFlag whenActivated {
            fakeNavigator.doSomething()
            fakeNavigator.navigateToActiveFeature()
            fakeNavigator.doSomething()
        }

        verify { fakeNavigator wasNot Called }
    }

    @Test
    fun `given an enabled state for a feature flag, then logic should on the "otherwise" block should not be executed`() {
        val activeFlag = ActiveFeatureFlag()
        val fakeNavigator = mockk<Navigator>(relaxed = true)

        activeFlag whenActivated {
            fakeNavigator.doSomething()
            fakeNavigator.navigateToActiveFeature()
            fakeNavigator.doSomething()
        } otherwise {
            fakeNavigator.navigateToDefaultScreen()
        }

        verify(exactly = 1) { fakeNavigator.navigateToActiveFeature() }
        verify(exactly = 2) { fakeNavigator.doSomething() }
        verify(exactly = 0) { fakeNavigator.navigateToDefaultScreen() }
    }

    @Test
    fun `given a disabled state for a feature flag, then logic should on the "otherwise" block should be executed`() {
        val inactiveFlag = InactiveFeatureFlag()
        val fakeNavigator = mockk<Navigator>(relaxed = true)

        inactiveFlag whenActivated {
            fakeNavigator.doSomething()
            fakeNavigator.navigateToActiveFeature()
            fakeNavigator.doSomething()
        } otherwise {
            fakeNavigator.navigateToDefaultScreen()
        }

        verify(exactly = 0) { fakeNavigator.navigateToActiveFeature() }
        verify(exactly = 0) { fakeNavigator.doSomething() }
        verify(exactly = 1) { fakeNavigator.navigateToDefaultScreen() }
    }

    private class ActiveFeatureFlag : FeatureFlag(enabled = true)
    private class InactiveFeatureFlag : FeatureFlag(enabled = false)

    private class Navigator {
        fun doSomething() { this.hashCode() }
        fun navigateToActiveFeature() { this.hashCode() }
        fun navigateToDefaultScreen() { this.hashCode() }
    }
}
