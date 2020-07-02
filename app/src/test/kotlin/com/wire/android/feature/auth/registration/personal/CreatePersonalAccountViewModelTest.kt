package com.wire.android.feature.auth.registration.personal

import com.wire.android.UnitTest
import com.wire.android.core.accessibility.AccessibilityManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`

@ExperimentalCoroutinesApi
class CreatePersonalAccountViewModelTest : UnitTest() {

    private lateinit var createPersonalAccountViewModel: CreatePersonalAccountViewModel

    @Mock
    private lateinit var accessibilityManagerWrapper: AccessibilityManager

    @Before
    fun setup() {
        createPersonalAccountViewModel = CreatePersonalAccountViewModel(accessibilityManagerWrapper)
    }

    @Test
    fun `given shouldShowKeyboard is queried, when talkback is not enabled, then return true `() {
        runBlockingTest {
            `when`(accessibilityManagerWrapper.isTalkbackEnabled()).thenReturn(false)

            assertThat(createPersonalAccountViewModel.shouldShowKeyboard()).isEqualTo(true)
        }
    }

    @Test
    fun `given shouldShowKeyboard is queried, when talkback is enabled, then return false `() {
        runBlockingTest {
            `when`(accessibilityManagerWrapper.isTalkbackEnabled()).thenReturn(true)
            assertThat(createPersonalAccountViewModel.shouldShowKeyboard()).isEqualTo(false)
        }
    }
}
