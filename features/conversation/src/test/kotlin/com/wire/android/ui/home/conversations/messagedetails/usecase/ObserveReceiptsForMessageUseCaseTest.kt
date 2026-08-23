/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

package com.wire.android.ui.home.conversations.messagedetails.usecase

import com.wire.android.config.TestDispatcherProvider
import com.wire.android.mapper.UIParticipantMapper
import com.wire.android.mapper.testUIParticipant
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.message.UserSummary
import com.wire.kalium.logic.data.message.receipt.DetailedReceipt
import com.wire.kalium.logic.data.message.receipt.ReceiptType
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.data.user.type.UserTypeInfo
import com.wire.kalium.logic.feature.message.ObserveMessageReceiptsUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveReceiptsForMessageUseCaseTest {

    @Test
    fun givenOrderedReceipts_whenObserving_thenMapsEachReceiptAndPreservesOrder() = runTest {
        val receipts = listOf(receipt(0), receipt(1), receipt(2))
        val (arrangement, useCase) = Arrangement()
            .withReceipts(receipts)
            .withParticipantMappings(receipts)
            .arrange()

        val result = useCase(CONVERSATION_ID, MESSAGE_ID, ReceiptType.READ).first()

        assertEquals(
            listOf(testUIParticipant(0), testUIParticipant(1), testUIParticipant(2)),
            result.readReceipts,
        )
        coVerify(exactly = 1) {
            arrangement.observeMessageReceipts(CONVERSATION_ID, MESSAGE_ID, ReceiptType.READ)
        }
        receipts.forEach { receipt ->
            io.mockk.verify(exactly = 1) { arrangement.uiParticipantMapper.toUIParticipant(receipt) }
        }
    }

    private class Arrangement {

        @MockK
        lateinit var observeMessageReceipts: ObserveMessageReceiptsUseCase

        @MockK
        lateinit var uiParticipantMapper: UIParticipantMapper

        init {
            MockKAnnotations.init(this)
        }

        fun withReceipts(receipts: List<DetailedReceipt>) = apply {
            coEvery { observeMessageReceipts(any(), any(), any()) } returns flowOf(receipts)
        }

        fun withParticipantMappings(receipts: List<DetailedReceipt>) = apply {
            receipts.forEachIndexed { index, receipt ->
                io.mockk.every { uiParticipantMapper.toUIParticipant(receipt) } returns testUIParticipant(index)
            }
        }

        fun arrange() = this to ObserveReceiptsForMessageUseCase(
            observeMessageReceipts = observeMessageReceipts,
            uiParticipantMapper = uiParticipantMapper,
            dispatchers = TestDispatcherProvider(),
        )
    }

    private companion object {
        val CONVERSATION_ID = ConversationId("conversation", "domain")
        const val MESSAGE_ID = "message"

        fun receipt(index: Int) = DetailedReceipt(
            type = ReceiptType.READ,
            date = Instant.parse("2026-01-01T00:00:0${index}Z"),
            userSummary = UserSummary(
                userId = QualifiedID("user$index", "domain$index"),
                userName = "User $index",
                userHandle = "user$index",
                userPreviewAssetId = null,
                userType = UserTypeInfo.Regular(UserType.NONE),
                isUserDeleted = false,
                connectionStatus = ConnectionState.ACCEPTED,
                availabilityStatus = UserAvailabilityStatus.AVAILABLE,
                accentId = index,
            ),
        )
    }
}
