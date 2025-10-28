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

import androidx.annotation.VisibleForTesting
import dagger.hilt.android.scopes.ViewModelScoped
import java.security.SecureRandom
import javax.inject.Inject

@ViewModelScoped
class GenerateRandomPasswordUseCase @Inject constructor() {

    operator fun invoke(): String {

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

    @VisibleForTesting
    companion object {
        val lowercase: List<Char> = ('a'..'z').toList()
        val uppercase: List<Char> = ('A'..'Z').toList()
        val digits: List<Char> = ('0'..'9').toList()
        val specialChars: List<Char> = "!@#$%^&*()_+[]{}|;:,.<>?-".toList()

        val allCharacters: List<Char> = lowercase + uppercase + digits + specialChars

        const val MIN_LENGTH = 15
        const val MAX_LENGTH = 20
        const val FIXED_CHAR_COUNT = 4
    }
}
