package com.wire.android.shared.user.username

import com.wire.android.UnitTest
import com.wire.android.core.extension.EMPTY
import com.wire.android.framework.collections.second
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeLessThan
import org.amshove.kluent.shouldStartWith
import org.junit.Before
import org.junit.Test

class UsernameGeneratorTest : UnitTest() {

    private lateinit var usernameGeneration: UsernameAttemptsGenerator

    @Before
    fun setup() {
        usernameGeneration = UsernameAttemptsGenerator(adjectivesArray, wordsArray)
    }

    @Test
    fun `given any name and two attempts, when username is generated, then assert two attempts have been created`() {
        val result = usernameGeneration.generateUsernameAttempts(String.EMPTY, ATTEMPTS_TOTAL_FOR_EACH_LIST)

        result.size shouldBeEqualTo ATTEMPTS_TOTAL
    }

    @Test
    fun `given name without accents, when username is generated, then assert first item is lower case name`() {
        val name = "Martin"
        val result = usernameGeneration.generateUsernameAttempts(name, ATTEMPTS_TOTAL_FOR_EACH_LIST)

        result.first() shouldStartWith name.toLowerCase()
    }

    @Test
    fun `given name with accents, when username is generated, then assert first item is lower case and normalised name`() {
        val name = "Martiné"
        val result = usernameGeneration.generateUsernameAttempts(name, ATTEMPTS_TOTAL_FOR_EACH_LIST)

        result.first() shouldStartWith "martine"
    }

    @Test
    fun `given no name, when two attempts are generated, then assert second item is a random concatenation of two arrays in lowercase`() {
        val result = usernameGeneration.generateUsernameAttempts(String.EMPTY, ATTEMPTS_TOTAL_FOR_EACH_LIST)

        result.second() shouldStartWith "${TEST_ADJECTIVE.toLowerCase()}${TEST_RANDOM_WORD.toLowerCase()}"
    }

    @Test
    fun `given very long name chars and two attempts, when username is generated, then assert name username does not exceed max length`() {
        val name = "NjIgRYHTci02DUEWbhd7e71BgzEv9Z8pAOytZC4o0h7usOIkCqZa4Uxl626oAgN391ivyeoeBIi3MzEgEvVgk2hRh7ESJmm6ieVocpkuixu7TRzj1" +
                "SMRdC1kbJ9u4WjhJZJ1gURR31ftkppsnmV6SvOfdLpLSXIflL8mWwlNE1ZNaUyC9uz2YlYlKKY4BgW1YX0by9Zl"

        val result = usernameGeneration.generateUsernameAttempts(name, ATTEMPTS_TOTAL_FOR_EACH_LIST)

        result.first().length shouldBeLessThan USERNAME_MAX_LENGTH
    }

    companion object {
        private const val TEST_ADJECTIVE = "Monkey"
        private const val TEST_RANDOM_WORD = "Watermelon"
        private const val LIST_TYPES_OF_USERNAME = 2
        private const val ATTEMPTS_TOTAL = 2
        private const val USERNAME_MAX_LENGTH = 256
        private val adjectivesArray = arrayOf(TEST_ADJECTIVE)
        private val wordsArray = arrayOf(TEST_RANDOM_WORD)
        private val ATTEMPTS_TOTAL_FOR_EACH_LIST
            get() = ATTEMPTS_TOTAL / LIST_TYPES_OF_USERNAME
    }
}
