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
 */
package com.wire.android.ui.home.conversations.banner

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.config.ScopedArgsTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.di.scopedArgs
import com.wire.android.ui.common.banner.SecurityClassificationArgs
import com.wire.android.ui.common.banner.SecurityClassificationViewModelImpl
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.conversation.GetOtherUserSecurityClassificationLabelUseCase
import com.wire.kalium.logic.feature.conversation.ObserveSecurityClassificationLabelUseCase
import com.wire.kalium.logic.feature.conversation.SecurityClassificationType
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
@ExtendWith(ScopedArgsTestExtension::class)
@ExtendWith(NavigationTestExtension::class)
class SecurityClassificationViewModelTest {

    @Test
    fun `given conversationId, when observing classification, then update state`() = runTest {
        // Given
        val classificationChannel = Channel<SecurityClassificationType>(capacity = Channel.UNLIMITED)

        val (arrangement, viewModel) = Arrangement()
            .withArgs(SecurityClassificationArgs(conversationId = CONVERSATION_ID, userId = null))
            .withSecurityClassificationLabel(classificationChannel.consumeAsFlow())
            .withOtherUserSecurityClassificationLabel(SecurityClassificationType.CLASSIFIED)
            .arrange()

        // When
        classificationChannel.send(SecurityClassificationType.CLASSIFIED)

        // Then
        arrangement.observeSecurityClassificationLabel(CONVERSATION_ID).test {
            classificationChannel.send(SecurityClassificationType.CLASSIFIED)
            awaitItem()
            println("assert equals")
            assertEquals(SecurityClassificationType.CLASSIFIED, viewModel.state())
        }
    }

    private companion object {
        val CONVERSATION_ID = ConversationId("some-dummy-value", "some.dummy.domain")
        val USER_ID = UserId("user_value", "user.domain")
    }

    private class Arrangement {

        @MockK
        lateinit var observeSecurityClassificationLabel: ObserveSecurityClassificationLabelUseCase

        @MockK
        lateinit var getOtherUserSecurityClassificationLabel: GetOtherUserSecurityClassificationLabelUseCase

        @MockK
        lateinit var savedStateHandle: SavedStateHandle

        init {
            MockKAnnotations.init(this,  relaxUnitFun = true)
            every { savedStateHandle.scopedArgs<SecurityClassificationArgs>() } returns SecurityClassificationArgs(CONVERSATION_ID, USER_ID)
        }

        private val viewModel = SecurityClassificationViewModelImpl(
            TestDispatcherProvider(),
            observeSecurityClassificationLabel,
            getOtherUserSecurityClassificationLabel,
            savedStateHandle
        )

        fun withArgs(
            args: SecurityClassificationArgs
        ) = apply {
            every { savedStateHandle.scopedArgs<SecurityClassificationArgs>() } returns args
        }

        fun withSecurityClassificationLabel(
            result: Flow<SecurityClassificationType>
        ) = apply {
            coEvery { observeSecurityClassificationLabel.invoke(any()) } returns result
        }

        fun withOtherUserSecurityClassificationLabel(
            result: SecurityClassificationType
        ) = apply {
            coEvery { getOtherUserSecurityClassificationLabel(any()) } returns result
        }

        fun arrange() = this to viewModel
    }
}
