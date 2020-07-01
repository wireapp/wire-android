package com.wire.android.feature.auth.registration.personal

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.wire.android.UnitTest
import com.wire.android.core.accessibility.AccessibilityManagerWrapper
import com.wire.android.framework.coroutines.CoroutinesTestRule
import com.wire.android.framework.livedata.awaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`

@ExperimentalCoroutinesApi
class CreatePersonalAccountViewModelTest : UnitTest() {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private lateinit var createPersonalAccountViewModel: CreatePersonalAccountViewModel

    @Mock
    private lateinit var accessibilityManagerWrapper: AccessibilityManagerWrapper

    @Before
    fun setup() {
        `when`(accessibilityManagerWrapper.isTalkbackEnabled()).thenReturn(false)
        createPersonalAccountViewModel = CreatePersonalAccountViewModel(accessibilityManagerWrapper)
    }

    @Test
    fun `given view model is initialised, when talkback is not enabled, then update keyboard live data`() {
        runBlockingTest {
            assertThat(createPersonalAccountViewModel.keyboardDisplayLiveData.awaitValue()).isEqualTo(Unit)
        }
    }
}