package com.wire.android.shared.conversation.content

import android.content.Context
import com.wire.android.R
import com.wire.android.UnitTest
import com.wire.android.core.extension.isWithinTheLastMinutes
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import java.time.LocalTime
import java.time.OffsetDateTime

class ConversationTimeGeneratorTest : UnitTest() {

    @MockK
    private lateinit var context: Context

    @MockK(relaxed = true)
    private lateinit var offsetDateTime : OffsetDateTime

    private lateinit var conversationTimeGenerator: ConversationTimeGenerator

    @Before
    fun setUp() {
        conversationTimeGenerator = ConversationTimeGenerator(context)
        mockkStatic("com.wire.android.core.extension.OffsetDateTimeKt")
    }

    @Test
    fun `given separatorTimeLabel is called, when input date is last two minutes from now, then return JUST NOW string` () {
        val justNow = "JUST NOW"
        every { offsetDateTime.isWithinTheLastMinutes(TEST_TWO_MINUTES) } returns true
        every { context.resources.getString(R.string.conversation_chat_just_now) } returns "JUST NOW"

        val result = conversationTimeGenerator.separatorTimeLabel(offsetDateTime)

        result shouldBeEqualTo justNow
    }

    @Test
    fun `given separatorTimeLabel is called, when input date is last 60 minutes from now, then return minutes ago label` () {
        val minutesAgo = "5 minutes Ago"
        val localTime = mockk<LocalTime>()
        val mock = spyk(conversationTimeGenerator, recordPrivateCalls = true)
        every { offsetDateTime.toLocalTime() } returns localTime
        every { offsetDateTime.isWithinTheLastMinutes(TEST_TWO_MINUTES) } returns false
        every { offsetDateTime.isWithinTheLastMinutes(TEST_Sixty_MINUTES) } returns true
        every { mock["minutesAgo"](localTime) } returns minutesAgo

        val result = mock.separatorTimeLabel(offsetDateTime)

        result shouldBeEqualTo minutesAgo
    }

    @Test
    fun `given separatorTimeLabel is called, when input date passed 60 minutes from now, then return full date` () {
        val fullDate = "Monday, 12 20, 10:10"
        val mock = spyk(conversationTimeGenerator, recordPrivateCalls = true)
        every { offsetDateTime.isWithinTheLastMinutes(TEST_TWO_MINUTES) } returns false
        every { offsetDateTime.isWithinTheLastMinutes(TEST_Sixty_MINUTES) } returns false
        every { mock["fullDateTime"](offsetDateTime) } returns fullDate

        val result = mock.separatorTimeLabel(offsetDateTime)

        result shouldBeEqualTo fullDate
    }

    companion object {
        private const val TEST_TWO_MINUTES = 2L
        private const val TEST_Sixty_MINUTES = 60L
    }
}
