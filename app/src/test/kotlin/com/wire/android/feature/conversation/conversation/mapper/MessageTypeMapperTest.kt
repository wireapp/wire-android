package com.wire.android.feature.conversation.conversation.mapper

import com.wire.android.UnitTest
import com.wire.android.feature.conversation.conversation.Text
import com.wire.android.feature.conversation.conversation.Unknown
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class MessageTypeMapperTest : UnitTest() {

    private lateinit var messageTypeMapper: MessageTypeMapper

    @Before
    fun set() {
        messageTypeMapper = MessageTypeMapper()
    }

    @Test
    fun `given fromValueToString is called with a MessageType, then returns proper string`() {
        messageTypeMapper.fromValueToString(Text) shouldBeEqualTo TEST_MESSAGE_TYPE_TEXT
        messageTypeMapper.fromValueToString(Unknown) shouldBeEqualTo TEST_MESSAGE_TYPE_UNKNOWN
    }

    @Test
    fun `given fromStringValue is called with a string, then returns proper MessageType`() {
        messageTypeMapper.fromStringValue(TEST_MESSAGE_TYPE_TEXT) shouldBeEqualTo Text
        messageTypeMapper.fromStringValue(TEST_MESSAGE_TYPE_UNKNOWN) shouldBeEqualTo Unknown
    }

    companion object {
        private const val TEST_MESSAGE_TYPE_TEXT = "text"
        private const val TEST_MESSAGE_TYPE_UNKNOWN = "unknown"
    }

}
