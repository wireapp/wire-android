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

package com.wire.android.util.extension

import com.wire.kalium.logic.data.conversation.ClientId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ClientIdTest {

    @Test
    fun givenClientIdHasOneCharacter_whenFormattingAsString_thenItHasTheExpectedFormat() {
        val clientId = OneCharacterIds.first
        val validatedClientId = clientId.formatAsString()

        assertEquals(EXPECTED_FORMATTED_CLIENT_ID_STRING_LENGTH, validatedClientId.replace(" ", "").length)
        assertEquals(OneCharacterIds.second, validatedClientId)
    }

    @Test
    fun givenClientIdHasTwoCharacter_whenFormattingAsString_thenItHasTheExpectedFormat() {
        val clientId = TwoCharacterIds.first
        val validatedClientId = clientId.formatAsString()

        assertEquals(EXPECTED_FORMATTED_CLIENT_ID_STRING_LENGTH, validatedClientId.replace(" ", "").length)
        assertEquals(TwoCharacterIds.second, validatedClientId)
    }

    @Test
    fun givenClientIdHasThreeCharacter_whenFormattingAsString_thenItHasTheExpectedFormat() {
        val clientId = ThreeCharacterIds.first
        val validatedClientId = clientId.formatAsString()

        assertEquals(EXPECTED_FORMATTED_CLIENT_ID_STRING_LENGTH, validatedClientId.replace(" ", "").length)
        assertEquals(ThreeCharacterIds.second, validatedClientId)
    }

    @Test
    fun givenClientIdHasFourCharacter_whenFormattingAsString_thenItHasTheExpectedFormat() {
        val clientId = FourCharacterIds.first
        val validatedClientId = clientId.formatAsString()

        assertEquals(EXPECTED_FORMATTED_CLIENT_ID_STRING_LENGTH, validatedClientId.replace(" ", "").length)
        assertEquals(FourCharacterIds.second, validatedClientId)
    }

    @Test
    fun givenClientIdHasFiveCharacter_whenFormattingAsString_thenItHasTheExpectedFormat() {
        val clientId = FiveCharacterIds.first
        val validatedClientId = clientId.formatAsString()

        assertEquals(EXPECTED_FORMATTED_CLIENT_ID_STRING_LENGTH, validatedClientId.replace(" ", "").length)
        assertEquals(FiveCharacterIds.second, validatedClientId)
    }

    @Test
    fun givenClientIdHasSixCharacter_whenFormattingAsString_thenItHasTheExpectedFormat() {
        val clientId = SixCharacterIds.first
        val validatedClientId = clientId.formatAsString()

        assertEquals(EXPECTED_FORMATTED_CLIENT_ID_STRING_LENGTH, validatedClientId.replace(" ", "").length)
        assertEquals(SixCharacterIds.second, validatedClientId)
    }

    @Test
    fun givenClientIdHasSevenCharacter_whenFormattingAsString_thenItHasTheExpectedFormat() {
        val clientId = SevenCharacterIds.first
        val validatedClientId = clientId.formatAsString()

        assertEquals(EXPECTED_FORMATTED_CLIENT_ID_STRING_LENGTH, validatedClientId.replace(" ", "").length)
        assertEquals(SevenCharacterIds.second, validatedClientId)
    }

    @Test
    fun givenClientIdHasEightCharacter_whenFormattingAsString_thenItHasTheExpectedFormat() {
        val clientId = EightCharacterIds.first
        val validatedClientId = clientId.formatAsString()

        assertEquals(EXPECTED_FORMATTED_CLIENT_ID_STRING_LENGTH, validatedClientId.replace(" ", "").length)
        assertEquals(EightCharacterIds.second, validatedClientId)
    }

    @Test
    fun givenClientIdHasEightNineCharacter_whenFormattingAsString_thenItHasTheExpectedFormat() {
        val clientId = NineCharacterIds.first
        val validatedClientId = clientId.formatAsString()

        assertEquals(EXPECTED_FORMATTED_CLIENT_ID_STRING_LENGTH, validatedClientId.replace(" ", "").length)
        assertEquals(NineCharacterIds.second, validatedClientId)
    }

    @Test
    fun givenClientIdHasTenCharacter_whenFormattingAsString_thenItHasTheExpectedFormat() {
        val clientId = TenCharacterIds.first
        val validatedClientId = clientId.formatAsString()

        assertEquals(EXPECTED_FORMATTED_CLIENT_ID_STRING_LENGTH, validatedClientId.replace(" ", "").length)
        assertEquals(TenCharacterIds.second, validatedClientId)
    }

    @Test
    fun givenClientIdHasElevenCharacter_whenFormattingAsString_thenItHasTheExpectedFormat() {
        val clientId = ElevenCharacterIds.first
        val validatedClientId = clientId.formatAsString()

        assertEquals(EXPECTED_FORMATTED_CLIENT_ID_STRING_LENGTH, validatedClientId.replace(" ", "").length)
        assertEquals(ElevenCharacterIds.second, validatedClientId)
    }

    @Test
    fun givenClientIdHasTwelveCharacter_whenFormattingAsString_thenItHasTheExpectedFormat() {
        val clientId = TwelveCharacterIds.first
        val validatedClientId = clientId.formatAsString()

        assertEquals(EXPECTED_FORMATTED_CLIENT_ID_STRING_LENGTH, validatedClientId.replace(" ", "").length)
        assertEquals(TwelveCharacterIds.second, validatedClientId)
    }

    @Test
    fun givenClientIdHasThirteenCharacter_whenFormattingAsString_thenItHasTheExpectedFormat() {
        val clientId = ThirteenCharacterIds.first
        val validatedClientId = clientId.formatAsString()

        assertEquals(EXPECTED_FORMATTED_CLIENT_ID_STRING_LENGTH, validatedClientId.replace(" ", "").length)
        assertEquals(ThirteenCharacterIds.second, validatedClientId)
    }

    @Test
    fun givenClientIdHasFourteenCharacter_whenFormattingAsString_thenItHasTheExpectedFormat() {
        val clientId = FourteenCharacterIds.first
        val validatedClientId = clientId.formatAsString()

        assertEquals(EXPECTED_FORMATTED_CLIENT_ID_STRING_LENGTH, validatedClientId.replace(" ", "").length)
        assertEquals(FourteenCharacterIds.second, validatedClientId)
    }

    @Test
    fun givenClientIdHasFifteenCharacter_whenFormattingAsString_thenItHasTheExpectedFormat() {
        val clientId = FifteenCharacterIds.first
        val validatedClientId = clientId.formatAsString()

        assertEquals(EXPECTED_FORMATTED_CLIENT_ID_STRING_LENGTH, validatedClientId.replace(" ", "").length)
        assertEquals(FifteenCharacterIds.second, validatedClientId)
    }

    @Test
    fun givenClientIdHasSixteenCharacter_whenFormattingAsString_thenItHasTheExpectedFormat() {
        val clientId = SixteenCharacterIds.first
        val validatedClientId = clientId.formatAsString()

        assertEquals(EXPECTED_FORMATTED_CLIENT_ID_STRING_LENGTH, validatedClientId.replace(" ", "").length)
        assertEquals(SixteenCharacterIds.second, validatedClientId)
    }

    private companion object TestData {
        const val EXPECTED_FORMATTED_CLIENT_ID_STRING_LENGTH = 16

        val OneCharacterIds = ClientId("2") to "00 00 00 00 00 00 00 02"
        val TwoCharacterIds = ClientId("72") to "00 00 00 00 00 00 00 72"
        val ThreeCharacterIds = ClientId("F72") to "00 00 00 00 00 00 0F 72"
        val FourCharacterIds = ClientId("BF72") to "00 00 00 00 00 00 BF 72"
        val FiveCharacterIds = ClientId("8BF72") to "00 00 00 00 00 08 BF 72"
        val SixCharacterIds = ClientId("E8BF72") to "00 00 00 00 00 E8 BF 72"
        val SevenCharacterIds = ClientId("AE8BF72") to "00 00 00 00 0A E8 BF 72"
        val EightCharacterIds = ClientId("EAE8BF72") to "00 00 00 00 EA E8 BF 72"
        val NineCharacterIds = ClientId("DEAE8BF72") to "00 00 00 0D EA E8 BF 72"
        val TenCharacterIds = ClientId("2DEAE8BF72") to "00 00 00 2D EA E8 BF 72"
        val ElevenCharacterIds = ClientId("62DEAE8BF72") to "00 00 06 2D EA E8 BF 72"
        val TwelveCharacterIds = ClientId("262DEAE8BF72") to "00 00 26 2D EA E8 BF 72"
        val ThirteenCharacterIds = ClientId("D262DEAE8BF72") to "00 0D 26 2D EA E8 BF 72"
        val FourteenCharacterIds = ClientId("1D262DEAE8BF72") to "00 1D 26 2D EA E8 BF 72"
        val FifteenCharacterIds = ClientId("11D262DEAE8BF72") to "01 1D 26 2D EA E8 BF 72"
        val SixteenCharacterIds = ClientId("011D262DEAE8BF72") to "01 1D 26 2D EA E8 BF 72"
    }
}
