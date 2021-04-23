package com.wire.android.crypto.storage

import com.wire.android.crypto.AndroidTest
import com.wire.android.crypto.model.UserID
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class CryptoPreferencesStorageTest : AndroidTest() {

    @Test
    fun `given a value is stored, when attempting to read it, then the same value should be returned`() {
        val subject = CryptoPreferencesStorage(context(), UserID("123"))

        subject.updateLastPreKeyID(42)

        subject.lastPreKeyId() shouldBeEqualTo 42
    }

    @Test
    fun `given a value is updated on B, when reading from A, then it should return from A`() {
        val subjectA = CryptoPreferencesStorage(context(), UserID("123"))
        val subjectB = CryptoPreferencesStorage(context(), UserID("ABC"))

        subjectA.updateLastPreKeyID(42)
        subjectB.updateLastPreKeyID(84)

        subjectA.lastPreKeyId() `should be equal to` 42
    }

    @Test
    fun `given two storages for the same User, when one is updated, then both should have the updated value`() {
        val userID = UserID("123")

        val subjectOne = CryptoPreferencesStorage(context(), userID)
        val subjectTwo = CryptoPreferencesStorage(context(), userID)

        subjectOne.updateLastPreKeyID(42)

        subjectOne.lastPreKeyId() `should be equal to` 42
        subjectTwo.lastPreKeyId() `should be equal to` 42
    }
}
