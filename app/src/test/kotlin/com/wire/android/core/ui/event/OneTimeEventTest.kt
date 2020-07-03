package com.wire.android.core.ui.event

import com.wire.android.UnitTest
import org.assertj.core.api.Assertions.assertThat

import org.junit.Test

class OneTimeEventTest : UnitTest() {

    private lateinit var oneTimeEvent: OneTimeEvent<Int>

    @Test
    fun `given that content has not been handled, when getContentIfNotHandled() is called, returns value`() {
        oneTimeEvent = OneTimeEvent(CONTENT)

        assertThat(oneTimeEvent.getContentIfNotHandled()).isEqualTo(CONTENT)
    }

    @Test
    fun `given that content is already handled, when getContentIfNotHandled() is called, returns null`() {
        oneTimeEvent = OneTimeEvent(CONTENT)

        oneTimeEvent.getContentIfNotHandled()

        assertThat(oneTimeEvent.getContentIfNotHandled()).isNull()
    }

    @Test
    fun `given that content has not been handled, when peekContent() is called, returns value`() {
        oneTimeEvent = OneTimeEvent(CONTENT)

        assertThat(oneTimeEvent.peekContent()).isEqualTo(CONTENT)
    }

    @Test
    fun `given that content is already handled, when peekContent() is called, returns value`() {
        oneTimeEvent = OneTimeEvent(CONTENT)

        oneTimeEvent.getContentIfNotHandled()

        assertThat(oneTimeEvent.peekContent()).isEqualTo(CONTENT)
    }

    companion object {
        private const val CONTENT = 3
    }
}