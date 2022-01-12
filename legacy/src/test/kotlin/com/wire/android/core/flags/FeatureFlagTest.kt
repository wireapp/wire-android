package com.wire.android.core.flags

import com.wire.android.UnitTest
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class FeatureFlagTest : UnitTest() {

    @Test
    fun `given a feature flag, when it is activated, then executes given logic block`() {
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
    fun `given a feature flag, when it is deactivated, then does not execute given logic block`() {
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
    fun `given a feature flag, when it is activated, then does not execute given otherwise logic block`() {
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
        verify(inverse = true) { fakeNavigator.navigateToDefaultScreen() }
    }

    @Test
    fun `given a feature flag, when it is deactivated, then execute given otherwise logic block`() {
        val inactiveFlag = InactiveFeatureFlag()
        val fakeNavigator = mockk<Navigator>(relaxed = true)

        inactiveFlag whenActivated {
            fakeNavigator.doSomething()
            fakeNavigator.navigateToActiveFeature()
            fakeNavigator.doSomething()
        } otherwise {
            fakeNavigator.navigateToDefaultScreen()
        }

        verify(inverse = true) { fakeNavigator.navigateToActiveFeature() }
        verify(inverse = true) { fakeNavigator.doSomething() }
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
