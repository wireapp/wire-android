package com.wire.android.core.crypto.data

import com.wire.android.AndroidTest
import com.wire.android.core.crypto.model.UserID
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class PreKeyLocalDataSourceTest : AndroidTest() {

    @Test
    fun `given a value is stored, when attempting to read it, then the same value should be returned`() {
        val subject = PreKeyLocalDataSource(context())

        subject.updateLastPreKeyID(UserID("123"), 42)

        subject.lastPreKeyId(UserID("123")) shouldBeEqualTo 42
    }

    @Test
    fun `given a value is updated for user B, when reading from user A, then it should return from A`() {
        val subject = PreKeyLocalDataSource(context())
        val userA = UserID("A")
        val userB = UserID("B")

        subject.updateLastPreKeyID(userA, 42)
        subject.updateLastPreKeyID(userB, 84)

        subject.lastPreKeyId(userA) shouldBeEqualTo 42
    }

    @Test
    fun `given two storages for the same User, when one is updated, then both should have the updated value`() {
        val userID = UserID("123")

        val subjectOne = PreKeyLocalDataSource(context())
        val subjectTwo = PreKeyLocalDataSource(context())

        subjectOne.updateLastPreKeyID(userID, 42)

        subjectOne.lastPreKeyId(userID) shouldBeEqualTo 42
        subjectTwo.lastPreKeyId(userID) shouldBeEqualTo 42
    }
}
