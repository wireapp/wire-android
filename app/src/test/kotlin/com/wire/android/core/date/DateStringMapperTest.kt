package com.wire.android.core.date

import com.wire.android.UnitTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import java.time.OffsetDateTime

class DateStringMapperTest : UnitTest() {

    private val subject = DateStringMapper()

    @Test
    fun `given fromOffsetDateTimeToString is called, when calling fromStringToOffsetDateTime, the original time should be returned`() {
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
