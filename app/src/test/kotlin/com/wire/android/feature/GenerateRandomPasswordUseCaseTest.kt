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
package com.wire.android.feature

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GenerateRandomPasswordUseCaseTest {

    @Test
    fun `Given GenerateRandomPasswordUseCase, when generating a password, then it should meet the specified criteria`() {
        val generateRandomPasswordUseCase = GenerateRandomPasswordUseCase()

        repeat(100) { // Run the test 100 times
            val password = generateRandomPasswordUseCase.invoke()

            // Test criteria
            assertTrue(password.length >= GenerateRandomPasswordUseCase.MIN_LENGTH)
            assertTrue(password.length <= GenerateRandomPasswordUseCase.MAX_LENGTH)
            assertTrue(password.any { it in GenerateRandomPasswordUseCase.lowercase })
            assertTrue(password.any { it in GenerateRandomPasswordUseCase.uppercase })
            assertTrue(password.any { it in GenerateRandomPasswordUseCase.digits })
            assertTrue(password.any { it in GenerateRandomPasswordUseCase.specialChars })
        }
    }

    @Test
    fun `Given character sets, when accessing lowercase, then it should return the expected value`() {
        assertEquals("abcdefghijklmnopqrstuvwxyz".toList(), GenerateRandomPasswordUseCase.lowercase.sorted())
    }

    @Test
    fun `Given character sets, when accessing uppercase, then it should return the expected value`() {
        assertEquals("ABCDEFGHIJKLMNOPQRSTUVWXYZ".toList(), GenerateRandomPasswordUseCase.uppercase.sorted())
    }

    @Test
    fun `Given character sets, when accessing digits, then it should return the expected value`() {
        assertEquals("0123456789".toList(), GenerateRandomPasswordUseCase.digits.sorted())
    }

    @Test
    fun `Given character sets, when accessing specialChars, then it should return the expected value`() {
        assertEquals("!@#$%^&*()_+[]{}|;:,.<>?-".toList().sorted(), GenerateRandomPasswordUseCase.specialChars.sorted())
    }

    @Test
    fun `Given character sets, when accessing allCharacters, then it should return the expected value`() {
        val expectedAllCharacters =
            GenerateRandomPasswordUseCase.lowercase +
                    GenerateRandomPasswordUseCase.uppercase +
                    GenerateRandomPasswordUseCase.digits +
                    GenerateRandomPasswordUseCase.specialChars

        assertEquals(expectedAllCharacters.toList().sorted(), GenerateRandomPasswordUseCase.allCharacters.sorted())
    }
}
