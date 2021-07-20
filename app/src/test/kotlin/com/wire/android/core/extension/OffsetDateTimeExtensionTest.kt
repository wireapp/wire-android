package com.wire.android.core.extension

import com.wire.android.UnitTest
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class OffsetDateTimeExtensionTest : UnitTest() {

    @MockK
    private lateinit var offsetDateTime1: OffsetDateTime

    @MockK
    private lateinit var offsetDateTime2: OffsetDateTime

    private val offsetDateTime = OffsetDateTime.of(2021,7,20,10,10,59,10, ZoneOffset.UTC)

    private val nowDate = LocalDateTime.of(2021,7,19,7,10,0)

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
    fun `given isLastSixtyMinutes is called, when the amount of time between the two dates is fewer than 60 minutes, then return true`() {
        val offsetDateTime1 = mockk<OffsetDateTime>()
        val offsetDateTime2 = mockk<OffsetDateTime>()
        every { offsetDateTime2.until(offsetDateTime1, ChronoUnit.MINUTES) } returns 100L
        every { offsetDateTime1.until(offsetDateTime2, ChronoUnit.MINUTES) } returns 50L

        val result = offsetDateTime2.isLastSixtyMinutes(offsetDateTime1)

        result shouldBeEqualTo true
    }

    @Test
    fun `given isLastSixtyMinutes is called, when the amount of time between the two dates is greater than 60 minutes, then return false`(){
        every { offsetDateTime2.until(offsetDateTime1, ChronoUnit.MINUTES) } returns 10L
        every { offsetDateTime1.until(offsetDateTime2, ChronoUnit.MINUTES) } returns 500L

        val result = offsetDateTime2.isLastSixtyMinutes(offsetDateTime1)

        result shouldBeEqualTo false
    }

    @Test
    fun `given isLastSixtyMinutes is called, when the amount of time between the two dates is equal to to 60 minutes, then return true`() {
        every { offsetDateTime2.until(offsetDateTime1, ChronoUnit.MINUTES) } returns 160L
        every { offsetDateTime1.until(offsetDateTime2, ChronoUnit.MINUTES) } returns 100L

        val result = offsetDateTime2.isLastSixtyMinutes(offsetDateTime1)

        result shouldBeEqualTo true
    }

    @Test
    fun `given isLastXMinutesFromNow is called, when the input date is 2 minutes ago from now, then return true`() {
        val oldDate = LocalDateTime.of(2021,7,19,7,9,0)
        mockkStatic(LocalDateTime::class)
        val offsetDateTime = mockk<OffsetDateTime>()
        every { LocalDateTime.ofInstant(offsetDateTime.toInstant(), ZoneId.systemDefault()) } returns oldDate
        every { LocalDateTime.now() } returns nowDate

        val result = offsetDateTime.isLastXMinutesFromNow(2L)

        result shouldBeEqualTo true
    }

    @Test
    fun `given isLastXMinutesFromNow is called, when the input date is not 2 minutes ago from now, then return false`() {
        val oldDate = LocalDateTime.of(2021,7,19,7,5,0)
        mockkStatic(LocalDateTime::class)
        val offsetDateTime = mockk<OffsetDateTime>()
        every { LocalDateTime.ofInstant(offsetDateTime.toInstant(), ZoneId.systemDefault()) } returns oldDate
        every { LocalDateTime.now() } returns nowDate

        val result = offsetDateTime.isLastXMinutesFromNow(2L)

        result shouldBeEqualTo false
    }

    @Test
    fun `given isLastXMinutesFromNow is called, when the input date is 60 minutes ago from now, then return true`() {
        val oldDate = LocalDateTime.of(2021,7,19,7,50,0)
        mockkStatic(LocalDateTime::class)
        val offsetDateTime = mockk<OffsetDateTime>()
        every { LocalDateTime.ofInstant(offsetDateTime.toInstant(), ZoneId.systemDefault()) } returns oldDate
        every { LocalDateTime.now() } returns nowDate

        val result = offsetDateTime.isLastXMinutesFromNow(60L)

        result shouldBeEqualTo true
    }


    @Test
    fun `given isLastXMinutesFromNow is called, when the input date is more than 60 minutes ago from now, then return false`() {
        val oldDate = LocalDateTime.of(2021,7,19,6,5,0)
        mockkStatic(LocalDateTime::class)
        val offsetDateTime = mockk<OffsetDateTime>()
        every { LocalDateTime.ofInstant(offsetDateTime.toInstant(), ZoneId.systemDefault()) } returns oldDate
        every { LocalDateTime.now() } returns nowDate

        val result = offsetDateTime.isLastXMinutesFromNow(60L)

        result shouldBeEqualTo false
    }

    @Test
    fun `given timeFromOffsetDateTime is called, when offsetDateTime is valid, then return time`() {
        val result = offsetDateTime.timeFromOffsetDateTime()

        val expected = "10:10"
        result shouldBeEqualTo expected
    }

    @Test
    fun `given dateWithoutYear is called, when offsetDateTime is valid, then return date without year`() {
        val result = offsetDateTime.dateWithoutYear()

        val expected = "mar., juil. 20, 10:10"
        result shouldBeEqualTo expected
    }

    @Test
    fun `given dateWithYear is called, when offsetDateTime is valid, then return date with year`() {
        val result = offsetDateTime.dateWithYear()

        val expected = "mar., juil. 20, 2021, 10:10"
        result shouldBeEqualTo expected
    }
}
