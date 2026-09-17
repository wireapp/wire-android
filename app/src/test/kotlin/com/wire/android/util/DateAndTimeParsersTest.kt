/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.android.util

import kotlinx.datetime.Instant
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class DateAndTimeParsersTest {

    @Test
    fun givenInstant_whenDeviceDateTimeIsFormatted_thenReturnLongDateAndShortTime() {
        val result = TEST_INSTANT.deviceDateTimeFormat()

        assertEquals("March 24, 2022, 6:02 PM", result.normalizeSpaces())
    }

    @Test
    fun givenInstant_whenMediumDateTimeIsFormatted_thenReturnMediumDateAndTime() {
        val result = TEST_INSTANT.formatMediumDateTime()

        assertEquals("Mar 24, 2022, 6:02:30 PM", result.normalizeSpaces())
    }

    @Test
    fun givenInstant_whenFullDateShortTimeIsFormatted_thenReturnFullDateAndShortTime() {
        val result = TEST_INSTANT.formatFullDateShortTime()

        assertEquals("Thursday, March 24, 2022, 6:02 PM", result.normalizeSpaces())
    }

    @Test
    fun givenInstant_whenMessageTimeIsFormatted_thenReturnShortTime() {
        val result = TEST_INSTANT.uiMessageDateTime()

        assertEquals("6:02 PM", result.normalizeSpaces())
    }

    @Test
    fun givenInstant_whenFileDateTimeIsFormatted_thenReturnFileSafeTimestamp() {
        assertEquals("2022-03-24-06-02-30", TEST_INSTANT.fileDateTime())
    }

    @Test
    fun givenInstant_whenReadReceiptDateTimeIsFormatted_thenReturnReadReceiptTimestamp() {
        val result = TEST_INSTANT.uiReadReceiptDateTime()

        assertEquals("Mar 24 2022,  06:02 PM", result.normalizeSpaces())
    }

    @ParameterizedTest
    @CsvSource(
        "UTC|March 24, 2022, 6:02 PM",
        "America/New_York|March 24, 2022, 2:02 PM",
        "Asia/Tokyo|March 25, 2022, 3:02 AM",
        delimiter = '|'
    )
    fun givenExplicitZoneId_whenDeviceDateTimeIsFormatted_thenReturnValueInRequestedZone(zone: String, expected: String) {
        val zoneId = ZoneId.of(zone)

        val result = TEST_INSTANT.deviceDateTimeFormat(zoneId = zoneId)

        assertEquals(expected, result.normalizeSpaces())
    }

    @ParameterizedTest
    @CsvSource(
        "UTC|Mar 24, 2022, 6:02:30 PM",
        "America/New_York|Mar 24, 2022, 2:02:30 PM",
        "Asia/Tokyo|Mar 25, 2022, 3:02:30 AM",
        delimiter = '|'
    )
    fun givenExplicitZoneId_whenMediumDateTimeIsFormatted_thenReturnValueInRequestedZone(zone: String, expected: String) {
        val zoneId = ZoneId.of(zone)

        val result = TEST_INSTANT.formatMediumDateTime(zoneId = zoneId)

        assertEquals(expected, result.normalizeSpaces())
    }

    @ParameterizedTest
    @CsvSource(
        "UTC|Thursday, March 24, 2022, 6:02 PM",
        "America/New_York|Thursday, March 24, 2022, 2:02 PM",
        "Asia/Tokyo|Friday, March 25, 2022, 3:02 AM",
        delimiter = '|'
    )
    fun givenExplicitZoneId_whenFullDateShortTimeIsFormatted_thenReturnValueInRequestedZone(zone: String, expected: String) {
        val zoneId = ZoneId.of(zone)

        val result = TEST_INSTANT.formatFullDateShortTime(zoneId = zoneId)

        assertEquals(expected, result.normalizeSpaces())
    }

    @ParameterizedTest
    @CsvSource(
        "UTC|6:02 PM",
        "America/New_York|2:02 PM",
        "Asia/Tokyo|3:02 AM",
        delimiter = '|'
    )
    fun givenExplicitZoneId_whenMessageTimeIsFormatted_thenReturnValueInRequestedZone(zone: String, expected: String) {
        val zoneId = ZoneId.of(zone)

        val result = TEST_INSTANT.uiMessageDateTime(zoneId = zoneId)

        assertEquals(expected, result.normalizeSpaces())
    }

    @ParameterizedTest
    @CsvSource(
        "UTC|2022-03-24-06-02-30",
        "America/New_York|2022-03-24-02-02-30",
        "Asia/Tokyo|2022-03-25-03-02-30",
        delimiter = '|'
    )
    fun givenExplicitZoneId_whenFileDateTimeIsFormatted_thenReturnValueInRequestedZone(zone: String, expected: String) {
        val zoneId = ZoneId.of(zone)

        val result = TEST_INSTANT.fileDateTime(zoneId = zoneId)

        assertEquals(expected, result.normalizeSpaces())
    }

    @ParameterizedTest
    @CsvSource(
        "UTC|Mar 24 2022,  06:02 PM",
        "America/New_York|Mar 24 2022,  02:02 PM",
        "Asia/Tokyo|Mar 25 2022,  03:02 AM",
        delimiter = '|'
    )
    fun givenExplicitZoneId_whenReadReceiptDateTimeIsFormatted_thenReturnValueInRequestedZone(zone: String, expected: String) {
        val zoneId = ZoneId.of(zone)

        val result = TEST_INSTANT.uiReadReceiptDateTime(zoneId = zoneId)

        assertEquals(expected, result.normalizeSpaces())
    }

    @ParameterizedTest
    @CsvSource(
        "UTC|Mar 24, 2022",
        "America/New_York|Mar 24, 2022",
        "Asia/Tokyo|Mar 25, 2022",
        delimiter = '|'
    )
    fun givenExplicitZoneId_whenMediumOnlyDateTimeIsFormatted_thenReturnValueInRequestedZone(zone: String, expected: String) {
        val zoneId = ZoneId.of(zone)

        val result = Date(TEST_INSTANT.toEpochMilliseconds()).toMediumOnlyDateTime(zoneId = zoneId)

        assertEquals(expected, result.normalizeSpaces())
    }

    @ParameterizedTest
    @CsvSource(
        "UTC|Mar 24, 6:02 PM",
        "America/New_York|Mar 24, 2:02 PM",
        "Asia/Tokyo|Mar 25, 3:02 AM",
        delimiter = '|'
    )
    fun givenExplicitZoneId_whenCellFileDateTimeIsFormatted_thenReturnValueInRequestedZone(zone: String, expected: String) {
        val zoneId = ZoneId.of(zone)

        val result = TEST_INSTANT.cellFileDateTime(zoneId = zoneId)

        assertEquals(expected, result.normalizeSpaces())
    }

    @ParameterizedTest
    @CsvSource(
        "UTC|6:02 PM",
        "America/New_York|2:02 PM",
        "Asia/Tokyo|3:02 AM",
        delimiter = '|'
    )
    fun givenExplicitZoneId_whenCellFileTimeIsFormatted_thenReturnValueInRequestedZone(zone: String, expected: String) {
        val zoneId = ZoneId.of(zone)

        val result = TEST_INSTANT.cellFileTime(zoneId = zoneId)

        assertEquals(expected, result.normalizeSpaces())
    }

    @ParameterizedTest
    @CsvSource(
        "UTC|Thursday, March 24",
        "America/New_York|Thursday, March 24",
        "Asia/Tokyo|Friday, March 25",
        delimiter = '|'
    )
    fun givenExplicitZoneId_whenLinkExpirationDateIsFormatted_thenReturnValueInRequestedZone(zone: String, expected: String) {
        val zoneId = ZoneId.of(zone)

        val result = TEST_INSTANT.toEpochMilliseconds().uiLinkExpirationDate(zoneId = zoneId)

        assertEquals(expected, result.normalizeSpaces())
    }

    @ParameterizedTest
    @CsvSource(
        "UTC|6:02 PM",
        "America/New_York|2:02 PM",
        "Asia/Tokyo|3:02 AM",
        delimiter = '|'
    )
    fun givenExplicitZoneId_whenLinkExpirationTimeIsFormatted_thenReturnValueInRequestedZone(zone: String, expected: String) {
        val zoneId = ZoneId.of(zone)

        val result = TEST_INSTANT.toEpochMilliseconds().uiLinkExpirationTime(zoneId = zoneId)

        assertEquals(expected, result.normalizeSpaces())
    }

    private fun String.normalizeSpaces(): String = replace('\u202f', ' ')

    companion object {
        private val TEST_INSTANT = Instant.parse("2022-03-24T18:02:30.360Z")
        private var systemDefaultLocale: Locale? = null
        private var systemDefaultTimeZone: TimeZone? = null

        @JvmStatic
        @BeforeAll
        fun setup() {
            systemDefaultTimeZone = TimeZone.getDefault()
            systemDefaultLocale = Locale.getDefault()
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            Locale.setDefault(Locale.US)
        }

        @JvmStatic
        @AfterAll
        fun tearDown() {
            TimeZone.setDefault(systemDefaultTimeZone!!)
            Locale.setDefault(systemDefaultLocale!!)
        }
    }
}
