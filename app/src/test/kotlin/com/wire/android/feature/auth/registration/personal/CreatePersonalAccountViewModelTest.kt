package com.wire.android.feature.auth.registration.personal

import com.wire.android.UnitTest
import com.wire.android.core.accessibility.Accessibility
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`

@ExperimentalCoroutinesApi
class CreatePersonalAccountViewModelTest : UnitTest() {

    private lateinit var createPersonalAccountViewModel: CreatePersonalAccountViewModel

    @Mock
    private lateinit var accessibility: Accessibility

    @Before
    fun setup() {
        createPersonalAccountViewModel = CreatePersonalAccountViewModel(accessibility)
    }

    @Test
    fun `given shouldShowKeyboard is queried, when talkback is not enabled, then return true `() {
        runBlocking {
            `when`(accessibility.isTalkbackEnabled()).thenReturn(false)

            assertThat(createPersonalAccountViewModel.shouldShowKeyboard()).isEqualTo(true)
        }
    }

    @Test
    fun `given shouldShowKeyboard is queried, when talkback is enabled, then return false `() {
        runBlocking {
            `when`(accessibility.isTalkbackEnabled()).thenReturn(true)
            assertThat(createPersonalAccountViewModel.shouldShowKeyboard()).isEqualTo(false)
        }
    }
}
