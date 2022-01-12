package com.wire.android.feature.conversation.data

import com.wire.android.UnitTest
import com.wire.android.feature.conversation.Group
import com.wire.android.feature.conversation.IncomingConnection
import com.wire.android.feature.conversation.OneToOne
import com.wire.android.feature.conversation.Self
import com.wire.android.feature.conversation.Unknown
import com.wire.android.feature.conversation.WaitingForConnection
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class ConversationTypeMapperTest : UnitTest() {

    private lateinit var conversationTypeMapper: ConversationTypeMapper

    @Before
    fun setUp() {
        conversationTypeMapper = ConversationTypeMapper()
    }

    @Test
    fun `given fromIntValue is called with an int, then returns proper type`() {
        conversationTypeMapper.fromIntValue(0) shouldBeEqualTo Group
        conversationTypeMapper.fromIntValue(1) shouldBeEqualTo Self
        conversationTypeMapper.fromIntValue(2) shouldBeEqualTo OneToOne
        conversationTypeMapper.fromIntValue(3) shouldBeEqualTo WaitingForConnection
        conversationTypeMapper.fromIntValue(4) shouldBeEqualTo IncomingConnection
        conversationTypeMapper.fromIntValue(-1) shouldBeEqualTo Unknown
    }

    @Test
    fun `given toIntValue is called with a type, then returns proper int value`() {
        conversationTypeMapper.toIntValue(Group) shouldBeEqualTo 0
        conversationTypeMapper.toIntValue(Self) shouldBeEqualTo 1
        conversationTypeMapper.toIntValue(OneToOne) shouldBeEqualTo 2
        conversationTypeMapper.toIntValue(WaitingForConnection) shouldBeEqualTo 3
        conversationTypeMapper.toIntValue(IncomingConnection) shouldBeEqualTo 4
        conversationTypeMapper.toIntValue(Unknown) shouldBeEqualTo -1
    }
}
