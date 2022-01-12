package com.wire.android.core.accessibility

import com.wire.android.UnitTest
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.amshove.kluent.shouldBe
import org.junit.Before
import org.junit.Test

class InputFocusViewModelTest : UnitTest() {

    @MockK
    private lateinit var accessibility: Accessibility

    private lateinit var inputFocusViewModel: InputFocusViewModel

    @Before
    fun setUp() {
        inputFocusViewModel = InputFocusViewModel(accessibility)
    }

    @Test
    fun `given talkBack is enabled, when canFocusWithKeyboard() is called, returns false`() {
        every { accessibility.isTalkbackEnabled() } returns true

        inputFocusViewModel.canFocusWithKeyboard() shouldBe false
    }

    @Test
    fun `given talkBack is disabled, when canFocusWithKeyboard() is called, returns true`() {
        every { accessibility.isTalkbackEnabled() } returns false

        inputFocusViewModel.canFocusWithKeyboard() shouldBe true
    }
}
