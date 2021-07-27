package com.wire.android.core.extension

import com.wire.android.UnitTest
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class OffsetDateTimeExtensionTest : UnitTest() {

    @MockK
    private lateinit var offsetDateTime1: OffsetDateTime

    @MockK
    private lateinit var offsetDateTime2: OffsetDateTime

    private val offsetDateTime = OffsetDateTime.of(2021,7,20,10,10,59,10, ZoneOffset.UTC)

    @Before
    fun setUp() {
        mockkStatic(OffsetDateTime::class)
    }

    @Test
    fun `given isSameYear is called, when input dates are different in year, then return false`() {
        every { offsetDateTime1.year } returns 2020
        every { offsetDateTime2.year } returns 2021

        val result = offsetDateTime1.isSameYear(offsetDateTime2)

        result shouldBeEqualTo false
    }

    @Test
    fun `given isSameYear is called, when input dates are identical in year, then return true`() {
        every { offsetDateTime1.year } returns 2021
        every { offsetDateTime2.year } returns 2021

        val result = offsetDateTime1.isSameYear(offsetDateTime2)

        result shouldBeEqualTo true
    }

    @Test
    fun `given isSameYear is called, when input dates are different in days and months, then return true`() {
        with(offsetDateTime1) {
            every { year } returns 2021
            every { dayOfMonth } returns 1
            every { monthValue } returns 10
        }
        with(offsetDateTime2) {
            every { year } returns 2021
            every { dayOfMonth } returns 5
            every { monthValue } returns 9
        }

        val result = offsetDateTime1.isSameYear(offsetDateTime2)

        result shouldBeEqualTo true
    }

    @Test
    fun `given isSameDay is called, when the input dates are identical, then return true`() {
        every { offsetDateTime1.toLocalDate() } returns LocalDate.of(2017, 7, 1)
        every { offsetDateTime2.toLocalDate() } returns LocalDate.of(2017, 7, 1)

        val result = offsetDateTime1.isSameDay(offsetDateTime2)
        result shouldBeEqualTo true
    }

    @Test
    fun `given isSameDay is called, when input dates are different only in days, then return false`() {
        every { offsetDateTime1.toLocalDate() } returns LocalDate.of(2021, 7, 2)
        every { offsetDateTime2.toLocalDate() } returns LocalDate.of(2021, 7, 1)

        val result = offsetDateTime1.isSameDay(offsetDateTime2)

        result shouldBeEqualTo false
    }

    @Test
    fun `given isSameDay is called, when input are different only in months, then return false`() {
        every { offsetDateTime1.toLocalDate() } returns LocalDate.of(2021, 8, 1)
        every { offsetDateTime2.toLocalDate() } returns LocalDate.of(2021, 7, 1)

        val result = offsetDateTime1.isSameDay(offsetDateTime2)
        result shouldBeEqualTo false
    }

    @Test
    fun `given isSameDay is called, when input dates are different only in years, then return false`() {
        every { offsetDateTime1.toLocalDate() } returns LocalDate.of(2022, 7, 1)
        every { offsetDateTime2.toLocalDate() } returns LocalDate.of(2021, 7, 1)

        val result = offsetDateTime1.isSameDay(offsetDateTime2)

        result shouldBeEqualTo false
    }

    @Test
    fun `given isMoreThanSixtyMinutes is called, when the amount of time between 2 dates is less than 60 minutes, then return true`() {
        val offsetDateTime1 = mockk<OffsetDateTime>()
        val offsetDateTime2 = mockk<OffsetDateTime>()
        every { offsetDateTime2.until(offsetDateTime1, ChronoUnit.MINUTES) } returns 100L
        every { offsetDateTime1.until(offsetDateTime2, ChronoUnit.MINUTES) } returns 50L

        val result = offsetDateTime2.isMoreThanSixtyMinutesApartOf(offsetDateTime1)

        result shouldBeEqualTo true
    }

    @Test
    fun `given isMoreThanSixtyMinutes is called, when the amount of time between 2 dates is greater than 60 minutes, then return false`(){
        every { offsetDateTime2.until(offsetDateTime1, ChronoUnit.MINUTES) } returns 10L
        every { offsetDateTime1.until(offsetDateTime2, ChronoUnit.MINUTES) } returns 500L

        val result = offsetDateTime2.isMoreThanSixtyMinutesApartOf(offsetDateTime1)

        result shouldBeEqualTo false
    }

    @Test
    fun `given isMoreThanSixtyMinutes is called, when the amount of time between 2 dates is equal to 60 minutes, then return true`() {
        every { offsetDateTime2.until(offsetDateTime1, ChronoUnit.MINUTES) } returns 160L
        every { offsetDateTime1.until(offsetDateTime2, ChronoUnit.MINUTES) } returns 100L

        val result = offsetDateTime2.isMoreThanSixtyMinutesApartOf(offsetDateTime1)

        result shouldBeEqualTo true
    }

    @Test
    fun `given isWithinTheLastMinutes is called, when the input date is within the last 2 minutes, then return true`() {
        val date = OffsetDateTime.of(2021,7,20,10,9,59,10, ZoneOffset.UTC)

        every { OffsetDateTime.now() } returns offsetDateTime

        val result = date.isWithinTheLastMinutes(2L)

        result shouldBeEqualTo true
    }

    @Test
    fun `given isWithinTheLastMinutes is called, when the input date is not within the last 2 minutes, then return false`() {
        val date = OffsetDateTime.of(2021,7,20,10,5,59,10, ZoneOffset.UTC)
        every { OffsetDateTime.now() } returns offsetDateTime

        val result = date.isWithinTheLastMinutes(2L)

        result shouldBeEqualTo false
    }

    @Test
    fun `given isWithinTheLastMinutes is called, when the input date is within the last 60 minutes, then return true`() {
        val date = OffsetDateTime.of(2021,7,20,9,11,59,10, ZoneOffset.UTC)
        every { OffsetDateTime.now() } returns offsetDateTime

        val result = date.isWithinTheLastMinutes(60L)

        result shouldBeEqualTo true
    }


    @Test
    fun `given isWithinTheLastMinutes is called, when the input date is not within the last 60 minutes, then return false`() {
        val date = OffsetDateTime.of(2021,7,20,9,10,59,10, ZoneOffset.UTC)
        every { OffsetDateTime.now() } returns offsetDateTime

        val result = date.isWithinTheLastMinutes(60L)

        result shouldBeEqualTo false
    }

    @Test
    fun `given timeFromOffsetDateTime is called, when offsetDateTime is valid, then return time`() {
        val result = offsetDateTime.timeFromOffsetDateTime()

        val expected = "10:10 AM"
        result shouldBeEqualTo expected
    }

    @Test
    fun `given dateWithoutYear is called, when offsetDateTime is valid, then return date without year`() {
        val result = offsetDateTime.dateWithoutYear()

        val expected = "Tue, Jul 20, 10:10"
        result shouldBeEqualTo expected
    }

    @Test
    fun `given dateWithYear is called, when offsetDateTime is valid, then return date with year`() {
        val result = offsetDateTime.dateWithYear()

        val expected = "Tue, Jul 20, 2021, 10:10"
        result shouldBeEqualTo expected
    }
}
