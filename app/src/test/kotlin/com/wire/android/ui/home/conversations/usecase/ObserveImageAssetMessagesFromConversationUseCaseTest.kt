/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package com.wire.android.ui.home.conversations.usecase

import androidx.paging.PagingData
import androidx.paging.testing.asSnapshot
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.mapper.UIAssetMapper
import com.wire.android.util.time.TimeZoneProvider
import com.wire.kalium.logic.data.asset.AssetMessage
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.asset.ObservePaginatedAssetImageMessages
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import okio.Path.Companion.toPath
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class ObserveImageAssetMessagesFromConversationUseCaseTest {

    @Test
    fun `given asset messages across months, when use case is invoked, then date separators are inserted correctly`() = runTest {
        // Given
        val (_, useCase) = Arrangement()
            .sendPagingData(
                listOf(
                    assetMessage("asset1", Instant.parse("2023-10-12T10:00:00.671Z")),
                    assetMessage(
                        "asset2", Instant.parse("2023-11-12T10:00:00.671Z")
                    )
                )
            )
            .arrange()

        // When
        val result = useCase(conversationId = ConversationId("test-conversation-id", "test-domain"), initialOffset = 0)

        // Then
        val items: List<UIImageAssetPagingItem> = result.asSnapshot()

        assertInstanceOf(UIImageAssetPagingItem.Label::class.java, items[0])
        assertInstanceOf(UIImageAssetPagingItem.Asset::class.java, items[1])
        assertInstanceOf(UIImageAssetPagingItem.Label::class.java, items[2])
        assertInstanceOf(UIImageAssetPagingItem.Asset::class.java, items[3])
    }

    private class Arrangement {

        @MockK
        lateinit var getAssetMessages: ObservePaginatedAssetImageMessages

        @MockK
        lateinit var timeZoneProvider: TimeZoneProvider

        private val useCase: ObserveImageAssetMessagesFromConversationUseCase by lazy {
            ObserveImageAssetMessagesFromConversationUseCase(
                getAssetMessages, UIAssetMapper(), TestDispatcherProvider(), timeZoneProvider,
            )
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { timeZoneProvider.currentSystemDefault() } returns TimeZone.of("UTC")
        }

        suspend fun sendPagingData(pagingItems: List<AssetMessage>) = apply {
            coEvery {
                getAssetMessages(
                    any(),
                    any(),
                    any()
                )
            } returns flowOf(PagingData.from(pagingItems))
        }

        fun arrange() = this to useCase
    }

    companion object {
        fun assetMessage(assetId: String, time: Instant = Instant.parse("2023-11-12T10:00:00.671Z")) = AssetMessage(
            time = time,
            conversationId = ConversationId("value", "domain"),
            username = "username",
            messageId = "messageId",
            assetId = assetId,
            width = 640,
            height = 480,
            assetPath = "asset/path".toPath(),
            isSelfAsset = false
        )
    }
}
