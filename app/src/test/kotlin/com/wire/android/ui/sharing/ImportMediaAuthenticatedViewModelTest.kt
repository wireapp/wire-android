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
package com.wire.android.ui.sharing

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.paging.PagingData
import app.cash.turbine.test
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.SnapshotExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.config.mockUri
import com.wire.android.framework.TestConversationItem
import com.wire.android.framework.TestUser
import com.wire.android.ui.home.conversations.usecase.GetConversationsFromSearchUseCase
import com.wire.android.ui.home.conversations.usecase.HandleUriAssetUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.ObserveSelfDeletionTimerSettingsForConversationUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.PersistNewSelfDeletionTimerUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class, SnapshotExtension::class)
class ImportMediaAuthenticatedViewModelTest {
    private val dispatcherProvider = TestDispatcherProvider()

    @Test
    fun `given search query, when collecting conversations, then call use case with proper params`() = runTest(dispatcherProvider.main()) {
        // Given
        val searchQueryText = "search"
        val (arrangement, viewModel) = Arrangement().arrange()
        viewModel.importMediaState.conversations.test {
            // When
            viewModel.searchQueryTextState.setTextAndPlaceCursorAtEnd(searchQueryText)
            advanceUntilIdle()
            // Then
            coVerify(exactly = 1) {
                arrangement.getConversationsPaginated(
                    searchQuery = searchQueryText,
                    fromArchive = false,
                    newActivitiesOnTop = false,
                    onlyInteractionEnabled = true,
                    useStrictMlsFilter = false,
                )
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    inner class Arrangement {

        @MockK
        lateinit var getSelfUser: ObserveSelfUserUseCase

        @MockK
        lateinit var getConversationsPaginated: GetConversationsFromSearchUseCase

        @MockK
        lateinit var handleUriAssetUseCase: HandleUriAssetUseCase

        @MockK
        lateinit var persistNewSelfDeletionTimerUseCase: PersistNewSelfDeletionTimerUseCase

        @MockK
        lateinit var observeSelfDeletionSettingsForConversation: ObserveSelfDeletionTimerSettingsForConversationUseCase

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            coEvery {
                getConversationsPaginated.invoke(any(), any(), any(), any(), useStrictMlsFilter = any())
            } returns flowOf(
                PagingData.from(listOf(TestConversationItem.CONNECTION, TestConversationItem.PRIVATE, TestConversationItem.GROUP))
            )
            coEvery {
                getSelfUser.invoke()
            } returns flowOf(TestUser.SELF_USER)
            mockUri()
        }

        fun arrange() = this to ImportMediaAuthenticatedViewModel(
            getSelf = getSelfUser,
            getConversationsPaginated = getConversationsPaginated,
            handleUriAsset = handleUriAssetUseCase,
            persistNewSelfDeletionTimerUseCase = persistNewSelfDeletionTimerUseCase,
            observeSelfDeletionSettingsForConversation = observeSelfDeletionSettingsForConversation,
            dispatchers = dispatcherProvider,
        )
    }
}
