package com.wire.android.feature.conversation.conversation.mapper

import com.wire.android.UnitTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import com.wire.android.feature.conversation.conversation.Default
import com.wire.android.feature.conversation.conversation.Deleted
import com.wire.android.feature.conversation.conversation.FailedRead
import com.wire.android.feature.conversation.conversation.Sent
import com.wire.android.feature.conversation.conversation.Delivered
import com.wire.android.feature.conversation.conversation.Failed
import com.wire.android.feature.conversation.conversation.Pending

class MessageStateMapperTest : UnitTest() {

    private lateinit var messageStateMapper: MessageStateMapper

    @Before
    fun set() {
        messageStateMapper = MessageStateMapper()
    }

    @Test
    fun `given fromStringValue is called with a string, then returns proper MessageState`() {
        messageStateMapper.fromStringValue(TEST_SENT_STATE) shouldBeEqualTo Sent
        messageStateMapper.fromStringValue(TEST_PENDING_STATE) shouldBeEqualTo Pending
        messageStateMapper.fromStringValue(TEST_FAILED_STATE) shouldBeEqualTo Failed
        messageStateMapper.fromStringValue(TEST_DELIVERED_STATE) shouldBeEqualTo Delivered
        messageStateMapper.fromStringValue(TEST_FAILED_READ_STATE) shouldBeEqualTo FailedRead
        messageStateMapper.fromStringValue(TEST_DELETED_STATE) shouldBeEqualTo Deleted
        messageStateMapper.fromStringValue(TEST_DEFAULT_STATE) shouldBeEqualTo Default
    }

    @Test
    fun `given fromValueToString is called with a MessageState, then returns proper string`() {
        messageStateMapper.fromValueToString(Sent) shouldBeEqualTo TEST_SENT_STATE
        messageStateMapper.fromValueToString(Pending) shouldBeEqualTo TEST_PENDING_STATE
        messageStateMapper.fromValueToString(Failed) shouldBeEqualTo TEST_FAILED_STATE
        messageStateMapper.fromValueToString(Delivered) shouldBeEqualTo TEST_DELIVERED_STATE
        messageStateMapper.fromValueToString(FailedRead) shouldBeEqualTo TEST_FAILED_READ_STATE
        messageStateMapper.fromValueToString(Deleted) shouldBeEqualTo TEST_DELETED_STATE
        messageStateMapper.fromValueToString(Default) shouldBeEqualTo TEST_DEFAULT_STATE
    }

    companion object {
        private const val TEST_SENT_STATE = "sent"
        private const val TEST_PENDING_STATE = "pending"
        private const val TEST_FAILED_STATE = "failed"
        private const val TEST_DELIVERED_STATE = "delivered"
        private const val TEST_FAILED_READ_STATE = "failed_read"
        private const val TEST_DELETED_STATE = "deleted"
        private const val TEST_DEFAULT_STATE = "default"
    }
}
