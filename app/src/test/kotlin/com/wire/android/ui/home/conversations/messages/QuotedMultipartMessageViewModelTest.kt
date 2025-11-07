/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.ui.home.conversations.messages

import app.cash.turbine.test
import com.wire.android.ui.home.conversations.model.UIMultipartQuotedContent
import com.wire.android.ui.home.conversations.model.UIQuotedMessage
import com.wire.android.ui.home.conversations.usecase.ObserveQuoteMessageForConversationUseCase
import com.wire.android.ui.theme.Accent
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.UserId
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull

class QuotedMultipartMessageViewModelTest {

    @Test
    fun `given a quoted message with multiple attachments when observing then valid data is returned`() = runTest {
        val (_, viewModel) = Arrangement()
            .withQuotedMessageContent(
                UIQuotedMessage.UIQuotedData.Multipart(
                    text = UIText.DynamicString("Multipart message"),
                    attachments = listOf(videoAttachment, imageAttachment, fileAttachment)
                )
            )
            .arrange()

        viewModel.observeMultipartMessage("message_id").test {
            val data = awaitItem()

            assertNull(data.fileAttachment)
            assertNull(data.mediaAttachment)
            assertEquals(3, data.attachmentsCount)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given a quoted message with one media attachment when observing then valid data is returned`() = runTest {
        val (_, viewModel) = Arrangement()
            .withQuotedMessageContent(
                UIQuotedMessage.UIQuotedData.Multipart(
                    text = UIText.DynamicString("Multipart message"),
                    attachments = listOf(videoAttachment)
                )
            )
            .arrange()

        viewModel.observeMultipartMessage("message_id").test {
            val data = awaitItem()

            assertNull(data.fileAttachment)
            assertEquals(videoAttachment, data.mediaAttachment)
            assertEquals(1, data.attachmentsCount)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given a quoted message with one file attachment when observing then valid data is returned`() = runTest {
        val (_, viewModel) = Arrangement()
            .withQuotedMessageContent(
                UIQuotedMessage.UIQuotedData.Multipart(
                    text = UIText.DynamicString("Multipart message"),
                    attachments = listOf(fileAttachment)
                )
            )
            .arrange()

        viewModel.observeMultipartMessage("message_id").test {
            val data = awaitItem()

            assertNull(data.mediaAttachment)
            assertEquals(fileAttachment, data.fileAttachment)
            assertEquals(1, data.attachmentsCount)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given non-multipart quoted message no data emitted`() = runTest {
        val (_, viewModel) = Arrangement()
            .withQuotedMessageContent(
                UIQuotedMessage.UIQuotedData.Text(
                    value = UIText.DynamicString("Just a text message")
                )
            )
            .arrange()

        viewModel.observeMultipartMessage("message_id").test {
            awaitComplete()
        }
    }

    private class Arrangement {

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        @MockK
        lateinit var observeQuotedMessage: ObserveQuoteMessageForConversationUseCase

        fun withQuotedMessageContent(content: UIQuotedMessage.UIQuotedData.Content) = apply {
            coEvery {
                observeQuotedMessage.invoke(any(), any())
            } returns flowOf(
                UIQuotedMessage.UIQuotedData(
                    messageId = "message_id",
                    senderId = UserId("user", "domain"),
                    senderName = UIText.DynamicString("Sender Name"),
                    senderAccent = Accent.Unknown,
                    originalMessageDateDescription = UIText.DynamicString("Date"),
                    editedTimeDescription = null,
                    quotedContent = content,
                )
            )
        }

        fun arrange(): Pair<Arrangement, QuotedMultipartMessageViewModel> {
            val viewModel = QuotedMultipartMessageViewModel(
                conversationId = QualifiedID("convo-id", "convo.domain"),
                messageId = "test_message_id",
                observeQuotedMessage = observeQuotedMessage
            )
            return this to viewModel
        }
    }

    private companion object {
        val videoAttachment = UIMultipartQuotedContent(
            name = "video1.mp4",
            localPath = null,
            previewUrl = null,
            mimeType = "video/mp4",
            assetAvailable = true
        )
        val imageAttachment = UIMultipartQuotedContent(
            name = "image1.png",
            localPath = null,
            previewUrl = null,
            mimeType = "image/png",
            assetAvailable = true
        )
        val fileAttachment = UIMultipartQuotedContent(
            name = "document.pdf",
            localPath = null,
            previewUrl = null,
            mimeType = "application/pdf",
            assetAvailable = true
        )
    }
}
