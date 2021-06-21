package com.wire.android.feature.conversation.content.mapper

import com.wire.android.UnitTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import java.time.OffsetDateTime

class MessageTimeMapperTest : UnitTest() {

    private val subject = MessageTimeMapper()

    @Test
    fun `given fromOffsetDateTimeToString is called, when calling fromStringToOffsetDateTime, the original offsetTime should be returned`() {
        val currentTime = OffsetDateTime.now()

        val stringRepresentation = subject.fromOffsetDateTimeToString(currentTime)

        subject.fromStringToOffsetDateTime(stringRepresentation) shouldBeEqualTo currentTime
    }

    @Test
    fun `given fromStringToOffsetDateTime is called, when calling fromOffsetDateTimeToString, the original string should be returned`() {
        val timeString = "2019-12-12T21:21:00+03:00"

        val offsetTime = subject.fromStringToOffsetDateTime(timeString)

        subject.fromOffsetDateTimeToString(offsetTime) shouldBeEqualTo timeString
    }
}
