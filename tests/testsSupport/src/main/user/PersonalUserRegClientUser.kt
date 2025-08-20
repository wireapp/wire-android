/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package user

import java.security.SecureRandom

data class UserInfo(
    val name: String,
    val lastName: String,
    val username: String,
    val password: String,
    val domain: String,
) {
    val email: String
        get() = "$username@$domain"
}

object UserClient {

    val lowercase: List<Char> = ('a'..'z').shuffled()
    val uppercase: List<Char> = ('A'..'Z').shuffled()
    val digits: List<Char> = ('0'..'9').shuffled()
    val specialChars: List<Char> = "!@#$%^&*()_+[]{}|;:,.<>?-".toList().shuffled()
    val allCharacters: List<Char> = (lowercase + uppercase + digits + specialChars).shuffled()
    const val MIN_LENGTH = 15
    const val MAX_LENGTH = 20
    const val FIXED_CHAR_COUNT = 4

    fun generateUniqueUserInfo(): UserInfo {
        val password = generateRandomPassword()
        val time = System.currentTimeMillis()
        val userName = "smoke$time"
        val domain = "wire.engineering"
        val name = "Smoke$time"
        val lastName = "Tester$time"
        return UserInfo(
            name = name,
            lastName = lastName,
            username = userName,
            password = password,
            domain = domain,
        )
    }

    @Suppress("MagicNumber")
    fun generateRandomPassword(): String {
        val secureRandom = SecureRandom()
        val passwordLength = secureRandom.nextInt(MAX_LENGTH - MIN_LENGTH + 1) + MIN_LENGTH
        return buildList<Char> {
            add(lowercase[secureRandom.nextInt(lowercase.size)])
            add(uppercase[secureRandom.nextInt(uppercase.size)])
            add(digits[secureRandom.nextInt(digits.size)])
            add(specialChars[secureRandom.nextInt(specialChars.size)])
            repeat(passwordLength - FIXED_CHAR_COUNT) {
                add(allCharacters[secureRandom.nextInt(allCharacters.size)])
            }
        }.shuffled(secureRandom).joinToString("")
    }

    object RandomStringGenerator { // Character pools
        private const val NUMERIC = "0123456789"
        private const val ALPHABETIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        private const val ALPHANUMERIC = ALPHABETIC + NUMERIC
        private const val SPECIAL_CHARS = "!@#$%^&*()-_=+[]{}|;:',.<>/?`~"
        private const val ALL_CHARS = ALPHANUMERIC + SPECIAL_CHARS

        /**
         * Generates a random numeric string
         * @param length Desired length of the string
         * @return String containing random digits
         */
        fun randomNumeric(length: Int): String {
            require(length >= 0) { "Length must be non-negative" }
            return List(length) { NUMERIC.random() }.joinToString("")
        }

        /**
         * Generates a random alphabetic string (upper and lower case)
         * @param length Desired length of the string
         * @return String containing random letters
         */
        fun randomAlphabetic(length: Int): String {
            require(length >= 0) { "Length must be non-negative" }
            return List(length) { ALPHABETIC.random() }.joinToString("")
        }

        /**
         * Generates a random alphanumeric string
         * @param length Desired length of the string
         * @return String containing random letters and digits
         */
        fun randomAlphanumeric(length: Int): String {
            require(length >= 0) { "Length must be non-negative" }
            return List(length) { ALPHANUMERIC.random() }.joinToString("")
        }

        /**
         * Generates a random string with special characters
         * @param length Desired length of the string
         * @return String containing random letters, digits, and special characters
         */
        fun randomWithSpecialChars(length: Int): String {
            require(length >= 0) { "Length must be non-negative" }
            return List(length) { ALL_CHARS.random() }.joinToString("")
        }

        /**
         * Generates a random string from a custom character pool
         * @param length Desired length of the string
         * @param charPool Custom set of characters to choose from
         * @return String containing random characters from the custom pool
         */
        fun randomCustom(length: Int, charPool: String): String {
            require(length >= 0) { "Length must be non-negative" }
            require(charPool.isNotEmpty()) { "Character pool must not be empty" }
            return List(length) { charPool.random() }.joinToString("")
        }
    }
}
