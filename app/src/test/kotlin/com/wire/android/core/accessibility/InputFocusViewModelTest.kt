package com.wire.android.core.accessibility

import com.wire.android.UnitTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`

class InputFocusViewModelTest : UnitTest() {

    @Mock
    private lateinit var accessibility: Accessibility

    private lateinit var inputFocusViewModel: InputFocusViewModel

    @Before
    fun setUp() {
        inputFocusViewModel = InputFocusViewModel(accessibility)
    }

    @Test
    fun `given talkBack is enabled, when canFocusWithKeyboard() is called, returns false`() {
        `when`(accessibility.isTalkbackEnabled()).thenReturn(true)

        assertThat(inputFocusViewModel.canFocusWithKeyboard()).isFalse()
    }

    @Test
    fun `given talkBack is disabled, when canFocusWithKeyboard() is called, returns true`() {
        `when`(accessibility.isTalkbackEnabled()).thenReturn(false)

        assertThat(inputFocusViewModel.canFocusWithKeyboard()).isTrue()
    }
}
