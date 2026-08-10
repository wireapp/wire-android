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
package com.wire.android.di.metro

import androidx.lifecycle.SavedStateHandle
import com.wire.android.media.audiomessage.AudioMessageArgs
import com.wire.android.media.audiomessage.AudioMessageViewModelImpl
import com.wire.android.ui.home.conversations.CompositeMessageViewModelImpl
import com.wire.android.ui.home.conversations.ScopedMessageManualViewModelFactory
import com.wire.android.ui.home.conversations.edit.MessageOptionsMenuArgs
import com.wire.android.ui.home.conversations.edit.MessageOptionsMenuViewModelImpl
import com.wire.android.ui.home.conversations.messages.item.AssetLocalPathArgs
import com.wire.android.ui.home.conversations.messages.item.AssetLocalPathViewModelImpl
import com.wire.android.ui.home.conversations.model.CompositeMessageArgs
import com.wire.android.ui.home.conversations.typing.TypingIndicatorArgs
import com.wire.android.ui.home.conversations.typing.TypingIndicatorViewModelImpl
import com.wire.android.ui.home.messagecomposer.actions.SelfDeletingMessageActionArgs
import com.wire.android.ui.home.messagecomposer.actions.SelfDeletingMessageActionViewModelImpl
import com.wire.android.ui.home.messagecomposer.attachments.IsFileSharingEnabledViewModelImpl
import com.wire.android.ui.home.messagecomposer.recordaudio.RecordAudioViewModel
import dev.zacsweers.metro.Provider
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class ScopedMessageManualViewModelFactoryTest {

    @Test
    fun givenCompositeArguments_whenCreatingViewModel_thenDelegatesExactSavedStateHandleAndArguments() {
        val (arrangement, factory) = Arrangement().arrange()
        val savedStateHandle = SavedStateHandle()
        val args = mockk<CompositeMessageArgs>()
        every {
            arrangement.compositeMessageFactory.create(savedStateHandle, args)
        } returns arrangement.compositeMessageViewModel

        val result = factory.compositeMessageViewModel(savedStateHandle, args)

        assertSame(arrangement.compositeMessageViewModel, result)
        verify(exactly = 1) {
            arrangement.compositeMessageFactory.create(savedStateHandle, args)
        }
    }

    @Test
    fun givenMessageOptionsArguments_whenCreatingViewModel_thenDelegatesExactArguments() {
        val (arrangement, factory) = Arrangement().arrange()
        val args = mockk<MessageOptionsMenuArgs>()
        every { arrangement.messageOptionsMenuFactory.create(args) } returns arrangement.messageOptionsMenuViewModel

        val result = factory.messageOptionsMenuViewModel(args)

        assertSame(arrangement.messageOptionsMenuViewModel, result)
        verify(exactly = 1) { arrangement.messageOptionsMenuFactory.create(args) }
    }

    @Test
    fun givenTypingIndicatorArguments_whenCreatingViewModel_thenDelegatesExactArguments() {
        val (arrangement, factory) = Arrangement().arrange()
        val args = mockk<TypingIndicatorArgs>()
        every { arrangement.typingIndicatorFactory.create(args) } returns arrangement.typingIndicatorViewModel

        val result = factory.typingIndicatorViewModel(args)

        assertSame(arrangement.typingIndicatorViewModel, result)
        verify(exactly = 1) { arrangement.typingIndicatorFactory.create(args) }
    }

    @Test
    fun givenAssetLocalPathArguments_whenCreatingViewModel_thenDelegatesExactArguments() {
        val (arrangement, factory) = Arrangement().arrange()
        val args = mockk<AssetLocalPathArgs>()
        every { arrangement.assetLocalPathFactory.create(args) } returns arrangement.assetLocalPathViewModel

        val result = factory.assetLocalPathViewModel(args)

        assertSame(arrangement.assetLocalPathViewModel, result)
        verify(exactly = 1) { arrangement.assetLocalPathFactory.create(args) }
    }

    @Test
    fun givenSelfDeletingMessageArguments_whenCreatingViewModel_thenDelegatesExactArguments() {
        val (arrangement, factory) = Arrangement().arrange()
        val args = mockk<SelfDeletingMessageActionArgs>()
        every {
            arrangement.selfDeletingMessageActionFactory.create(args)
        } returns arrangement.selfDeletingMessageActionViewModel

        val result = factory.selfDeletingMessageActionViewModel(args)

        assertSame(arrangement.selfDeletingMessageActionViewModel, result)
        verify(exactly = 1) { arrangement.selfDeletingMessageActionFactory.create(args) }
    }

    @Test
    fun whenCreatingFileSharingViewModel_thenUsesProviderAndReturnsItsInstance() {
        val (arrangement, factory) = Arrangement().arrange()
        every { arrangement.isFileSharingEnabledProvider() } returns arrangement.isFileSharingEnabledViewModel

        val result = factory.isFileSharingEnabledViewModel()

        assertSame(arrangement.isFileSharingEnabledViewModel, result)
        verify(exactly = 1) { arrangement.isFileSharingEnabledProvider() }
    }

    @Test
    fun whenCreatingRecordAudioViewModel_thenUsesProviderAndReturnsItsInstance() {
        val (arrangement, factory) = Arrangement().arrange()
        every { arrangement.recordAudioProvider() } returns arrangement.recordAudioViewModel

        val result = factory.recordAudioViewModel()

        assertSame(arrangement.recordAudioViewModel, result)
        verify(exactly = 1) { arrangement.recordAudioProvider() }
    }

    @Test
    fun givenAudioMessageArguments_whenCreatingViewModel_thenDelegatesExactArguments() {
        val (arrangement, factory) = Arrangement().arrange()
        val args = mockk<AudioMessageArgs>()
        every { arrangement.audioMessageFactory.create(args) } returns arrangement.audioMessageViewModel

        val result = factory.audioMessageViewModel(args)

        assertSame(arrangement.audioMessageViewModel, result)
        verify(exactly = 1) { arrangement.audioMessageFactory.create(args) }
    }

    private class Arrangement {
        @MockK
        lateinit var compositeMessageFactory: CompositeMessageViewModelImpl.Factory

        @MockK
        lateinit var messageOptionsMenuFactory: MessageOptionsMenuViewModelImpl.Factory

        @MockK
        lateinit var typingIndicatorFactory: TypingIndicatorViewModelImpl.Factory

        @MockK
        lateinit var assetLocalPathFactory: AssetLocalPathViewModelImpl.Factory

        @MockK
        lateinit var selfDeletingMessageActionFactory: SelfDeletingMessageActionViewModelImpl.Factory

        @MockK
        lateinit var isFileSharingEnabledProvider: Provider<IsFileSharingEnabledViewModelImpl>

        @MockK
        lateinit var recordAudioProvider: Provider<RecordAudioViewModel>

        @MockK
        lateinit var audioMessageFactory: AudioMessageViewModelImpl.Factory

        val compositeMessageViewModel = mockk<CompositeMessageViewModelImpl>()
        val messageOptionsMenuViewModel = mockk<MessageOptionsMenuViewModelImpl>()
        val typingIndicatorViewModel = mockk<TypingIndicatorViewModelImpl>()
        val assetLocalPathViewModel = mockk<AssetLocalPathViewModelImpl>()
        val selfDeletingMessageActionViewModel = mockk<SelfDeletingMessageActionViewModelImpl>()
        val isFileSharingEnabledViewModel = mockk<IsFileSharingEnabledViewModelImpl>()
        val recordAudioViewModel = mockk<RecordAudioViewModel>()
        val audioMessageViewModel = mockk<AudioMessageViewModelImpl>()

        init {
            MockKAnnotations.init(this)
        }

        fun arrange(): Pair<Arrangement, ScopedMessageManualViewModelFactory> =
            this to WireMetroViewModelBindings.scopedMessageManualViewModelFactory(
                compositeMessageFactory = compositeMessageFactory,
                messageOptionsMenuFactory = messageOptionsMenuFactory,
                typingIndicatorFactory = typingIndicatorFactory,
                assetLocalPathFactory = assetLocalPathFactory,
                selfDeletingMessageActionFactory = selfDeletingMessageActionFactory,
                isFileSharingEnabledProvider = isFileSharingEnabledProvider,
                recordAudioProvider = recordAudioProvider,
                audioMessageFactory = audioMessageFactory,
            ) as ScopedMessageManualViewModelFactory
    }
}
