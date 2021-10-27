package com.wire.android.feature.conversation.content.mapper

import com.waz.model.Messages
import com.wire.android.UnitTest
import com.wire.android.feature.conversation.content.Content
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class MessageContentMapperTest : UnitTest() {

    private lateinit var messageContentMapper: MessageContentMapper

    @Before
    fun set() {
        messageContentMapper = MessageContentMapper()
    }

    @Test
    fun `given a text content, when calling fromContentToString, then returns proper string value`() {
        messageContentMapper.fromContentToString(Content.Text("Hello")) shouldBeEqualTo "Hello"
    }

    @Test
    fun `given a text content, when calling fromContentToString, then returns proper type string`() {
        messageContentMapper.fromContentToStringType(Content.Text("Hello")) shouldBeEqualTo TEST_MESSAGE_TYPE_TEXT
    }

    @Test
    fun `given a text type and raw content, when calling fromStringToContent, then returns proper Content`() {
        messageContentMapper.fromStringToContent(TEST_MESSAGE_TYPE_TEXT, "Hello") shouldBeEqualTo Content.Text("Hello")
    }

    @Test
    fun `given a messageUid and a text content, when calling fromContentToPlainMessage, then should add Uid to the message`() {
        val messageId = "some cool id"
        val content = Content.Text("Servus")

        val genericMessage = getGenericMessageFromMappedContent(messageId, content)

        genericMessage.messageId shouldBeEqualTo messageId
    }

    @Test
    fun `given a messageUid and a text content, when calling fromContentToPlainMessage, then should add text to the message`() {
        val messageId = "some cool id v2"
        val content = Content.Text("Servus")

        val genericMessage = getGenericMessageFromMappedContent(messageId, content)

        genericMessage.text.content shouldBeEqualTo content.value
    }

    private fun getGenericMessageFromMappedContent(
        messageId: String,
        content: Content.Text
    ): Messages.GenericMessage {
        val result = messageContentMapper.fromContentToPlainMessage(messageId, content)
        return Messages.GenericMessage.parseFrom(result.data)
    }

    companion object {
        private const val TEST_MESSAGE_TYPE_TEXT = "text"
    }

}
