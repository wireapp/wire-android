package com.wire.android.shared.user.username

import com.wire.android.core.extension.EMPTY
import com.wire.android.core.extension.removeAccents
import kotlin.random.Random

class UsernameAttemptsGenerator(
    private val randomAdjectives: Array<String>,
    private val randomWords: Array<String>
) {

    fun generateUsernameAttempts(name: String, attempts: Int = ATTEMPTS_TOTAL / 2): List<String> {
        val nameBasedUsernameAttempts = buildAttempts(generateUsernameFromName(name), attempts)
        val randomUsernameAttempts = buildAttempts(generateRandomUsername(), attempts)
        return listOf(nameBasedUsernameAttempts, randomUsernameAttempts).flatten()
    }

    private fun buildAttempts(username: String, attempts: Int): List<String> =
        (1..attempts)
            .map(::generateTrailingNumbers)
            .map { "$username${checkAndTrimForUsernameLimit(username, it)}" }

    private fun checkAndTrimForUsernameLimit(username: String, input: String): String =
        input.substring(0, input.length.coerceAtMost(USERNAME_MAX_LENGTH - username.length))

    private fun generateRandomUsername(): String =
        "${randomAdjectives.random()}${randomWords.random()}".toLowerCase()

    private fun generateUsernameFromName(name: String): String =
        name.removeAccents().toLowerCase()

    private fun generateTrailingNumbers(attemptNumber: Int) =
        (TRAILING_NUMBER_STARTING_POINT..Random.nextInt(RANDOM_TRAILING_NUMBER_LIMIT + attemptNumber))
            .map { ALLOWED_CHARS.random() }
            .joinToString(String.EMPTY)

    companion object {
        private val ALLOWED_CHARS = ('0'..'9')
        private const val TRAILING_NUMBER_STARTING_POINT = 3
        private const val ATTEMPTS_TOTAL = 40
        private const val USERNAME_MAX_LENGTH = 256
        private const val RANDOM_TRAILING_NUMBER_LIMIT = 12
    }
}
