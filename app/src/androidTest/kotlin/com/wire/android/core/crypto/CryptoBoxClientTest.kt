package com.wire.android.core.crypto

import androidx.test.filters.RequiresDevice
import com.wire.android.InstrumentationTest
import com.wire.android.core.crypto.data.PreKeyRepository
import com.wire.android.core.crypto.mapper.PreKeyMapper
import com.wire.android.core.crypto.model.PreKey
import com.wire.android.core.crypto.model.UserId
import com.wire.android.core.functional.map
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@RequiresDevice
class CryptoBoxClientTest : InstrumentationTest() {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @MockK
    private lateinit var repository: PreKeyRepository

    @MockK
    private lateinit var mapper: PreKeyMapper

    lateinit var subject: CryptoBoxClient

    private val userId = UserId("abc")

    @Before
    fun setup() {
        subject = CryptoBoxClient(appContext, repository, userId, mapper)
    }

    @Test
    fun givenPreKeysAreNeeded_whenTheyAreCreated_thenTheRepositoryIsUpdated() {
        val preKey = PreKey(42, "data")
        every { mapper.fromCryptoBoxModel(any()) } returns preKey

        subject.createInitialPreKeys()
        verify(exactly = 1) {
            repository.updateLastPreKeyIdForUser(userId, any())
        }
    }

    @Test
    fun givenPreKeysAreGenerated_whenConverting_theMapperShouldBeUsed() {
        val preKey = PreKey(42, "data")
        every { mapper.fromCryptoBoxModel(any()) } returns preKey

        val generated = subject.createInitialPreKeys()
        generated.isRight shouldBe true

        generated.map {
            val allKeys = it.createdKeys + it.lastKey
            verify(exactly = allKeys.size) { mapper.fromCryptoBoxModel(any()) }
        }
    }

    @Test
    fun givenPreKeysAreGenerated_whenReturning_theMapperResultShouldBeUsed() {
        val preKey = PreKey(42, "data")
        every { mapper.fromCryptoBoxModel(any()) } returns preKey

        val generated = subject.createInitialPreKeys()
        generated.isRight shouldBe true

        generated.map{
            it.lastKey shouldBeEqualTo preKey
            it.createdKeys shouldContainSame generateSequence { preKey }
                .take(it.createdKeys.size)
                .toList()
        }
    }

    @Test
    fun givenPreKeysAreGenerated_whenStoring_theLastPreKeyIdShouldBeUsed() {
        val preKey = PreKey(42, "data")
        every { mapper.fromCryptoBoxModel(any()) } returns preKey

        val result = subject.createInitialPreKeys()
        result.isRight shouldBe true

        result.map {
            val lastKeyId = it.createdKeys.last().id
            verify {
                repository.updateLastPreKeyIdForUser(userId, lastKeyId)
            }
        }
    }
}
