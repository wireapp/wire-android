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

package com.wire.android.ui.home.conversations.info

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.mockUri
import com.wire.android.framework.TestUser
import com.wire.android.ui.home.conversations.ConversationNavArgs
import com.ramcosta.composedestinations.generated.app.navArgs
import com.wire.kalium.common.error.StorageFailure
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.client.IsWireCellsEnabledUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.FetchConversationMLSVerificationStatusUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class ConversationInfoViewModelArrangement {

    val conversationId: ConversationId = ConversationId("some-dummy-value", "some.dummy.domain")

    private val conversationDetailsChannel = Channel<ConversationDetails>(capacity = Channel.UNLIMITED)

    @MockK
    lateinit var qualifiedIdMapper: QualifiedIdMapper

    @MockK
    private lateinit var savedStateHandle: SavedStateHandle

    @MockK
    lateinit var observeConversationDetails: ObserveConversationDetailsUseCase

    @MockK
    lateinit var fetchConversationMLSVerificationStatus: FetchConversationMLSVerificationStatusUseCase

    @MockK
    lateinit var isCellsEnabled: IsWireCellsEnabledUseCase

    private val viewModel: ConversationInfoViewModel by lazy {
        ConversationInfoViewModel(
            qualifiedIdMapper = qualifiedIdMapper,
            savedStateHandle = savedStateHandle,
            observeConversationDetails = observeConversationDetails,
            fetchConversationMLSVerificationStatus = fetchConversationMLSVerificationStatus,
            selfUserId = TestUser.SELF_USER_ID,
            isWireCellFeatureEnabled = isCellsEnabled,
        )
    }

    init {
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockUri()
        every { savedStateHandle.navArgs<ConversationNavArgs>() } returns ConversationNavArgs(conversationId = conversationId)

        every {
            qualifiedIdMapper.fromStringToQualifiedID("some-dummy-value@some.dummy.domain")
        } returns QualifiedID("some-dummy-value", "some.dummy.domain")
        coEvery { observeConversationDetails(any()) } returns conversationDetailsChannel.consumeAsFlow().map {
            ObserveConversationDetailsUseCase.Result.Success(it)
        }
        coEvery { fetchConversationMLSVerificationStatus.invoke(any()) } returns Unit
        coEvery { isCellsEnabled() } returns false
    }

    suspend fun withConversationDetailUpdate(conversationDetails: ConversationDetails) = apply {
        coEvery {
            qualifiedIdMapper.fromStringToQualifiedID("id@domain")
        } returns QualifiedID("id", "domain")
        conversationDetailsChannel.send(conversationDetails)
    }

    suspend fun withConversationDetailFailure(failure: StorageFailure) = apply {
        coEvery { observeConversationDetails(any()) } returns flowOf(ObserveConversationDetailsUseCase.Result.Failure(failure))
    }

    fun withMentionedUserId(id: UserId) = apply {
        every { qualifiedIdMapper.fromStringToQualifiedID(id.toString()) } returns id
    }

    fun arrange() = this to viewModel
}
