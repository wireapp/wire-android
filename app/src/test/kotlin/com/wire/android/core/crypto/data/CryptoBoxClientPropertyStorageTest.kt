package com.wire.android.core.crypto.data

import com.wire.android.AndroidTest
import com.wire.android.core.crypto.model.UserId
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class CryptoBoxClientPropertyStorageTest : AndroidTest() {

    @Test
    fun `given a value is stored, when attempting to read it, then the same value should be returned`() {
        val subject = CryptoBoxClientPropertyStorage(context())

        subject.updateLastPreKeyId(UserId("123"), 42)

        subject.lastPreKeyId(UserId("123")) shouldBeEqualTo 42
    }

    @Test
    fun `given a value is updated for user B, when reading from user A, then it should return from A`() {
        val subject = CryptoBoxClientPropertyStorage(context())
        val userA = UserId("A")
        val userB = UserId("B")

        subject.updateLastPreKeyId(userA, 42)
        subject.updateLastPreKeyId(userB, 84)

        subject.lastPreKeyId(userA) shouldBeEqualTo 42
    }

    @Test
    fun `given two storages for the same User, when one is updated, then both should have the updated value`() {
        val userId = UserId("123")

        val subjectOne = CryptoBoxClientPropertyStorage(context())
        val subjectTwo = CryptoBoxClientPropertyStorage(context())

        subjectOne.updateLastPreKeyId(userId, 42)

        subjectOne.lastPreKeyId(userId) shouldBeEqualTo 42
        subjectTwo.lastPreKeyId(userId) shouldBeEqualTo 42
    }
}
