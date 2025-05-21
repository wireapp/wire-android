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

import java.util.*
import kotlin.random.Random
import kotlin.*



data class UserInfo(
    val firstName: String,
    val lastName: String,
    val username: String,
    val password: String,
    val domain: String,
    val staticPassword: String
) {
    val email: String
        get() = "$username@$domain"
}


class UserClient {

    companion object {
        fun generateUniqueUserInfo(): UserInfo {
            val password = generateRandomPassword()
            val time = System.currentTimeMillis()
            val userName = "smoketester$time"
            val domain = "wire.engineering"
            val firstName = "Smoke"
            val lastName = "Tester$time"
            val staticPassword = "Aqa123456!"
            return UserInfo(firstName = firstName, lastName = lastName, username = userName, password = password, domain = domain, staticPassword = staticPassword)
        }

        fun generateRandomPassword(): String {
            val lowercase = "abcdefghijklmnopqrstuvwxyz"
            val uppercase = lowercase.uppercase(Locale.getDefault())
            val numbers = "0123456789"
            val specials = "!@#$%^&*()"


            val passwordBuilder = java.lang.StringBuilder()

            repeat(5) {

                passwordBuilder.append(randomCharacterFrom(lowercase))
            }

            passwordBuilder.append(randomCharacterFrom(uppercase))
            passwordBuilder.append(randomCharacterFrom(specials))
            passwordBuilder.append(randomCharacterFrom(numbers))

            return passwordBuilder.toString()

        }

        private fun randomCharacterFrom(characters: String): Char {
            return characters[Random.nextInt(characters.length)]
        }
    }
}

