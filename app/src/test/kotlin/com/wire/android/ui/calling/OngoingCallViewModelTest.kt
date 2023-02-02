/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui.calling

import androidx.lifecycle.SavedStateHandle
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.calling.ongoing.OngoingCallViewModel
import com.wire.android.util.CurrentScreenManager
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.RequestVideoStreamsUseCase
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.jupiter.api.BeforeEach

@OptIn(ExperimentalCoroutinesApi::class)
class OngoingCallViewModelTest {

    @MockK
    private lateinit var savedStateHandle: SavedStateHandle

    @MockK
    private lateinit var navigationManager: NavigationManager

    @MockK
    private lateinit var establishedCall: ObserveEstablishedCallsUseCase

    @MockK
    private lateinit var requestVideoStreams: RequestVideoStreamsUseCase

    @MockK
    private lateinit var qualifiedIdMapper: QualifiedIdMapper

    @MockK
    private lateinit var currentScreenManager: CurrentScreenManager


    private lateinit var ongoingCallViewModel: OngoingCallViewModel

    @BeforeEach
    fun setup() {
        val scheduler = TestCoroutineScheduler()
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        val dummyConversationId = "some-dummy-value@some.dummy.domain"
        MockKAnnotations.init(this)
        every { savedStateHandle.get<String>(any()) } returns dummyConversationId
        every { savedStateHandle.set(any(), any<String>()) } returns Unit

        ongoingCallViewModel = OngoingCallViewModel(
            savedStateHandle = savedStateHandle,
            qualifiedIdMapper = qualifiedIdMapper,
            navigationManager = navigationManager,
            establishedCall = establishedCall,
            requestVideoStreams = requestVideoStreams,
            currentScreenManager = currentScreenManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
}
